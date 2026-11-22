import os
import unittest
from unittest.mock import patch


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")
os.environ.setdefault("AWS_S3_CDN_URL", "https://cdn.test")
os.environ.setdefault("AWS_REGION", "test-region")
os.environ.setdefault("RABBITMQ_USER", "guest")
os.environ.setdefault("RABBITMQ_PASSWORD", "guest")
os.environ.setdefault("DIGITALOCEAN_LLM_MODEL", "test-model")
os.environ.setdefault("SOURCE_LANGUAGE_HINT", "")

from app.services.asr import resolve_whisper_runtime


class AsrRuntimeTest(unittest.TestCase):
    def test_cpu_runtime_uses_int8_and_cpu_beam_size(self):
        with (
            patch("app.services.asr.settings.WHISPER_DEVICE", "cpu"),
            patch("app.services.asr.settings.WHISPER_COMPUTE_TYPE", "float16"),
            patch("app.services.asr.settings.WHISPER_CPU_THREADS", 4),
            patch("app.services.asr.settings.WHISPER_NUM_WORKERS", 1),
            patch("app.services.asr.settings.WHISPER_CPU_BEAM_SIZE", 1),
        ):
            runtime = resolve_whisper_runtime()

        self.assertEqual("cpu", runtime["device"])
        self.assertEqual("int8", runtime["compute_type"])
        self.assertEqual(4, runtime["cpu_threads"])
        self.assertEqual(1, runtime["beam_size"])

    def test_auto_runtime_uses_detected_cpu(self):
        with (
            patch("app.services.asr.settings.WHISPER_DEVICE", "auto"),
            patch("app.services.asr.settings.WHISPER_COMPUTE_TYPE", "auto"),
            patch("app.services.asr.resolve_device", return_value="cpu"),
        ):
            runtime = resolve_whisper_runtime()

        self.assertEqual("cpu", runtime["device"])
        self.assertEqual("int8", runtime["compute_type"])

    def test_auto_runtime_uses_detected_cuda(self):
        with (
            patch("app.services.asr.settings.WHISPER_DEVICE", "auto"),
            patch("app.services.asr.settings.WHISPER_COMPUTE_TYPE", "auto"),
            patch("app.services.asr.resolve_device", return_value="cuda"),
        ):
            runtime = resolve_whisper_runtime()

        self.assertEqual("cuda", runtime["device"])
        self.assertEqual("float16", runtime["compute_type"])


if __name__ == "__main__":
    unittest.main()
