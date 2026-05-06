# CollabEditor — Real-Time Collaborative Code Editor

## Live Demo
[link placeholder]

## Architecture Overview

```
React Frontend (Vite + CodeMirror) 
        │
        │  WebSocket / STOMP over HTTP
        ▼
Spring Boot Backend (port 8080)
        │
        ├─ Redis (session state, rate limiting, ephemeral data)
        └─ PostgreSQL (users, rooms, code snapshots)
```

## Tech Stack

| Technology              | Purpose                              | Why chosen                                         |
|-------------------------|--------------------------------------|----------------------------------------------------|
| Spring Boot 3.5         | Backend API & WebSocket server       | Mature, reactive WebSocket/STOMP support           |
| React 19 + Vite         | Frontend SPA                         | Fast HMR, modern tooling, small bundle             |
| WebSocket / STOMP       | Real-time bidirectional messaging    | Built-in Spring support, fallback-friendly         |
| Operational Transform   | Conflict resolution for edits        | Mature, predictable, easier to debug than CRDT     |
| Redis                   | Session state & rate limiting        | Fast in-memory ops, TTL support, simple primitives |
| PostgreSQL              | Persistent storage & snapshots       | Strong consistency, JSON support, reliable backups |
| JJWT 0.11.5             | JWT authentication                   | Standard, lightweight, HS256 signing              |
| CodeMirror              | Code editor component                | Language modes, extensible, battle-tested          |

## Key Engineering Decisions

1. **WebSocket over HTTP polling**
   - Real-time code sync requires low-latency, bidirectional communication.
   - WebSocket keeps a single long-lived connection, avoiding per-poll overhead and latency (hundreds of ms vs <50 ms).
   - STOMP subprotocol gives message-routing semantics (topics, queues) without reinventing pub/sub.

2. **Operational Transform (OT) over CRDT**
   - OT is conceptually simpler to reason about for linear text edits and integrates naturally with existing code-editor change events.
   - Server-side transform function is centralized (easier to audit and evolve); CRDT would require more complex per-client state merging.
   - For a small-to-medium team editor, OT performance and correctness are easier to guarantee with snapshot/versioning.

3. **Redis for session state over DB**
   - Session presence, typing indicators, and short-lived rate-limit counters are high-frequency, ephemeral operations.
   - Redis offers O(1) reads/writes with automatic TTL expiry — no need for background cleanup jobs.
   - Offloads write amplification from PostgreSQL where strong consistency isn't required for transient state.

4. **JWT over session-based auth**
   - Stateless JWT avoids server-side session stores and simplifies horizontal scaling (no sticky sessions or session replication).
   - Each request carries authorization; WebSocket upgrade can validate the token once during handshake.
   - Works naturally across REST and WebSocket boundaries and supports mobile/third-party clients.

## System Design

### How real-time sync works (step by step)

1. Client A opens `/room/xyz`. App fetches latest code snapshot via `GET /api/room-state/{roomId}/code` (authenticated).
2. Client A connects via WebSocket (`/ws`) with `Authorization: Bearer <token>` and joins topic `/topic/room/xyz`.
3. Client A sends a `join` message to `/app/room/{roomId}/join`. Server broadcasts presence to other participants.
4. Client A types → local CodeMirror emits change → client sends `edit` operation (insert/delete, position, content, version) to `/app/room/{roomId}/edit`.
5. Server receives operation, applies OT against concurrent ops (if any), stores transformed op, and broadcasts to all clients in room (including sender if needed).
6. Each client applies the transformed operation to its local document. Because OT preserves intention, all replicas converge.

### How conflict resolution works

- Each operation carries a `version` (timestamp) and `userId`.
- On the server, `OperationalTransformService.transform(a, b)` adjusts operation `a` against concurrent operation `b` according to position and tie-breaking rules (userId lexicographic for same-position inserts).
- For insert-insert at same position, higher userId is shifted right to maintain deterministic ordering.
- For delete-delete at same position, effective length is reduced to avoid over-deletion.
- Applied operations are validated against document bounds to prevent corruption.

### How rate limiting works (token bucket)

- Per-user (identified by WebSocket session or userId) rate limiter stored in Redis with key `rate_limit:{userId}`.
- Tokens: 10 requests per second.
- Each incoming REST or WebSocket-originated action checks token count. If available, decrement and allow; otherwise reject with 429 / drop action.
- TTL automatically expires keys after inactivity.

### How reconnection works

