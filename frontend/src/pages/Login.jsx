import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiError } from '../api/client.js';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(email, password);
      const to = location.state?.from?.pathname || '/';
      navigate(to, { replace: true });
    } catch (err) {
      setError(apiError(err, '로그인에 실패했습니다.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card narrow">
      <h2>로그인</h2>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={onSubmit} className="form">
        <label>이메일
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>비밀번호
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-primary" disabled={busy}>{busy ? '처리 중…' : '로그인'}</button>
      </form>
      <p className="muted">계정이 없으신가요? <Link to="/register">회원가입</Link></p>
    </div>
  );
}
