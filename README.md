# FinPilot AI

**Your AI Financial Coach** — an Android-native financial coaching app combining RAG, a LangGraph agentic Goal Planner, safety guardrails, JWT authentication, an admin/audit dashboard, and Kotlin Multiplatform architecture.

📽️ **[Demo video](./docs/video.mp4)** &nbsp;·&nbsp; 📊 **[Presentation deck](./docs/FinPilot_AI_Presentation.pptx)**

---

## Overview

FinPilot AI lets a user upload a bank/card statement PDF, then:
- **Ask questions** about their spending and get grounded, cited answers (RAG)
- **See a deterministic Financial Health Score** computed from real transaction data — never AI-generated numbers
- **Generate savings goal plans** via a multi-step, self-correcting LangGraph agent
- **Sign up / log in** with a real username/password + JWT session
- **View system usage** through an admin/audit dashboard (second-persona ops surface)

Built as a capstone project demonstrating retrieval-augmented generation, agentic orchestration, AI safety guardrails, evaluation methodology, and Kotlin Multiplatform mobile architecture.

---

## Screenshots

| Home Dashboard | Quick Actions |
|---|---|
| ![Home](docs/screenshots/home_dashboard.png) | ![Quick Actions](docs/screenshots/home_quick_actions.png) |

| AI Coach (Chat) | Goal Planner |
|---|---|
| ![Chat](docs/screenshots/ai_coach_chat.png) | ![Goal Planner](docs/screenshots/goal_planner_result.png) |

| Profile (with Admin entry point) | Admin Dashboard |
|---|---|
| ![Profile](docs/screenshots/profile_admin_entry.png) | ![Admin Dashboard](docs/screenshots/admin_dashboard.png) |

---

## Architecture

```
Android App (Kotlin Multiplatform + Jetpack Compose)
        |  HTTPS / JSON (Ktor client)
        v
FastAPI Backend (Python)
  |-- Auth (JWT + bcrypt + SQLAlchemy/SQLite)
  |-- PDF Extraction -> Chunking -> Embedding -> Qdrant (vector DB)
  |-- Retrieval -> LLM (Groq) -> Guardrails -> Response
  |-- LangGraph Agent (Goal Planner) -> Deterministic math + LLM narrative
  |-- Deterministic Financial Summary (health score, category breakdown)
  \-- Audit Logging -> Admin Dashboard (usage metrics, interaction logs)
```

---

## Setup

### Backend
```bash
cd finpilot-backend
python -m venv venv
venv\Scripts\activate        # Windows
pip install -r requirements.txt

# .env file:
# GROQ_API_KEY=your_key_here
# JWT_SECRET_KEY=your_random_secret

uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Android
1. Open in Android Studio.
2. Point `FinPilotApi`'s `baseUrl` at `http://10.0.2.2:8000` (emulator), your LAN IP (physical device — also update `network_security_config.xml`), or your deployed backend URL.
3. Run on emulator or device.

### Evaluation
```bash
python eval_suite.py
```
Runs 12 categorized prompts against the live server — Retrieval Accuracy, Routing, Safety/Guardrails, Response Quality.

---

<br>

# Technical Documentation

Every component, backend and Android: what it does, the concepts it demonstrates, and how it's implemented.

## Backend Components

### 1. App Configuration
**File:** `app/core/config.py` · **Concepts:** Twelve-Factor App config, environment-based settings

A single `Settings` class reads every config value from environment variables (`.env`, via `python-dotenv`): `UPLOAD_DIR`, `QDRANT_LOCAL_ONLY`/`QDRANT_URL`, `EMBEDDING_MODEL`, `GROQ_API_KEY`/`GROQ_MODEL`, `JWT_SECRET_KEY`, `JWT_EXPIRE_MINUTES`, `DATABASE_URL`. Backend/embedding/LLM implementations are swappable via env flags (`EMBEDDING_BACKEND`, `LLM_BACKEND`), enabling offline/mock testing without hitting real APIs.

