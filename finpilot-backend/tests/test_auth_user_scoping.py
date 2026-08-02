import unittest

from app.core.security import create_access_token, decode_access_token


class AuthUserScopingTests(unittest.TestCase):
    def test_access_token_round_trips_user_id(self):
        token = create_access_token("alice", 7)
        payload = decode_access_token(token)

        self.assertEqual(payload["username"], "alice")
        self.assertEqual(payload["user_id"], 7)


if __name__ == "__main__":
    unittest.main()
