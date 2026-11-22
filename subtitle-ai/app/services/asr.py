import logging
import os
import shutil
import subprocess
import threading
from collections.abc import Callable
from pathlib import Path

from app.core.config import settings
from app.models import ASRSegment, WordTimestamp


logger = logging.getLogger(__name__)


class FasterWhisperAsrService:
    def __init__(self):
        self._model = None
        self._runtime: dict | None = None
        self._lock = threading.Lock()

    def transcribe_audio(
        self,
        audio_path: Path,
        language: str | None = None,
        on_progress: Callable[[float], None] | None = None,
    ) -> dict:
        model, runtime = self._load_model()
        kwargs = {
            "beam_size": runtime["beam_size"],
            "vad_filter": True,
            "temperature": 0.0,
            "condition_on_previous_text": False,
            "compression_ratio_threshold": 2.4,
            "log_prob_threshold": -1.0,
            "no_speech_threshold": 0.6,
            "word_timestamps": True
        }
        # The job's language beats the service-wide hint; neither means auto-detection.
        language = (language or settings.SOURCE_LANGUAGE_HINT or "").strip()
        if language:
            kwargs["language"] = language

        logger.info(
            "Starting ASR audio=%s device=%s compute_type=%s language=%s",
            audio_path,
            runtime["device"],
            runtime["compute_type"],
            language or "auto",
        )
        segments_iter, info = model.transcribe(str(audio_path), **kwargs)
        duration = float(getattr(info, "duration", 0.0) or 0.0)
        segments: list[ASRSegment] = []
        for index, segment in enumerate(segments_iter):
            if on_progress and duration > 0:
                on_progress(min(1.0, float(segment.end) / duration))
            text = (segment.text or "").strip()
            if not text:
                continue
            words = [
                WordTimestamp(
                    word=(word.word or "").strip(),
                    start=float(word.start or segment.start),
                    end=float(word.end or segment.end),
                    probability=float(getattr(word, "probability", 0.0) or 0.0),
                )
                for word in (segment.words or [])
                if (word.word or "").strip()
            ]
            segments.append(
                ASRSegment(
                    id=index,
                    start=float(segment.start),
                    end=float(segment.end),
                    text=text,
                    words=words,
                )
            )
        logger.info(
            "ASR completed audio=%s language=%s probability=%s segments=%s",
            audio_path,
            getattr(info, "language", None) or "und",
            float(getattr(info, "language_probability", 0.0) or 0.0),
            len(segments),
        )
        return {
            "language": getattr(info, "language", None) or "und",
            "language_probability": float(getattr(info, "language_probability", 0.0) or 0.0),
            # Seconds of audio, for progress and the real-time factor metric.
            "duration": duration,
            "segments": segments,
        }

    def preload(self) -> None:
        self._load_model()

    def _load_model(self):
        # The startup preload and the first job may race; only one of them loads the model.
        with self._lock:
            if self._model is not None and self._runtime is not None:
                return self._model, self._runtime

            try:
                from faster_whisper import WhisperModel
            except ImportError as exc:
                raise RuntimeError("faster-whisper is not installed") from exc

            runtime = resolve_whisper_runtime()
            self._model = self._create_model(WhisperModel, runtime)
            self._runtime = runtime
            return self._model, self._runtime

    def _create_model(self, whisper_model_cls, runtime: dict):
        configure_cpu_thread_env(runtime["cpu_threads"])
        model_kwargs = {
            "device": runtime["device"],
            "compute_type": runtime["compute_type"],
            "cpu_threads": runtime["cpu_threads"],
            "num_workers": runtime["num_workers"],
        }
        logger.info(
            "Loading faster-whisper model=%s device=%s compute_type=%s cpu_threads=%s num_workers=%s beam_size=%s",
            settings.WHISPER_MODEL_SIZE,
            runtime["device"],
            runtime["compute_type"],
            runtime["cpu_threads"],
            runtime["num_workers"],
            runtime["beam_size"],
        )
        return whisper_model_cls(settings.WHISPER_MODEL_SIZE, **model_kwargs)


asr_service = FasterWhisperAsrService()


