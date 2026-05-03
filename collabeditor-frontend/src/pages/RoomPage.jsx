import { useParams, useNavigate } from 'react-router-dom';

export default function RoomPage() {
  const { roomId } = useParams();
  const username = localStorage.getItem('username');
  const navigate = useNavigate();

  const handleLeaveRoom = () => {
    navigate('/dashboard');
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px' }}>
      <h2>Room: {roomId}</h2>
      <p>Logged in as: {username}</p>
      <p>Code editor coming in Week 2</p>
      <button onClick={handleLeaveRoom} style={{ padding: '10px 20px' }}>
        Leave Room
      </button>
    </div>
  );
}
