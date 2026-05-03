#!/bin/bash
set -e

# Kill any existing server
pkill -f "java.*spring-boot:run" 2>/dev/null || true
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

# Start the server
rm -f server.log
./mvnw spring-boot:run > server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start (up to 30 seconds)
TIMEOUT=30
while [ $TIMEOUT -gt 0 ]; do
  if grep -q "Started CollabEditorApplication" server.log; then
    echo "Server started"
    break
  fi
  sleep 1
  TIMEOUT=$((TIMEOUT-1))
done

if [ $TIMEOUT -eq 0 ]; then
  echo "Server did not start in time"
  kill $SERVER_PID 2>/dev/null || true
  exit 1
fi

# Now run the curl commands and capture the output

# 1. Register
echo "=== Registering user ==="
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "test123"}')
echo "$REGISTER_RESPONSE"

# 2. Login
echo "=== Logging in ==="
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "test123"}')
echo "$LOGIN_RESPONSE"

# Extract userId from login response
# We assume the response is JSON: {"success":true,"userId":"...","username":"testuser"}
USER_ID=$(echo "$LOGIN_RESPONSE" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
echo "Extracted userId: $USER_ID"

# 3. Create room
echo "=== Creating room ==="
CREATE_ROOM_RESPONSE=$(curl -s -X POST http://localhost:8080/api/rooms/create \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"my-first-room\", \"createdBy\": \"$USER_ID\"}")
echo "$CREATE_ROOM_RESPONSE"

# Extract roomId from create room response
# We assume the response is a JSON object with an "id" field
ROOM_ID=$(echo "$CREATE_ROOM_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Extracted roomId: $ROOM_ID"

# 4. Get room details
echo "=== Getting room details ==="
GET_ROOM_RESPONSE=$(curl -s -X GET http://localhost:8080/api/rooms/$ROOM_ID)
echo "$GET_ROOM_RESPONSE"

# Stop the server
echo "=== Stopping server ==="
kill $SERVER_PID 2>/dev/null || true
wait $SERVER_PID 2>/dev/null || true

exit 0