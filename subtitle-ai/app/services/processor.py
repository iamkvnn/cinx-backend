import asyncio
import contextvars
import json
import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from contextlib import contextmanager
from pathlib import Path

from app.core.config import settings
from app.core.metrics import (
    ASR_AUDIO_SECONDS,
    ASR_REALTIME_FACTOR,
    CUES_PER_JOB,
    JOB_REDELIVERIES,
    JOB_SECONDS,
    JOBS,
    JOBS_IN_PROGRESS,
    STAGE_SECONDS,
)
from app.models import SentenceItem, SubtitleItem, asr_segments_from_jsonable
from app.services.artifacts import write_json_artifact, write_text_artifact
from app.services.asr import extract_audio, transcribe_with_faster_whisper, word_confidence_items
from app.services.alignment import align_sentence_items_to_words
from app.services.job_journal import COMPLETED, FAILED, JobJournal, sweep_expired_jobs
from app.services.llm import DigitalOceanLLM
from app.services.quality import Thresholds, doubtful_word_share, review_summary
from app.services.segmentation import split_segments_to_sentences
from app.services.storage import S3Storage
from app.services.subtitle import (
    build_final_subtitles,
    group_sentence_items,
    parse_webvtt,
    sentence_items_from_cues,
    validate_subtitles,
    write_webvtt,
)


logger = logging.getLogger(__name__)
CONTENT_TYPE_VTT = "text/vtt"
CONTENT_TYPE_JSON = "application/json"
COMPLETED_ROUTING_KEY = "ai.subtitle.job.completed"
FAILED_ROUTING_KEY = "ai.subtitle.job.failed"
MAX_ERROR_MESSAGE_CHARS = 4000
ASR_ARTIFACT = "02_asr_result.json"
PROGRESS_STEP = 5
TRANSLATE_BATCH_MAX_CHARS = 3000


