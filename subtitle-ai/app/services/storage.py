from pathlib import Path

import boto3

from app.core.config import settings


class S3Storage:
    def __init__(self):
        self.client = boto3.client(
            "s3",
            endpoint_url=settings.AWS_S3_ENDPOINT,
            region_name=settings.AWS_REGION,
            aws_access_key_id=settings.aws_access_key_value,
            aws_secret_access_key=settings.aws_secret_key_value,
        )
        self.bucket = settings.AWS_S3_BUCKET
        self.cdn_url = settings.AWS_S3_CDN_URL.rstrip("/")

    def download_to_file(self, object_key: str, destination: Path) -> Path:
        destination.parent.mkdir(parents=True, exist_ok=True)
        self.client.download_file(self.bucket, object_key, str(destination))
        return destination

    def read_text(self, object_key: str) -> str:
        response = self.client.get_object(Bucket=self.bucket, Key=object_key)
        return response["Body"].read().decode("utf-8")

    def upload_text(self, object_key: str, content: str, content_type: str) -> dict:
        data = content.encode("utf-8")
        self.client.put_object(
            Bucket=self.bucket,
            Key=object_key,
            Body=data,
            ContentType=content_type,
            ACL="public-read",
        )
        return {
            "fileKey": object_key,
            "fileUrl": self.public_url(object_key),
            "fileSize": len(data),
            "fileType": content_type,
        }

    def public_url(self, object_key: str) -> str:
        if self.cdn_url:
            return f"{self.cdn_url}/{object_key}"
        if settings.AWS_S3_ENDPOINT:
            return f"{settings.AWS_S3_ENDPOINT.rstrip('/')}/{self.bucket}/{object_key}"
        return f"https://{self.bucket}.s3.{settings.AWS_REGION}.amazonaws.com/{object_key}"
