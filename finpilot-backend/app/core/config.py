import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()  # Load environment variables from .env file

class Settings:
    APP_NAME: str = "FinPilot AI Backend"
    ENV: str = os.getenv("ENV", "local")

    #storage
    UPLOAD_DIR: Path = Path(os.getenv("UPLOAD_DIR", "uploads"))

    #Chunking (tuned for transaction-line statements, see chunking.py)
    CHUNK_MAX_LINES: int = int(os.getenv("CHUNK_MAX_LINES", "12"))

     # Qdrant
    QDRANT_URL: str = os.getenv("QDRANT_URL", "http://localhost:6333")
    QDRANT_LOCAL_ONLY: bool = os.getenv("QDRANT_LOCAL_ONLY", "true").lower() == "true"
    QDRANT_PATH: str = os.getenv("QDRANT_PATH", "qdrant_local_db")
    QDRANT_COLLECTION: str = os.getenv("QDRANT_COLLECTION", "finpilot_statements")

    # Embeddings (FastEmbed — local, no API key needed)
    EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-en-v1.5")
    EMBEDDING_DIM: int = int(os.getenv("EMBEDDING_DIM", "384"))

     # LLM (Groq)
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    GROQ_MODEL: str = os.getenv("GROQ_MODEL", "llama-3.1-8b-instant")

    def __init__(self):
        # Ensure the upload directory exists
        self.UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    
settings = Settings()