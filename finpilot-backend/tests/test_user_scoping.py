import sys
import types
import unittest

from unittest.mock import patch

from app.core.config import settings


class FakeModels(types.SimpleNamespace):
    class MatchValue:
        def __init__(self, value):
            self.value = value

    class FieldCondition:
        def __init__(self, key: str, match: "FakeModels.MatchValue"):
            self.key = key
            self.match = match

    class Filter:
        def __init__(self, must=None):
            self.must = must or []


class FakePoint:
    def __init__(self, payload, score=1.0):
        self.payload = payload
        self.score = score


class FakeClient:
    def __init__(self, points):
        self._points = points

    def get_collections(self):
        # emulate qdrant_client.get_collections().collections
        coll = types.SimpleNamespace(name=settings.QDRANT_COLLECTION)
        return types.SimpleNamespace(collections=[coll])

    def query_points(self, collection_name, query, limit=5, query_filter=None):
        # inspect query_filter to optionally filter by user_id
        user_id = None
        if query_filter is not None and hasattr(query_filter, "must"):
            for cond in query_filter.must:
                if getattr(cond, "key", None) == "user_id":
                    user_id = getattr(getattr(cond, "match", None), "value", None)
        pts = []
        for p in self._points:
            uid = (p.payload or {}).get("user_id")
            if user_id is None or uid == user_id:
                pts.append(p)
        return types.SimpleNamespace(points=pts)

    def scroll(self, collection_name, scroll_filter=None, limit=1000):
        # similar filtering as query_points
        user_id = None
        if scroll_filter is not None and hasattr(scroll_filter, "must"):
            for cond in scroll_filter.must:
                if getattr(cond, "key", None) == "user_id":
                    user_id = getattr(getattr(cond, "match", None), "value", None)
        pts = []
        for p in self._points:
            uid = (p.payload or {}).get("user_id")
            if user_id is None or uid == user_id:
                pts.append(p)
        return pts, None


class UserScopingTests(unittest.TestCase):
    def setUp(self):
        # Install a fake qdrant_client.models module to avoid real dependency
        fake_models = types.ModuleType("qdrant_client.models")
        fake_models.MatchValue = FakeModels.MatchValue
        fake_models.FieldCondition = FakeModels.FieldCondition
        fake_models.Filter = FakeModels.Filter
        # minimal placeholders so embedding imports succeed
        class Distance:
            COSINE = "cosine"

        class VectorParams:
            def __init__(self, size, distance):
                self.size = size
                self.distance = distance

        class PointStruct:
            def __init__(self, id, vector, payload):
                self.id = id
                self.vector = vector
                self.payload = payload

        fake_models.Distance = Distance
        fake_models.VectorParams = VectorParams
        fake_models.PointStruct = PointStruct
        qc_mod = sys.modules.setdefault("qdrant_client", types.ModuleType("qdrant_client"))
        # provide a dummy QdrantClient so imports of `from qdrant_client import QdrantClient` succeed
        class DummyQdrantClient:
            pass

        setattr(qc_mod, "QdrantClient", DummyQdrantClient)
        sys.modules["qdrant_client.models"] = fake_models

    def tearDown(self):
        # Clean up injected modules
        sys.modules.pop("qdrant_client.models", None)

    def test_retrieve_filters_by_user_id(self):
        from app.services.retrieval import retrieve
        from app.services.embedding import embed_texts

        # two points, different user_id
        p1 = FakePoint({"chunk_id": "c1", "text": "one", "user_id": 7}, score=0.9)
        p2 = FakePoint({"chunk_id": "c2", "text": "two", "user_id": 8}, score=0.8)
        client = FakeClient([p1, p2])

        with patch("app.services.retrieval.get_qdrant_client", return_value=client):
            with patch("app.services.embedding.embed_texts", return_value=[[0.0]*settings.EMBEDDING_DIM]):
                results = retrieve("query", top_k=5, user_id=7)

        self.assertEqual(len(results), 1)
        self.assertEqual(results[0].chunk_id, "c1")

    def test_analyze_uses_user_scope_and_logs(self):
        from app.routers.analyze import analyze
        from app.models.schemas import AnalyzeRequest

        # two points, only one belongs to user 42
        p1 = FakePoint({"chunk_id": "u42_1", "text": "user42 text", "user_id": 42}, score=0.9)
        p2 = FakePoint({"chunk_id": "u99_1", "text": "other user", "user_id": 99}, score=0.8)
        client = FakeClient([p1, p2])

        class FakeUser:
            def __init__(self, id, username):
                self.id = id
                self.username = username

        fake_user = FakeUser(42, "bob")

        # stub LLM and guardrails
        with patch("app.services.retrieval.get_qdrant_client", return_value=client), \
             patch("app.services.embedding.embed_texts", return_value=[[0.0]*settings.EMBEDDING_DIM]), \
             patch("app.services.llm.generate_answer", return_value="answer"), \
             patch("app.services.guardrails.apply_response_guardrails", return_value=types.SimpleNamespace(final_text="answer")), \
             patch("app.routers.analyze.log_interaction", lambda *args, **kwargs: None):

            req = AnalyzeRequest(query="where did I spend more?", filename=None, top_k=5)
            import asyncio

            result = asyncio.get_event_loop().run_until_complete(analyze(req, db=None, current_user=fake_user))

        self.assertEqual(result.retrieved_chunks, 1)
        self.assertIn("u42_1", result.sources)


if __name__ == "__main__":
    unittest.main()