def extract_audio(input_media_path: Path, output_dir: Path) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    output_audio = output_dir / f"{input_media_path.stem}_16k_mono.wav"
    if not input_media_path.exists():
        raise FileNotFoundError(f"Input media not found: {input_media_path}")
    if shutil.which("ffmpeg") is None:
        raise RuntimeError("ffmpeg not found")
    logger.info("Extracting audio input=%s output=%s", input_media_path, output_audio)
    command = ["ffmpeg", "-y", "-i", str(input_media_path), "-vn"]
    if settings.AUDIO_NORMALIZE_ENABLED:
        command.extend(["-af", "loudnorm=I=-20:TP=-1.5:LRA=11"])
    command.extend(["-acodec", "pcm_s16le", "-ac", "1", "-ar", "16000", str(output_audio)])
    subprocess.run(
        command,
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
    )
    logger.info("Audio extraction completed output=%s size=%s", output_audio, output_audio.stat().st_size)
    return output_audio


def transcribe_with_faster_whisper(
    audio_path: Path,
    language: str | None = None,
    on_progress: Callable[[float], None] | None = None,
) -> dict:
    return asr_service.transcribe_audio(audio_path, language, on_progress)


def preload_whisper_model() -> None:
    asr_service.preload()


def cpu_runtime() -> dict:
    cpu_threads = max(1, int(settings.WHISPER_CPU_THREADS or os.cpu_count() or 1))
    return {
        "device": "cpu",
        "compute_type": "int8",
        "cpu_threads": cpu_threads,
        "num_workers": max(1, int(settings.WHISPER_NUM_WORKERS or 1)),
        "beam_size": max(1, int(settings.WHISPER_CPU_BEAM_SIZE or 1)),
    }


def resolve_whisper_runtime() -> dict:
    configured_device = (settings.WHISPER_DEVICE or "auto").strip().lower()
    device = resolve_device(configured_device)
    requested_compute_type = (settings.WHISPER_COMPUTE_TYPE or "").strip().lower()
    cpu_threads = max(1, int(settings.WHISPER_CPU_THREADS or os.cpu_count() or 1))
    num_workers = max(1, int(settings.WHISPER_NUM_WORKERS or 1))

    if device == "cpu":
        runtime = cpu_runtime()
        if requested_compute_type not in {"", "auto", "float16", "float32"}:
            runtime["compute_type"] = requested_compute_type
        runtime["cpu_threads"] = cpu_threads
        runtime["num_workers"] = num_workers
        return runtime

    return {
        "device": device,
        "compute_type": "float16" if requested_compute_type in {"", "auto"} else requested_compute_type,
        "cpu_threads": cpu_threads,
        "num_workers": num_workers,
        "beam_size": max(1, int(settings.WHISPER_BEAM_SIZE or 5)),
    }


def resolve_device(configured: str) -> str:
    if configured in {"cpu", "cuda"}:
        return configured
    try:
        # ctranslate2 is faster-whisper's own runtime, so no extra package is needed.
        import ctranslate2

        if ctranslate2.get_cuda_device_count() > 0:
            logger.info("Auto-detected CUDA for faster-whisper using ctranslate2")
            return "cuda"
    except Exception as exc:
        logger.info("CUDA auto-detect unavailable for faster-whisper; using CPU. reason=%s", exc)
    logger.info("Auto-detected CPU for faster-whisper")
    return "cpu"


def configure_cpu_thread_env(cpu_threads: int) -> None:
    thread_count = str(max(1, cpu_threads))
    for env_name in (
        "OMP_NUM_THREADS",
        "MKL_NUM_THREADS",
        "OPENBLAS_NUM_THREADS",
        "VECLIB_MAXIMUM_THREADS",
        "NUMEXPR_NUM_THREADS",
    ):
        os.environ.setdefault(env_name, thread_count)
    os.environ.setdefault("KMP_BLOCKTIME", "0")
    os.environ.setdefault("OMP_WAIT_POLICY", "ACTIVE")


def word_confidence_items(segments: list[ASRSegment]) -> list[dict]:
    items: list[dict] = []
    for segment in segments:
        for word in segment.words:
            items.append(
                {
                    "word": word.word,
                    "start": word.start,
                    "end": word.end,
                    "confidence": word.probability,
                }
            )
    return items