class SubtitleJobProcessor:
    def __init__(
        self,
        storage: S3Storage | None = None,
        llm: DigitalOceanLLM | None = None,
    ):
        self.storage = storage or S3Storage()
        self.llm = llm or DigitalOceanLLM()

    async def process_generate(self, event: dict, publisher) -> None:
        await self._run_job(event, publisher, self._generate, "generate")

    async def process_translate(self, event: dict, publisher) -> None:
        await self._run_job(event, publisher, self._translate, "translate")

    async def _run_job(self, event: dict, publisher, work, job_type: str) -> None:
        """Replay a finished job, cap interrupted ones, and record the result before publishing it."""
        job_id = require(event, "jobId")
        journal = JobJournal(job_id)
        if journal.finished:
            logger.info("Replaying stored subtitle result job_id=%s status=%s", job_id, journal.status)
            JOBS.labels(job_type, "replayed").inc()
            await publisher.publish_result(journal.result)
            return

        await asyncio.to_thread(sweep_expired_jobs, job_id)
        attempt = journal.begin_attempt()
        if attempt > 1:
            JOB_REDELIVERIES.labels(job_type).inc()
        if attempt > settings.MAX_JOB_ATTEMPTS:
            logger.warning("Subtitle job interrupted too often job_id=%s attempt=%s", job_id, attempt)
            result = journal.finish(
                FAILED,
                FAILED_ROUTING_KEY,
                failed_payload(
                    job_id,
                    "AI_SUBTITLE_INTERRUPTED",
                    f"Subtitle job was interrupted {attempt - 1} times before it finished",
                ),
            )
            JOBS.labels(job_type, "interrupted").inc()
            await publisher.publish_result(result)
            return

        started = time.perf_counter()
        JOBS_IN_PROGRESS.labels(job_type).inc()
        try:
            payload = await work(event, publisher, journal.directory, attempt)
        except Exception as exc:
            logger.exception("Subtitle job failed job_id=%s attempt=%s", job_id, attempt)
            outcome = "failed"
            result = journal.finish(FAILED, FAILED_ROUTING_KEY, failed_payload(job_id, "AI_SUBTITLE_FAILED", str(exc)))
        else:
            outcome = "completed"
            result = journal.finish(COMPLETED, COMPLETED_ROUTING_KEY, payload)
        finally:
            # Also on cancellation (shutdown), which leaves the job to a later delivery.
            JOBS_IN_PROGRESS.labels(job_type).dec()
        JOBS.labels(job_type, outcome).inc()
        JOB_SECONDS.labels(job_type).observe(time.perf_counter() - started)
        await publisher.publish_result(result)

    async def _generate(self, event: dict, publisher, work_dir: Path, attempt: int) -> dict:
        job_id = require(event, "jobId")
        lesson_id = require(event, "lessonId")
        video_file_key = require(event, "videoFileKey")
        expected_output_file_key = require(event, "expectedOutputFileKey")
        target_language_code = event.get("targetLanguageCode") or "auto"
        requested_language = None if target_language_code == "auto" else normalize_language(target_language_code)

        logger.info(
            "Generate subtitle job started job_id=%s lesson_id=%s video_file_key=%s target_language=%s "
            "output_key=%s attempt=%s",
            job_id,
            lesson_id,
            video_file_key,
            target_language_code,
            expected_output_file_key,
            attempt,
        )
        await publisher.publish_progress(job_id, 5)
        write_json_artifact(work_dir, "00_generate_event.json", event)

        asr_result = load_asr_checkpoint(work_dir)
        if asr_result is not None:
            logger.info("Resuming from the stored ASR result job_id=%s segments=%s", job_id, len(asr_result["segments"]))
        else:
            suffix = Path(video_file_key).suffix or ".media"
            logger.info("Downloading source media job_id=%s file_key=%s", job_id, video_file_key)
            media_path = work_dir / f"01_downloaded_media{suffix}"
            with stage("download"):
                await asyncio.to_thread(self.storage.download_to_file, video_file_key, media_path)
            logger.info("Source media downloaded job_id=%s local_path=%s", job_id, media_path)
            await publisher.publish_progress(job_id, 15)

            logger.info("Extracting audio job_id=%s media_path=%s", job_id, media_path)
            with stage("extract_audio"):
                audio_path = await asyncio.to_thread(extract_audio, media_path, work_dir)
            await publisher.publish_progress(job_id, 25)

            logger.info("Running ASR job_id=%s audio_path=%s", job_id, audio_path)
            progress = ProgressReporter(publisher, job_id, 25, 55)
            asr_started = time.perf_counter()
            with stage("asr"):
                asr_result = await asyncio.to_thread(
                    transcribe_with_faster_whisper,
                    audio_path,
                    requested_language,
                    progress,
                )
            observe_asr(asr_result, time.perf_counter() - asr_started)
            write_json_artifact(work_dir, ASR_ARTIFACT, asr_result)

        source_language = normalize_language(asr_result["language"])
        logger.info(
            "ASR result job_id=%s source_language=%s language_probability=%s segments=%s",
            job_id,
            source_language,
            asr_result["language_probability"],
            len(asr_result["segments"]),
        )
        await publisher.publish_progress(job_id, 55)

        output_language = requested_language or source_language
        logger.info("Building subtitles job_id=%s output_language=%s", job_id, output_language)
        with stage("build_subtitles"):
            vtt_content, subtitles = await asyncio.to_thread(
                self._generated_subtitles,
                asr_result["segments"],
                output_language,
                work_dir,
            )
        cue_count = len(subtitles)
        CUES_PER_JOB.labels("generate").observe(cue_count)
        confidence_items = word_confidence_items(asr_result["segments"])
        quality = review_summary(
            subtitles,
            quality_thresholds(),
            doubtful_word_share(confidence_items),
            asr_result.get("language_probability"),
        )
        write_json_artifact(work_dir, "12_quality.json", quality)
        await publisher.publish_progress(job_id, 70)

        logger.info(
            "Uploading generated VTT job_id=%s cues=%s output_key=%s",
            job_id,
            cue_count,
            expected_output_file_key,
        )
        with stage("upload"):
            uploaded = await asyncio.to_thread(
                self.storage.upload_text,
                expected_output_file_key,
                vtt_content,
                CONTENT_TYPE_VTT,
            )
        await publisher.publish_progress(job_id, 90)

        confidence_key = word_confidence_key(expected_output_file_key)
        confidence_content = json.dumps(confidence_items, ensure_ascii=False)
        write_text_artifact(work_dir, "11_word_confidence.json", confidence_content)
        logger.info("Uploading word confidence job_id=%s output_key=%s", job_id, confidence_key)
        with stage("upload"):
            confidence = await asyncio.to_thread(
                self.storage.upload_text,
                confidence_key,
                confidence_content,
                CONTENT_TYPE_JSON,
            )
        logger.info(
            "Generate subtitle job completed job_id=%s output_key=%s confidence_key=%s",
            job_id,
            uploaded["fileKey"],
            confidence["fileKey"],
        )
        return {
            "jobId": job_id,
            "outputFileKey": uploaded["fileKey"],
            "outputFileUrl": uploaded["fileUrl"],
            "fileName": f"{job_id}.vtt",
            "fileType": CONTENT_TYPE_VTT,
            "fileSize": uploaded["fileSize"],
            "languageCode": output_language,
            "displayName": event.get("displayName") or output_language,
            "wordConfidenceFileKey": confidence["fileKey"],
            "wordConfidenceFileUrl": confidence["fileUrl"],
            "quality": quality,
            # the audio length transcribed: learning charges the instructor's subtitle minutes from it
            "durationSeconds": round(float(asr_result.get("duration") or 0.0), 1) or None,
        }

    async def _translate(self, event: dict, publisher, work_dir: Path, attempt: int) -> dict:
        job_id = require(event, "jobId")
        source_file_key = require(event, "sourceFileKey")
        expected_output_file_key = require(event, "expectedOutputFileKey")
        source_language = normalize_language(event.get("sourceLanguageCode") or "und")
        target_language = normalize_language(require(event, "targetLanguageCode"))

        logger.info(
            "Translate subtitle job started job_id=%s source_file_key=%s "
            "source_language=%s target_language=%s output_key=%s attempt=%s",
            job_id,
            source_file_key,
            source_language,
            target_language,
            expected_output_file_key,
            attempt,
        )
        write_json_artifact(work_dir, "00_translate_event.json", event)
        await publisher.publish_progress(job_id, 10)
        logger.info("Reading source subtitle job_id=%s file_key=%s", job_id, source_file_key)
        with stage("download"):
            source_content = await asyncio.to_thread(self.storage.read_text, source_file_key)
        write_text_artifact(work_dir, "01_source_subtitle.vtt", source_content)
        cues = parse_webvtt(source_content)
        if not cues:
            raise ValueError("Source subtitle has no readable cues")
        write_json_artifact(work_dir, "02_parsed_source_cues.json", cues)
        logger.info("Parsed source subtitle job_id=%s cues=%s", job_id, len(cues))
        await publisher.publish_progress(job_id, 25)

        source_text = "\n".join(cue.text for cue in cues)
        write_text_artifact(work_dir, "03_source_transcript.txt", source_text)
        logger.info("Summarizing and extracting terminology job_id=%s", job_id)
        with stage("analysis"):
            analysis = await asyncio.to_thread(
                self.llm.summarize_and_extract_terms,
                source_text,
                source_language,
                target_language,
            )
        write_json_artifact(work_dir, "04_llm_analysis_output.json", analysis)
        await publisher.publish_progress(job_id, 40)

        groups, cue_times = group_sentence_items(sentence_items_from_cues(cues), source_language)
        write_json_artifact(work_dir, "05_source_sentence_groups.json", groups)
        logger.info("Translating subtitle sentences job_id=%s cues=%s sentences=%s", job_id, len(cues), len(groups))
        progress = ProgressReporter(publisher, job_id, 40, 70)
        with stage("translate"):
            translated_items = await asyncio.to_thread(
                translate_sentence_items,
                groups,
                self.llm,
                source_language,
                target_language,
                analysis,
                work_dir,
                progress,
            )
        write_json_artifact(work_dir, "07_translated_sentence_items.json", translated_items)
        await publisher.publish_progress(job_id, 70)

        logger.info("Building translated subtitles job_id=%s", job_id)
        with stage("build_subtitles"):
            vtt_content, subtitles = await asyncio.to_thread(
                self._render_subtitles,
                translated_items,
                target_language,
                work_dir,
                None,
                cue_times,
            )
        cue_count = len(subtitles)
        CUES_PER_JOB.labels("translate").observe(cue_count)
        quality = review_summary(subtitles, quality_thresholds())
        write_json_artifact(work_dir, "12_quality.json", quality)
        logger.info(
            "Uploading translated VTT job_id=%s cues=%s output_key=%s",
            job_id,
            cue_count,
            expected_output_file_key,
        )
        with stage("upload"):
            uploaded = await asyncio.to_thread(
                self.storage.upload_text,
                expected_output_file_key,
                vtt_content,
                CONTENT_TYPE_VTT,
            )
        await publisher.publish_progress(job_id, 90)
        logger.info("Translate subtitle job completed job_id=%s output_key=%s", job_id, uploaded["fileKey"])
        return {
            "jobId": job_id,
            "outputFileKey": uploaded["fileKey"],
            "outputFileUrl": uploaded["fileUrl"],
            "fileName": f"{job_id}.vtt",
            "fileType": CONTENT_TYPE_VTT,
            "fileSize": uploaded["fileSize"],
            "languageCode": target_language,
            "displayName": event.get("displayName") or target_language,
            "wordConfidenceFileKey": None,
            "wordConfidenceFileUrl": None,
            "quality": quality,
        }

    def _generated_subtitles(self, segments, output_language: str, work_dir: Path) -> tuple[str, list[SubtitleItem]]:
        sentences = split_segments_to_sentences(segments)
        write_json_artifact(work_dir, "03_local_split_sentences.json", sentences)
        aligned = align_sentence_items_to_words(sentences, segments)
        write_json_artifact(work_dir, "04_aligned_sentences.json", aligned)
        return self._render_subtitles(aligned, output_language, work_dir, segments)

    def _render_subtitles(
        self,
        items: list[SentenceItem],
        language: str,
        work_dir: Path,
        segments=None,
        cue_times: dict[int, list[tuple[float, float]]] | None = None,
    ) -> tuple[str, list[SubtitleItem]]:
        subtitles = build_final_subtitles(items, self.llm, language, work_dir, segments, cue_times)
        write_json_artifact(work_dir, "09_final_subtitles.json", subtitles)
        validate_subtitles(subtitles)
        vtt_content = write_webvtt(subtitles)
        write_text_artifact(work_dir, "10_final_subtitles.vtt", vtt_content)
        return vtt_content, subtitles


