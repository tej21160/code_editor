import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function DashboardPage() {
  const userId = localStorage.getItem('userId');
  const username = localStorage.getItem('username');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleCreateRoom = async () => {
    setError('');
    try {
      const response = await fetch('http://localhost:8080/api/rooms/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: `room-${Date.now()}`,
          createdBy: userId,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        navigate(`/room/${data.id}`);
      } else {
        setError('Failed to create room');
      }
    } catch (err) {
      setError('An error occurred. Please try again.');
    }
  };

  const handleJoinRoom = () => {
    const roomId = prompt('Enter room ID:');
    if (roomId) {
      navigate(`/room/${roomId}`);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    navigate('/login');
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px' }}>
      <h2>Welcome, {username}</h2>
      <button onClick={handleCreateRoom} style={{ padding: '10px 20px', margin: '10px' }}>
        Create Room
      </button>
      <button onClick={handleJoinRoom} style={{ padding: '10px 20px', margin: '10px' }}>
        Join Room
      </button>
      <button onClick={handleLogout} style={{ padding: '10px 20px', margin: '10px' }}>
        Logout
      </button>
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </div>
  );
}
