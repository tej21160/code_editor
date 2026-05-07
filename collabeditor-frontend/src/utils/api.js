const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const authFetch = (url, options = {}) => {
  const token = localStorage.getItem('token');
  return fetch(`${BASE_URL}${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers
    }
  });
};
