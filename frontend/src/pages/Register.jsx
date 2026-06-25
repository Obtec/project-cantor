import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiError } from '../api/client.js';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', affiliation: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const update = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await register(form);
      navigate('/', { replace: true });
    } catch (err) {
      setError(apiError(err, '회원가입에 실패했습니다.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card narrow">
      <h2>회원가입</h2>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={onSubmit} className="form">
        <label>이름
          <input value={form.name} onChange={update('name')} required />
        </label>
        <label>이메일
          <input type="email" value={form.email} onChange={update('email')} required />
        </label>
        <label>비밀번호 <span className="muted">(8자 이상)</span>
          <input type="password" value={form.password} onChange={update('password')} minLength={8} required />
        </label>
        <label>소속 <span className="muted">(선택)</span>
          <input value={form.affiliation} onChange={update('affiliation')} />
        </label>
        <button className="btn btn-primary" disabled={busy}>{busy ? '처리 중…' : '가입하기'}</button>
      </form>
      <p className="muted">이미 계정이 있으신가요? <Link to="/login">로그인</Link></p>
    </div>
  );
}