class ProgressReporter:
    """Publishes progress from worker threads onto the event loop, one event per PROGRESS_STEP."""

    def __init__(self, publisher, job_id: str, start: int, end: int):
        self.publisher = publisher
        self.job_id = job_id
        self.start = start
        self.end = end
        self.last = start
        self.loop = asyncio.get_running_loop()
        self.lock = threading.Lock()

    def __call__(self, fraction: float) -> None:
        percent = int(self.start + (self.end - self.start) * max(0.0, min(1.0, fraction)))
        with self.lock:
            if percent - self.last < PROGRESS_STEP:
                return
            self.last = percent
        # Best effort: publish_progress swallows broker errors, and nothing waits for the result.
        asyncio.run_coroutine_threadsafe(self.publisher.publish_progress(self.job_id, percent), self.loop)


@contextmanager
def stage(name: str):
    """Time one pipeline stage; only a stage that finishes is recorded."""
    started = time.perf_counter()
    yield
    STAGE_SECONDS.labels(name).observe(time.perf_counter() - started)


def observe_asr(asr_result: dict, seconds: float) -> None:
    audio_seconds = float(asr_result.get("duration") or 0.0)
    if audio_seconds > 0:
        ASR_AUDIO_SECONDS.inc(audio_seconds)
        ASR_REALTIME_FACTOR.observe(seconds / audio_seconds)