### 2. FastAPI Application Entrypoint
**File:** `app/main.py` · **Concepts:** REST API composition, dependency injection, CORS, ASGI lifecycle

Wires every feature router (`auth`, `upload`, `analyze`, `chat`, `goal_plan`, `admin`) into one app via `include_router()` with distinct URL prefixes. `@app.on_event("startup")` calls `init_db()` to create SQL tables before the app accepts traffic.

### 3. PDF Extraction
**File:** `app/services/pdf_extraction.py` · **Concepts:** Third-party library integration, defensive error handling

Extracts raw text and detected tables from every page via pdfplumber. Wraps all extraction failures (encrypted files, corrupt PDFs) into a single `ValueError`, so the router layer returns a clean HTTP response instead of a 500.

### 4. Transaction-Aware Chunking
**File:** `app/services/chunking.py` · **Concepts:** RAG chunking strategy design, regex pattern matching

A regex (`TRANSACTION_LINE_RE`) detects `date + description + amount` patterns line-by-line. Consecutive transaction lines group into chunks of up to `CHUNK_MAX_LINES` (12), each prefixed with a header so a retrieved chunk is self-contained context even without its neighbors. Deliberately domain-specific — naive fixed-size chunking would split a transaction mid-line, damaging retrieval accuracy.

### 5. Embeddings & Vector Storage
**File:** `app/services/embedding.py` · **Concepts:** Embeddings, vector databases, semantic search, idempotency

FastEmbed (`BAAI/bge-small-en-v1.5`) generates embeddings locally. Qdrant stores each chunk as a point with payload `{chunk_id, chunk_type, text, filename}`. Supports in-process embedded Qdrant (no Docker) or a real server/cloud instance. `store_chunks()` deletes existing points for a filename before inserting new ones — idempotency added after discovering re-uploads were silently duplicating data.

### 6. Retrieval
**File:** `app/services/retrieval.py` · **Concepts:** Semantic search, metadata filtering, vector similarity ranking

`retrieve()` — top-k semantic search via Qdrant's `query_points`, optionally filtered by filename. `get_all_chunks()` — full unranked retrieval via Qdrant's `scroll` API, used by financial calculations that need every transaction. `get_all_chunks(filename=None)` returns nothing rather than everything across all statements — a data-isolation safeguard.

### 7. LLM Wrapper
**File:** `app/services/llm.py` · **Concepts:** Prompt engineering, structured output extraction

Single choke-point for all Groq calls: `generate_answer()`/`chat_completion()` (RAG prompts instructing answer-only-from-context + citation), `extract_goal_details()` (structured JSON extraction with defensive markdown-fence stripping), `generate_plan_narrative()` (the only LLM call inside the agent — every number is pre-computed, never generated).

### 8. `/analyze` and `/chat` Endpoints (RAG)
**Files:** `app/routers/analyze.py`, `chat.py` · **Concepts:** RAG pattern, multi-turn state, citation tracking

Both retrieve top-k chunks, build context, call the LLM, pass the answer through `apply_response_guardrails()`. `/chat` accepts full message history. Both return `sources: List[str]` — rendered as tappable citation chips in the app, visible proof answers are grounded. Both log to the audit system on success and failure.

### 9. Deterministic Financial Summary
**File:** `app/services/financial_summary.py` · **Concepts:** Deterministic computation, rule-based classification

Computes Home Dashboard numbers with plain Python — **not** an LLM call. Categorizes transactions via keyword matching. Health score: baseline 50, `+ savings_rate * 1.2`, `− 20` if surplus negative, clamped `[0,100]`. Income comes from statement text or an explicit `monthly_income_override` (added after statement-parsed income proved unreliable — a statement shows balance, not salary).

### 10. LangGraph Goal Planner Agent
**Files:** `app/services/agent/{state,nodes,graph}.py` · **Concepts:** LangGraph, agentic workflows, conditional state machines, self-correction

