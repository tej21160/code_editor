const BASE_URL = import.meta.env.VITE_API_URL || 'https://codeeditor-production-f2d2.up.railway.app';

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