def translate_sentence_items(
    source_items: list[SentenceItem],
    llm: DigitalOceanLLM,
    source_language: str,
    target_language: str,
    analysis: dict,
    artifact_dir: Path | None = None,
    on_progress=None,
) -> list[SentenceItem]:
    batches = translation_batches([{"id": item.id, "text": item.text} for item in source_items])

    def translate(batch_index: int, batch: list[dict]) -> list[dict]:
        write_json_artifact(artifact_dir, f"06_llm_translate_batch_{batch_index}_input.json", batch)
        translated_batch = llm.translate_batch(batch, source_language, target_language, analysis)
        write_json_artifact(artifact_dir, f"06_llm_translate_batch_{batch_index}_output.json", translated_batch)
        return translated_batch

    translated_by_id: dict[int, str] = {}
    workers = max(1, min(settings.TRANSLATE_CONCURRENCY, len(batches)))
    with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="subtitle-translate") as pool:
        # Each batch keeps the job's logging context.
        futures = [
            pool.submit(contextvars.copy_context().run, translate, batch_index, batch)
            for batch_index, batch in enumerate(batches, start=1)
        ]
        try:
            for done, future in enumerate(as_completed(futures), start=1):
                for translated in future.result():
                    translated_by_id[int(translated["id"])] = translated["text"]
                if on_progress:
                    on_progress(done / len(futures))
        except Exception:
            for future in futures:
                future.cancel()
            raise
    return [
        SentenceItem(
            id=item.id,
            source_segment_id=item.source_segment_id,
            start=item.start,
            end=item.end,
            text=translated_by_id.get(item.id, item.text),
        )
        for item in source_items
    ]


