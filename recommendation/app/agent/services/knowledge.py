import uuid
from datetime import datetime

from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.agent.schemas import Citation
from app.core.config import settings
from app.entities.knowledge import KnowledgeChunk, KnowledgeDocument
from app.services.rag_index import rag_index, schedule_rag_rebuild
from app.services.embedding_model import embed_documents, embed_query


class KnowledgeService:
    def __init__(self, db: Session):
        self.db = db

    def import_document(
        self,
        title: str,
        content: str,
        source_type: str = "CMS",
        source_url: str | None = None,
    ) -> KnowledgeDocument:
        document = KnowledgeDocument(
            id=str(uuid.uuid4()),
            title=title,
            source_type=source_type,
            source_url=source_url,
            content=content,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow(),
        )
        self.db.add(document)
        self.db.flush()

        chunks = self._chunk_text(content)
        embeddings = embed_documents(chunks)
        for chunk_text, embedding in zip(chunks, embeddings):
            self.db.add(
                KnowledgeChunk(
                    id=str(uuid.uuid4()),
                    document_id=document.id,
                    content=chunk_text,
                    embedding=embedding,
                    chunk_metadata={},
                )
            )
        self.db.commit()
        schedule_rag_rebuild()
        return document

    def replace_document(
        self,
        document_id: str,
        title: str,
        content: str,
        source_type: str = "CMS",
        source_url: str | None = None,
    ) -> KnowledgeDocument:
        """Replace a document and its chunks.  The caller commits, together with the
        event's inbox row, then schedules the index rebuild."""
        # Embed first, so no row is locked while the model runs.
        chunks = self._chunk_text(content)
        embeddings = embed_documents(chunks)

        self.db.execute(delete(KnowledgeChunk).where(KnowledgeChunk.document_id == document_id))
        document = self.db.get(KnowledgeDocument, document_id)
        if document is None:
            document = KnowledgeDocument(id=document_id, title=title, source_type=source_type, source_url=source_url, content=content)
            self.db.add(document)
        else:
            document.title = title
            document.source_type = source_type
            document.source_url = source_url
            document.content = content
            document.updated_at = datetime.utcnow()
        self.db.flush()

        for chunk_text, embedding in zip(chunks, embeddings):
            self.db.add(
                KnowledgeChunk(
                    id=str(uuid.uuid4()),
                    document_id=document.id,
                    content=chunk_text,
                    embedding=embedding,
                    chunk_metadata={},
                )
            )
        return document

    def delete_document(self, document_id: str) -> bool:
        """Delete a document; True when there was one.  The caller commits and rebuilds."""
        document = self.db.get(KnowledgeDocument, document_id)
        if document is None:
            return False
        self.db.delete(document)
        return True

    def retrieve(
        self,
        query: str,
        top_k: int = 5,
        source_type: str | None = "CMS",
    ) -> tuple[list[str], list[Citation]]:
        # The index is rebuilt at startup and on knowledge events, never on the
        # request path: an empty result means there is nothing relevant.
        query_embedding = embed_query(query)
        results = [
            (chunk, score)
            for chunk, score in rag_index.search(query_embedding, top_k, source_type=source_type)
            if score >= settings.AGENT_POLICY_MIN_SCORE
        ]

        contexts = []
        citations = []
        for chunk, score in results:
            contexts.append(chunk.content)
            citations.append(
                Citation(
                    sourceType=chunk.source_type,
                    title=chunk.document_title,
                    documentId=chunk.document_id,
                    sourceUrl=chunk.source_url,
                    score=score,
                )
            )
        return contexts, citations

    def _chunk_text(self, content: str, chunk_size: int = 1200, overlap: int = 150) -> list[str]:
        normalized = " ".join(content.split())
        if not normalized:
            return [content]
        chunks = []
        start = 0
        while start < len(normalized):
            end = min(start + chunk_size, len(normalized))
            chunks.append(normalized[start:end])
            if end == len(normalized):
                break
            start = max(end - overlap, 0)
        return chunks
