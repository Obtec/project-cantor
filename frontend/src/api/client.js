import axios from 'axios';

// 기본은 동일 출처의 /api (nginx가 백엔드로 프록시). 빌드시 VITE_API_BASE로 덮어쓸 수 있다.
const baseURL = import.meta.env.VITE_API_BASE || '/api';

const client = axios.create({ baseURL });

const TOKEN_KEY = 'cantor_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      setToken(null);
      // 강제 로그아웃 상황: 로그인 페이지가 아니라면 이동
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(err);
  }
);

export function apiError(err, fallback = '요청을 처리하지 못했습니다.') {
  return err?.response?.data?.message || fallback;
}

export default client;