def translation_batches(items: list[dict]) -> list[list[dict]]:
    """Up to TRANSLATE_BATCH_SIZE items and TRANSLATE_BATCH_MAX_CHARS characters per request."""
    batches: list[list[dict]] = []
    current: list[dict] = []
    size = 0
    for item in items:
        if current and (
            len(current) >= settings.TRANSLATE_BATCH_SIZE
            or size + len(item["text"]) > TRANSLATE_BATCH_MAX_CHARS
        ):
            batches.append(current)
            current, size = [], 0
        current.append(item)
        size += len(item["text"])
    if current:
        batches.append(current)
    return batches


def load_asr_checkpoint(work_dir: Path) -> dict | None:
    """The ASR result of an earlier attempt of this job, so a redelivery skips Whisper."""
    path = work_dir / ASR_ARTIFACT
    if not path.exists():
        return None
    try:
        stored = json.loads(path.read_text(encoding="utf-8"))
        return {
            "language": stored["language"],
            "language_probability": float(stored.get("language_probability") or 0.0),
            "segments": asr_segments_from_jsonable(stored["segments"]),
        }
    except (OSError, ValueError, KeyError, TypeError):
        logger.warning("Stored ASR result is unreadable; transcribing again file=%s", path.name, exc_info=True)
        return None


def failed_payload(job_id: str, error_code: str, error_message: str) -> dict:
    return {
        "jobId": job_id,
        "errorCode": error_code,
        "errorMessage": error_message[:MAX_ERROR_MESSAGE_CHARS],
    }


def quality_thresholds() -> Thresholds:
    return Thresholds(
        max_line_chars=settings.MAX_SUBTITLE_LINE_CHARS,
        max_lines=settings.MAX_SUBTITLE_LINES,
        max_cps=settings.MAX_SUBTITLE_CPS,
        min_seconds=settings.MIN_SUBTITLE_SECONDS,
    )


def word_confidence_key(output_file_key: str) -> str:
    if output_file_key.endswith(".vtt"):
        return output_file_key[:-4] + ".words.json"
    return output_file_key + ".words.json"


def normalize_language(language_code: str) -> str:
    language_code = (language_code or "und").strip()
    return language_code if language_code else "und"


def require(event: dict, field: str) -> str:
    value = event.get(field)
    if value is None or str(value).strip() == "":
        raise ValueError(f"Missing required event field: {field}")
    return str(value)
