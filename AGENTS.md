# CollabEditor - AI Agent Context

## Project Structure
- Backend: /collabeditor (Spring Boot, Java 17, port 8080)
- Frontend: /collabeditor-frontend (React + Vite, port 5173)

## Stack
- Spring Boot 3.5, PostgreSQL, Redis, WebSocket/STOMP, JWT (jjwt 0.11.5)
- React 19, Vite, CodeMirror, @stomp/stompjs, sockjs-client

## Packages
- Backend: com.collabeditor
- Subpackages: entity, model, repository, service, controller, config

## Current Features (Done)
- REST APIs: register, login, rooms
- WebSocket real-time sync with STOMP
- Redis: session state, rate limiting (10/sec), versioning
- Operational Transform: insert/delete conflict resolution
- CodeMirror editor with JS/Python/Java support
- PostgreSQL snapshots every 30 edits
- Auto-reconnection with heartbeats

## Next: JWT auth (jjwt 0.11.5 already in pom.xml)