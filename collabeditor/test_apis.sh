#!/bin/bash
set -e

# Kill any existing server and clear port 8080
echo "Clearing existing processes..."
pkill -f "java.*spring-boot:run" 2>/dev/null || true
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

# Start the server
echo "Starting server..."
rm -f server.log
./mvnw spring-boot:run > server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

# Wait for server to start (up to 30 seconds)
echo "Waiting for server to start..."
TIMEOUT=30
while [ $TIMEOUT -gt 0 ]; do
  if grep -q "Started CollabEditorApplication" server.log; then
    echo "Server started!"
    break
  fi
  sleep 1
  TIMEOUT=$((TIMEOUT-1))
done

if [ $TIMEOUT -eq 0 ]; then
  echo "Error: Server did not start in time"
  cat server.log
  kill $SERVER_PID 2>/dev/null || true
  exit 1
fi

# Give it a second to fully initialize
sleep 2

# Now run the curl commands and capture the output

# 1. Register
echo ""
echo "=== 1. Register user ==="
REGISTER_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "test123"}' || echo "CURL_FAILED")
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | sed -n '1,$s/HTTP_STATUS:[0-9][0-9][0-9]$//p')
REGISTER_STATUS=$(echo "$REGISTER_RESPONSE" | tr -d '\n' | sed -n 's/.*HTTP_STATUS:\([0-9][0-9][0-9]\)/\1/p')
echo "Response body: $REGISTER_BODY"
echo "HTTP status: $REGISTER_STATUS"

# 2. Login
echo ""
echo "=== 2. Login user ==="
LOGIN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "test123"}' || echo "CURL_FAILED")
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed -n '1,$s/HTTP_STATUS:[0-9][0-9][0-9]$//p')
LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | tr -d '\n' | sed -n 's/.*HTTP_STATUS:\([0-9][0-9][0-9]\)/\1/p')
echo "Response body: $LOGIN_BODY"
echo "HTTP status: $LOGIN_STATUS"

# Extract userId from login response
USER_ID=""
if [ "$LOGIN_STATUS" = "200" ]; then
  USER_ID=$(echo "$LOGIN_BODY" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
fi
echo "Extracted userId: $USER_ID"

# 3. Create room
echo ""
echo "=== 3. Create room ==="
if [ -n "$USER_ID" ]; then
  CREATE_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/rooms/create \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"my-first-room\", \"createdBy\": \"$USER_ID\"}" || echo "CURL_FAILED")
  CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed -n '1,$s/HTTP_STATUS:[0-9][0-9][0-9]$//p')
  CREATE_STATUS=$(echo "$CREATE_RESPONSE" | tr -d '\n' | sed -n 's/.*HTTP_STATUS:\([0-9][0-9][0-9]\)/\1/p')
  echo "Response body: $CREATE_BODY"
  echo "HTTP status: $CREATE_STATUS"
  
  # Extract roomId from create room response
  ROOM_ID=""
  if [ "$CREATE_STATUS" = "200" ]; then
    ROOM_ID=$(echo "$CREATE_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
  fi
  echo "Extracted roomId: $ROOM_ID"
else
  echo "Skipping create room because login failed"
  CREATE_BODY=""
  CREATE_STATUS=""
  ROOM_ID=""
fi

# 4. Get room details
echo ""
echo "=== 4. Get room details ==="
if [ -n "$ROOM_ID" ]; then
  GET_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET http://localhost:8080/api/rooms/$ROOM_ID || echo "CURL_FAILED")
  GET_BODY=$(echo "$GET_RESPONSE" | sed -n '1,$s/HTTP_STATUS:[0-9][0-9][0-9]$//p')
  GET_STATUS=$(echo "$GET_RESPONSE" | tr -d '\n' | sed -n 's/.*HTTP_STATUS:\([0-9][0-9][0-9]\)/\1/p')
  echo "Response body: $GET_BODY"
  echo "HTTP status: $GET_STATUS"
else
  echo "Skipping get room details because room creation failed"
  GET_BODY=""
  GET_STATUS=""
fi

# Stop the server
echo ""
echo "=== Stopping server ==="
kill $SERVER_PID 2>/dev/null || true
wait $SERVER_PID 2>/dev/null || true
echo "Server stopped"

# Output summary
echo ""
echo "=== SUMMARY ==="
echo "Register: $REGISTER_STATUS"
echo "Login: $LOGIN_STATUS"
echo "Create room: $CREATE_STATUS"
echo "Get room details: $GET_STATUS"