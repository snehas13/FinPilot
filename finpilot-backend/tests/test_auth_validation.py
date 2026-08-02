import unittest

from app.routers.auth import validate_signup_request


class AuthValidationTests(unittest.TestCase):
    def test_signup_accepts_five_character_password(self):
        validate_signup_request("sneha", "sneha")


if __name__ == "__main__":
    unittest.main()