- WebSocket client (`@stomp/stompjs`) is configured with `reconnectDelay: 5000` and `heartbeatIncoming/Outgoing: 4000`.
- On disconnect, client queues unsent operations (or discards if stale) and attempts reconnect every 5s.
- On successful reconnect, client re-joins room (sends join), fetches latest snapshot via REST, and resumes editing.
- Heartbeats detect dead connections quickly; server-side STOMP broker cleans up sessions.

## API Endpoints

| Method | Endpoint                        | Auth  | Description                                     |
|--------|----------------------------------|-------|-------------------------------------------------|
| POST   | /api/users/register             | No    | Register new user                               |
| POST   | /api/users/login                | No    | Login, returns JWT token                        |
| POST   | /api/rooms/create               | Yes   | Create a new room                               |
| GET    | /api/room-state/{id}/code       | Yes   | Get latest code snapshot for room               |
| GET    | /api/room-state/{id}/users      | Yes   | Get users currently in room (via Redis)        |
| GET    | /api/room-state/{id}/history    | Yes   | Get list of code snapshots for room (history)   |
| WS     | /ws                             | Yes   | STOMP WebSocket endpoint (topics /app/room/...) |

## WebSocket Events

| Direction | Destination / Topic                    | Payload (JSON)                                    | Purpose                                     |
|-----------|----------------------------------------|---------------------------------------------------|---------------------------------------------|
| Client→   | /app/room/{roomId}/join                | `{roomId, userId, username, content:"", version:0}` | Notify presence (join)                      |
| Client→   | /app/room/{roomId}/edit                | `{roomId, userId, username, content, version}`     | Send code edit operation                    |
| Server→   | /topic/room/{roomId}                   | `{userId, username, content, version, ...}`        | Broadcast transformed edit or join message  |

## Running Locally

### Prerequisites
- Java 17 (JDK)
- Maven (or use `./mvnw` wrapper)
- Node 20+ and npm
- PostgreSQL 16 (or Docker)
- Redis 7 (or Docker)

### Backend setup
```bash
cd collabeditor
# Configure DB (if not using defaults in application.properties)
# Start PostgreSQL and Redis (docker example)
docker run -d --name pg -e POSTGRES_DB=collabeditor -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
docker run -d --name redis -p 6379:6379 redis:7

# Compile and run
./mvnw spring-boot:run
# App starts on http://localhost:8080
```

### Frontend setup
```bash
cd collabeditor-frontend
npm install
npm run dev
# Dev server on http://localhost:5173
```

Or build:
```bash
npm run build
```

### First-time tips
- The backend disables Spring Security auto-configuration exclusion for simplicity in dev (see `application.properties`). In production, ensure SecurityConfig is properly locked down.
- JWT secret in `application.properties` is the signing key — keep it secret in prod and rotate periodically.
- Snapshots are created every 30 edits (configurable) via `SnapshotService`.

## Trade-offs & Known Limitations

- **OT is server-centric**: transform logic lives on server, which is fine for moderate scale but could become a bottleneck at very high concurrency. Sharding rooms or moving transform closer to client (with deterministic rules) would help scale writes.
- **No operational compression/merge**: Rapid typing can produce many small ops. Batching or compressing ops before broadcast would reduce bandwidth and improve convergence speed.
- **Presence is best-effort**: Redis tracks who is in a room, but abrupt disconnects may leave stale presence until TTL or heartbeat timeout. A periodic reconciliation job or explicit leave messages would improve accuracy.
- **Cursor positions not synced**: Only code content is synced. Syncing selections/cursors would require additional lightweight events.
- **File tree / multiple files**: Current scope is single-file-per-room. Supporting multi-file projects would require a document hierarchy and per-file OT scopes.
- **Snapshot retention**: Snapshots are stored indefinitely in PostgreSQL with no archiving/rotation. For long-lived rooms, implement snapshot pruning or delta chains.

## Test Coverage

| Test suite                              | Tests | Purpose                                                                 |
|-----------------------------------------|-------|-------------------------------------------------------------------------|
| OperationalTransformServiceTest         | 6     | Validate OT rules for insert/delete combinations and apply correctness |
| JwtServiceTest                          | 4     | Ensure token generation, extraction, validation, and rejection          |
| CollabEditorApplicationTests (context)  | 1     | Spring context loads with security/data stack                          |

**Why**: OT correctness is critical for convergence; JWT tests ensure auth tokens round-trip and are validated correctly. These unit tests run fast and prevent regressions in core coordination logic. Integration/e2e tests (outside this scope) would add WebSocket round-trip and conflict simulations.

---