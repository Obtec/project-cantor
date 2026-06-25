import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import AppleTreeLogo from './AppleTreeLogo.jsx';
import NotificationBell from './NotificationBell.jsx';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const onLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="app">
      <header className="masthead">
        <div className="masthead-inner">
          <Link to="/" className="brand">
            <AppleTreeLogo size={48} />
            <div className="brand-text">
              <span className="brand-name">Cantor Journal</span>
              <span className="brand-sub">An International Open-Access Multidisciplinary Research Journal</span>
            </div>
          </Link>
          <div className="masthead-meta">
            <span>ISSN 2026–0001</span>
            <span>Vol. 1 · 2026</span>
            <span>Peer-Reviewed</span>
          </div>
        </div>

        <div className="mainnav">
          <div className="mainnav-inner">
            <nav className="nav-links">
              <NavLink to="/" end>Articles</NavLink>
              <NavLink to="/issues">Archive</NavLink>
              {user && <NavLink to="/submit">Submit Manuscript</NavLink>}
              {user && <NavLink to="/my">My Submissions</NavLink>}
              {hasRole('REVIEWER') && <NavLink to="/reviews">Review</NavLink>}
              {hasRole('EDITOR') && <NavLink to="/editor">Editorial Office</NavLink>}
            </nav>
            <div className="auth-area">
              {user ? (
                <>
                  <NotificationBell />
                  <span className="user-name">{user.name} 님</span>
                  <button className="btn-link" onClick={onLogout}>Sign out</button>
                </>
              ) : (
                <>
                  <Link to="/login">Sign in</Link>
                  <Link to="/register">Register</Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="container">
        <Outlet />
      </main>

      <footer className="footer">
        <div className="footer-inner">
          <div className="footer-title">🍎 Cantor Journal</div>
          <div className="footer-meta">
            An open-access, peer-reviewed multidisciplinary research journal · ISSN 2026–0001 · Published by the Cantor Society
          </div>
          <div className="footer-meta muted small">
            © 2026 Cantor Journal. All manuscripts are reviewed under a double-blind peer-review policy.
          </div>
        </div>
      </footer>
    </div>
  );
}
