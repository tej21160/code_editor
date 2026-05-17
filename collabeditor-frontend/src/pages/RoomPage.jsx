import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { connectToRoom, sendCodeChange, disconnectFromRoom } from '../services/websocket';
import CodeEditor from '../components/CodeEditor';
import { authFetch } from '../utils/api';

export default function RoomPage() {
  const { roomId } = useParams();
  const userId = localStorage.getItem('userId');
  const username = localStorage.getItem('username');
  const navigate = useNavigate();

  const [code, setCode] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('javascript');
  const [connectionStatus, setConnectionStatus] = useState('connecting');
  const [connectedUsers, setConnectedUsers] = useState([]);
  const [typingUsers, setTypingUsers] = useState([]);
  const [copySuccess, setCopySuccess] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const [snapshots, setSnapshots] = useState([]);
  const typingTimeouts = useRef({});
  const usersRef = useRef(new Map()); // userId -> username

  // Copy room ID to clipboard
  const handleCopyRoomId = async () => {
    try {
      await navigator.clipboard.writeText(roomId);
      setCopySuccess('Copied!');
      setTimeout(() => setCopySuccess(''), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  useEffect(() => {
    // Fetch existing code snapshot before connecting
    authFetch(`https://codeeditor-production-f2d2.up.railway.app/api/room-state/${roomId}/code`)
      .then(res => res.json())
      .then(data => {
        if (data.code && data.code.length > 0) {
          setCode(data.code);
        }
      })
      .catch(err => console.log('No existing code:', err));

    // Fetch existing users in the room
    authFetch(`https://codeeditor-production-f2d2.up.railway.app/api/room-state/${roomId}/users`)
      .then(res => res.json())
      .then(data => {
        if (data && Object.keys(data).length > 0) {
          Object.entries(data).forEach(([uid, uname]) => {
            if (!usersRef.current.has(uid)) {
              usersRef.current.set(uid, uname);
            }
          });
          setConnectedUsers(Array.from(usersRef.current.values()));
        }
      })
      .catch(err => console.log('No existing users:', err));

    const onMessageReceived = (message) => {
      // Detect join message (empty content, version 0)
      if (message.content === '' && message.version === 0) {
        // Add user to connected users
        if (!usersRef.current.has(message.userId)) {
          usersRef.current.set(message.userId, message.username);
          setConnectedUsers(Array.from(usersRef.current.values()));
        }
      } else {
        // Code change message from another user
        if (message.userId !== userId) {
          setCode(message.content);
          
          // Add to typing users
          setTypingUsers(prev => {
            const next = [...new Set([...prev, message.username])];
            return next;
          });
          
          // Clear previous timeout and set new one
          if (typingTimeouts.current[message.userId]) {
            clearTimeout(typingTimeouts.current[message.userId]);
          }
          
          // Remove from typing after 2 seconds
          typingTimeouts.current[message.userId] = setTimeout(() => {
            setTypingUsers(prev => {
              const next = prev.filter(u => u !== message.username);
              return next;
            });
            delete typingTimeouts.current[message.userId];
          }, 2000);
        }
      }
    };

    const onConnected = () => {
      setConnectionStatus('connected');
      
      // Add current user to connected users
      if (!usersRef.current.has(userId)) {
        usersRef.current.set(userId, username);
        setConnectedUsers(Array.from(usersRef.current.values()));
      }
    };

    connectToRoom(
      roomId, userId, username,
      onMessageReceived,
      onConnected,
      () => setConnectionStatus('disconnected')
    );

    return () => {
      disconnectFromRoom();
      usersRef.current.clear();
      Object.values(typingTimeouts.current).forEach(timeout => clearTimeout(timeout));
      typingTimeouts.current = {};
    };
  }, [roomId, userId, username]);

  const handleCodeChange = (value) => {
    setCode(value);
    
    // Send code change via WebSocket
    const version = Date.now();
    sendCodeChange(roomId, userId, username, value, version);
  };

  const handleHistoryClick = async () => {
    try {
      const response = await authFetch(`https://codeeditor-production-f2d2.up.railway.app/api/room-state/${roomId}/history`);
      const data = await response.json();
      setSnapshots(data || []);
      setShowHistory(!showHistory);
    } catch (err) {
      console.error('Failed to fetch history:', err);
    }
  };

  const handleRestoreSnapshot = (snapshot) => {
    setCode(snapshot.content);
    const version = Date.now();
    sendCodeChange(roomId, userId, username, snapshot.content, version);
    setShowHistory(false);
  };

  const handleCloseHistory = () => {
    setShowHistory(false);
  };

  const handleLeaveRoom = () => {
    disconnectFromRoom();
    navigate('/dashboard');
  };

  return (
    <div style={{ 
      display: 'flex', 
      minHeight: '100vh', 
      fontFamily: 'system-ui, -apple-system, sans-serif'
    }}>
      {/* Reconnection banner */}
      {connectionStatus === 'disconnected' && (
        <div style={{
          width: '100%',
          backgroundColor: '#fff3cd',
          color: '#856404',
          padding: '10px',
          textAlign: 'center',
          borderBottom: '1px solid #ffeeba',
          fontSize: '14px',
          fontWeight: 'bold'
        }}>
          ⚠️ Connection lost. Reconnecting...
        </div>
      )}
      
      {/* Left Side - 70% */}
      <div style={{ 
        flex: '0 0 70%', 
        padding: '20px',
        borderRight: '1px solid #e0e0e0',
        backgroundColor: '#1e1e1e'
      }}>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          marginBottom: '20px',
          flexWrap: 'wrap',
          gap: '10px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1 style={{ margin: 0, fontSize: '24px' }}>Room: {roomId}</h1>
            <button
              onClick={handleCopyRoomId}
              style={{
                padding: '6px 12px',
                fontSize: '14px',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                transition: 'background-color 0.2s'
              }}
              onMouseOver={e => e.target.style.backgroundColor = '#0056b3'}
              onMouseOut={e => e.target.style.backgroundColor = '#007bff'}
            >
              📋 Copy Room ID
            </button>
            {copySuccess && (
              <span style={{ 
                color: '#28a745', 
                fontWeight: 'bold',
                animation: 'fadeIn 0.3s',
                backgroundColor: '#d4edda',
                padding: '4px 8px',
                borderRadius: '4px'
              }}>
                {copySuccess}
              </span>
            )}
          </div>
          
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px'
          }}>
            <span style={{ 
              fontSize: '14px',
              backgroundColor: connectionStatus === 'connected' ? '#d4edda' : '#f8d7da',
              color: connectionStatus === 'connected' ? '#155724' : '#721c24',
              padding: '6px 12px',
              borderRadius: '20px',
              fontWeight: 'bold',
              border: '1px solid',
              borderColor: connectionStatus === 'connected' ? '#c3e6cb' : '#f5c6cb'
            }}>
              {connectionStatus === 'connected' ? '✅ Connected' : 
               connectionStatus === 'disconnected' ? '❌ Disconnected' : 
               '🔄 Connecting...'}
            </span>
          </div>
        </div>

        <div style={{ marginBottom: '20px' }}>
          <select 
            value={selectedLanguage} 
            onChange={e => setSelectedLanguage(e.target.value)}
            style={{
              padding: '6px 12px',
              borderRadius: '4px',
              border: '1px solid #333',
              backgroundColor: '#2d2d2d',
              color: '#d4d4d4',
              fontSize: '14px',
              cursor: 'pointer'
            }}
          >
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
            <option value="java">Java</option>
          </select>
        </div>

        <div style={{ marginBottom: '10px' }}>
          <button
            onClick={handleHistoryClick}
            style={{
              padding: '8px 16px',
              backgroundColor: '#6c757d',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '14px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            📜 History
          </button>
        </div>
        <CodeEditor 
          code={code} 
          onChange={handleCodeChange}
          language={selectedLanguage}
        />
        {showHistory && (
          <div style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            bottom: '20px',
            width: '320px',
            backgroundColor: '#fff',
            border: '1px solid #dee2e6',
            borderRadius: '8px',
            padding: '20px',
            zIndex: 1000,
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            display: 'flex',
            flexDirection: 'column'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '15px',
              paddingBottom: '10px',
              borderBottom: '1px solid #dee2e6'
            }}>
              <h3 style={{ margin: 0, fontSize: '18px', color: '#495057' }}>📜 Snapshot History</h3>
              <button
                onClick={handleCloseHistory}
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#6c757d',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '12px'
                }}
              >
                Close
              </button>
            </div>
            <div style={{ flex: 1, overflowY: 'auto' }}>
              {snapshots.length === 0 ? (
                <div style={{ padding: '20px', textAlign: 'center', color: '#6c757d' }}>
                  No snapshots found
                </div>
              ) : (
                snapshots.map((s, i) => (
                  <div
                    key={i}
                    style={{
                      padding: '12px',
                      marginBottom: '10px',
                      backgroundColor: '#f8f9fa',
                      border: '1px solid #e9ecef',
                      borderRadius: '6px'
                    }}
                  >
                    <div style={{ fontSize: '12px', color: '#6c757d', marginBottom: '6px' }}>
                      Version: {s.version}
                    </div>
                    <div style={{ fontSize: '12px', marginBottom: '8px' }}>
                      {new Date(s.savedAt).toLocaleString()}
                    </div>
                    <button
                      onClick={() => handleRestoreSnapshot(s)}
                      style={{
                        padding: '6px 12px',
                        backgroundColor: '#198754',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '12px',
                        width: '100%'
                      }}
                    >
                      Restore
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {typingUsers.length > 0 && (
          <div style={{
            marginTop: '10px',
            padding: '10px 15px',
            backgroundColor: '#e3f2fd',
            borderLeft: '4px solid #2196f3',
            borderRadius: '4px',
            fontSize: '14px',
            color: '#1976d2'
          }}>
            ✏️ {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
          </div>
        )}

        <div style={{ marginTop: '20px', textAlign: 'center' }}>
          <button 
            onClick={handleLeaveRoom}
            style={{
              padding: '12px 24px',
              backgroundColor: '#dc3545',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: 'bold',
              transition: 'all 0.2s',
              boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
            }}
            onMouseOver={e => {
              e.target.style.backgroundColor = '#c82333';
              e.target.style.transform = 'translateY(-2px)';
              e.target.style.boxShadow = '0 4px 8px rgba(0,0,0,0.15)';
            }}
            onMouseOut={e => {
              e.target.style.backgroundColor = '#dc3545';
              e.target.style.transform = 'translateY(0)';
              e.target.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';
            }}
          >
            🚪 Leave Room
          </button>
        </div>
      </div>

      {/* Right Side - 30% */}
      <div style={{ 
        flex: '0 0 30%', 
        padding: '20px',
        backgroundColor: '#fff',
        borderLeft: '1px solid #e0e0e0'
      }}>
        <div style={{ 
          backgroundColor: '#f8f9fa',
          padding: '15px',
          borderRadius: '8px',
          border: '1px solid #e9ecef',
          position: 'sticky',
          top: '20px'
        }}>
          <h2 style={{ 
            margin: '0 0 15px 0', 
            fontSize: '18px',
            color: '#495057',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            👥 Connected Users
            <span style={{
              fontSize: '12px',
              backgroundColor: '#6c757d',
              color: 'white',
              padding: '2px 8px',
              borderRadius: '10px'
            }}>
              {connectedUsers.length}
            </span>
          </h2>
          
          <ul style={{ 
            listStyle: 'none', 
            padding: 0, 
            margin: 0,
            maxHeight: '400px',
            overflowY: 'auto'
          }}>
            {connectedUsers.length === 0 ? (
              <li style={{ 
                padding: '10px',
                color: '#6c757d',
                fontStyle: 'italic',
                textAlign: 'center'
              }}>
                No users connected yet
              </li>
            ) : (
              connectedUsers.map((user, index) => (
                <li 
                  key={index}
                  style={{
                    padding: '10px 15px',
                    marginBottom: '8px',
                    backgroundColor: user === username ? '#d4edda' : '#e9ecef',
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: user === username ? '#c3e6cb' : '#dee2e6',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '14px',
                    fontWeight: user === username ? 'bold' : 'normal'
                  }}
                >
                  <span style={{ fontSize: '16px' }}>🟢</span>
                  <span>{user}</span>
                  {user === username && (
                    <span style={{
                      fontSize: '11px',
                      backgroundColor: '#28a745',
                      color: 'white',
                      padding: '2px 6px',
                      borderRadius: '10px',
                      marginLeft: 'auto'
                    }}>
                      you
                    </span>
                  )}
                </li>
              ))
            )}
          </ul>
        </div>

        <div style={{
          marginTop: '20px',
          padding: '15px',
          backgroundColor: '#f8f9fa',
          borderRadius: '8px',
          border: '1px solid #e9ecef',
          fontSize: '12px',
          color: '#6c757d'
        }}>
          <strong style={{ color: '#495057' }}>ℹ️ Room Info</strong>
          <div style={{ marginTop: '10px' }}>
            <div>Session ID: {roomId.substring(0, 8)}...</div>
            <div style={{ marginTop: '5px' }}>Editor: 📝 Live Code</div>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        textarea:focus {
          outline: none;
          border-color: #007bff;
          box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25);
        }
      `}</style>
    </div>
  );
}
