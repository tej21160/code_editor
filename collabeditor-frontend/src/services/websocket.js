import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient = null;

export const connectToRoom = (roomId, userId, username, onMessageReceived, onConnected, onDisconnected) => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      // Subscribe to room topic
      stompClient.subscribe(`/topic/room/${roomId}`, (message) => {
        const parsed = JSON.parse(message.body);
        onMessageReceived(parsed);
      });
      // Send join notification
      stompClient.publish({
        destination: `/app/room/${roomId}/join`,
        body: JSON.stringify({ roomId, userId, username, content: '', version: 0 })
      });
      onConnected();
    },
    onDisconnect: () => {
      console.log('Disconnected from WebSocket');
      if (onDisconnected) onDisconnected();
    },
    onWebSocketClose: () => {
      console.log('WebSocket closed, reconnecting in 5s...');
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame);
    }
  });
  stompClient.activate();
};

export const sendCodeChange = (roomId, userId, username, content, version) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: `/app/room/${roomId}/edit`,
      body: JSON.stringify({ roomId, userId, username, content, version })
    });
  }
};

export const disconnectFromRoom = () => {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};