`GoalPlanState` threads through six nodes: `classify_goal` (LLM structured extraction) → `retrieve_context` (real income/expense data — the "tool use" step) → `generate_plan` (deterministic math + capped realistic recommendations: cuts ≤30%/category, income-increase ≤20%) → `validate_plan` (checks `unaddressed_gap`, not just "did a recommendation exist") → `extend_timeline` (self-correction, capped per goal type so absurd goals can't "solve" via a centuries-long timeline) → `format_result`.

`add_conditional_edges()` after `validate_plan` is the actual agentic mechanism: it inspects `is_feasible`/`retry_count` at runtime and routes to a **different node** depending on outcome — looping back (max 2 retries) or finalizing. This branching, decided from the agent's own output, is what a fixed pipeline structurally cannot do.

### 11. Guardrails
**File:** `app/services/guardrails.py` · **Concepts:** AI safety engineering, fail-safe design

- `check_numeric_consistency()` — extracts numbers via regex (capturing only the numeric group, not a currency prefix), compares by float value against source text
- `check_advice_boundary()` — regex catches product-recommendation phrasing, substitutes a safe disclaimer
- `check_context_sufficiency()` — integer threshold check **before** the LLM call, short-circuiting entirely rather than filtering output after

Applied identically at every LLM touchpoint: `/chat`, `/analyze`, and the agent's narrative generation.

### 12. Authentication
**Files:** `app/core/security.py`, `models/db.py`, `routers/auth.py` · **Concepts:** Password hashing, JWT, ORM, enumeration-safe errors

`hash_password()`/`verify_password()` call `bcrypt` directly (not via `passlib`, which has a known compatibility bug with modern bcrypt). `create_access_token()` signs a 7-day JWT. SQLAlchemy `User` model, SQLite locally, swappable to Postgres via `DATABASE_URL`. `/auth/login` returns the **identical** error for "no such user" and "wrong password" to prevent username enumeration.

**Known scope cut:** tokens are issued but not yet enforced via `Depends(get_current_user)` on the AI endpoints.

### 13. Audit Logging & Admin Endpoints
**Files:** `models/db.py` (`AuditLog`), `services/audit.py`, `routers/admin.py` · **Concepts:** Observability, structured logging, aggregation

`AuditLog` table: interaction type, truncated request/response summaries (never full payloads), success, latency, error message, timestamp. Logged on every AI endpoint call, success and failure. `GET /admin/logs` (paginated, filterable), `GET /admin/metrics` (aggregate counts, avg latency, success rate, 7-day trend). The "second persona" ops surface — separate from the end-user experience.

### 14. Evaluation Suite
**File:** `eval_suite.py` · **Concepts:** LLM evaluation methodology, automated + human-judged testing

12 prompts against the **live** server across 4 categories: Retrieval Accuracy, Routing, Safety/Guardrails (all automated), Response Quality (mixed — feasibility checks automated, narrative quality printed for human judgment).

---

## Android Components (Kotlin Multiplatform)

### 1. Project Structure
**Dirs:** `shared/` (KMP), `androidApp/` (Android-only) · **Concepts:** KMP, separation of concerns

`shared/src/commonMain` holds models, networking, ViewModels — compiles for any KMP target. `androidApp/` is Compose UI only.

### 2. expect/actual Platform Abstraction
**Files:** `HttpClientFactory.kt` (+ `.android.kt`) · **Concepts:** KMP's core abstraction mechanism

`commonMain` declares `expect fun createHttpClient(): HttpClient`; `androidMain` provides `actual` using Ktor's `OkHttp` engine. Adding iOS later needs only a new `actual` (Darwin engine).

### 3. Data Models
**File:** `Models.kt` · **Concepts:** kotlinx.serialization, DTO pattern

`@Serializable` data classes mirroring backend Pydantic schemas field-for-field, no manual key-mapping needed.

### 4. Networking Layer
**File:** `FinPilotApi.kt` · **Concepts:** Ktor client, suspend functions

One `suspend fun` per backend endpoint. `baseUrl` is environment-dependent (emulator/device/production).

### 5. State Management (ViewModels)
**Files:** `HomeViewModel`, `ChatViewModel`, `UploadViewModel`, `GoalPlannerViewModel`, `AnalyzeViewModel`, `AuthViewModel`, `AdminViewModel` · **Concepts:** MVVM, reactive state, structured concurrency

Shared `UiState<T>` sealed class (`Loading`/`Success`/`Error`). Each ViewModel is a plain Kotlin class (KMP-shareable, not `androidx.lifecycle.ViewModel`) with a `CoroutineScope` cancelled via `onCleared()`, exposing `StateFlow`.

### 6. Session & Profile State
**Files:** `TokenStore.kt`, `UserProfileStore.kt` · **Concepts:** Singleton pattern, session state

In-memory holders for the JWT and user-entered monthly income override. Reset on app restart — a known scope cut.

### 7. Navigation
**File:** `MainActivity.kt` · **Concepts:** Jetpack Navigation Compose, single-activity pattern

`NavHost` defines all routes. Bottom nav hidden on `login`. Login clears back stack (`popUpTo("login"){inclusive=true}`); logout clears everything (`popUpTo(0)`).

### 8. UI Screens
**Dir:** `ui/screens/*.kt` · **Concepts:** Jetpack Compose, Material3, state hoisting

`LoginScreen`, `HomeDashboardScreen` (custom Canvas gauge + Quick Actions), `ChatScreen`, `StatementUploadScreen` (upload + inline Q&A with citations), `GoalPlannerCreateScreen`/output, `ProfileScreen` (income input, Admin entry point, real logout), `AdminScreen` (metrics + filterable logs).

### 9. Custom Canvas Drawing
**File:** `HomeDashboardScreen.kt — HealthScoreGauge()` · **Concepts:** Compose Canvas API, `drawArc`

Two overlapping arcs: gray background track, colored foreground arc with sweep proportional to score. Color bands by score threshold.

### 10. Theming
**File:** `Theme.kt` · **Concepts:** Material3 design tokens

Custom `lightColorScheme()` matching the product's soft-purple identity, custom `Shapes()` corner radii.

---

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | Liveness check |
| POST | `/auth/signup`, `/auth/login` | Username/password auth, returns JWT |
| POST | `/upload` | Upload + extract + chunk + embed a statement PDF |
| POST | `/analyze` | RAG Q&A scoped to one statement |
| POST | `/chat` | Multi-turn AI Coach conversation |
| GET | `/chat/summary` | Deterministic financial health summary |
| POST | `/goal-plan` | Runs the LangGraph Goal Planner agent |
| GET | `/admin/logs` | Filterable interaction log (paginated) |
| GET | `/admin/metrics` | Usage metrics: totals, avg latency, success rate, 7-day trend |

---

## Cross-Cutting Design Principles

1. **Deterministic vs. generative separation** — every number a user might act on financially is computed in plain code; the LLM only narrates.
2. **Guardrails at every LLM touchpoint** — `/chat`, `/analyze`, and the agent's narrative generation all pass through the same guardrail functions, not duplicated logic.
3. **Idempotency where it matters** — re-uploading a statement replaces, not duplicates, its data.
4. **Fail loud into the audit log, fail soft to the user** — exceptions are caught, logged with full context, and surfaced to the user as a clean message, never a raw 500.

---

## Known Issues & Scope Cuts

- JWT tokens are issued but not yet enforced on `/chat`, `/analyze`, `/goal-plan`
- Session state (auth token, income override) is in-memory only — lost on app restart
- Income detection from statement text is inherently unreliable — addressed via an explicit user-entered income field in Profile

---

## Concepts Demonstrated

RAG · Vector Search (Qdrant) · Embeddings (FastEmbed) · LangGraph Agentic Orchestration · Conditional State Machines · Self-Correction Loops · AI Safety Guardrails · Prompt Engineering · Evaluation Methodology · Deterministic/Generative Separation · REST API Design · JWT Auth · ORM · Kotlin Multiplatform · MVVM · Reactive State (StateFlow) · Declarative UI (Compose)
