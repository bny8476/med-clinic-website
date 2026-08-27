window.__ENV__ = {
  VITE_API_BASE_URL: window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://med-clinic-website.onrender.com/api'
};
