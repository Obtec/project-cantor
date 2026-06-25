import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';

export default function RequireRole({ role, children }) {
  const { user, loading, hasRole } = useAuth();

  if (loading) return <div className="container">불러오는 중…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!hasRole(role)) {
    return (
      <div className="container">
        <h2>접근 권한 없음</h2>
        <p>이 페이지는 {role} 권한이 필요합니다.</p>
      </div>
    );
  }
  return children;
}
