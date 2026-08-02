from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm.session import Session
from sqlalchemy.exc import IntegrityError

from app.models.db import User, get_db
from app.models.schemas import SignupRequest, LoginRequest, TokenResponse
from app.core.security import hash_password, verify_password, create_access_token

router = APIRouter()


def validate_signup_request(username: str, password: str) -> str:
    cleaned_username = username.strip().lower()

    if len(cleaned_username) < 3:
        raise HTTPException(status_code=400, detail="Username must be at least 3 characters.")
    if len(password) < 3:
        raise HTTPException(status_code=400, detail="Password must be at least 3 characters.")

    return cleaned_username


@router.post("/signup", response_model=TokenResponse)
async def signup(req: SignupRequest, db: Session = Depends(get_db)):
    username = validate_signup_request(req.username, req.password)

    existing = db.query(User).filter(User.username == username).first()
    if existing:
        raise HTTPException(status_code=409, detail="That username is already taken.")

    user = User(username=username, hashed_password=hash_password(req.password))
    db.add(user)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="That username is already taken.")

    token = create_access_token(username, user.id)
    return TokenResponse(access_token=token, username=username)


@router.post("/login", response_model=TokenResponse)
async def login(req: LoginRequest, db: Session = Depends(get_db)):
    username = req.username.strip().lower()
    user = db.query(User).filter(User.username == username).first()

    # Deliberately identical error for "no such user" and "wrong password" —
    # revealing which one it was lets an attacker enumerate valid usernames.
    if not user or not verify_password(req.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Incorrect username or password.")

    token = create_access_token(username, user.id)
    return TokenResponse(access_token=token, username=username)