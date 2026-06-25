import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import RequireAuth from './auth/RequireAuth.jsx';
import RequireRole from './auth/RequireRole.jsx';
import PaperList from './pages/PaperList.jsx';
import PaperDetail from './pages/PaperDetail.jsx';
import AuthorProfile from './pages/AuthorProfile.jsx';
import Issues from './pages/Issues.jsx';
import IssueDetail from './pages/IssueDetail.jsx';
import SubmitPaper from './pages/SubmitPaper.jsx';
import MySubmissions from './pages/MySubmissions.jsx';
import ReviewDashboard from './pages/ReviewDashboard.jsx';
import EditorDashboard from './pages/EditorDashboard.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<PaperList />} />
        <Route path="papers/:id" element={<PaperDetail />} />
        <Route path="authors/:id" element={<AuthorProfile />} />
        <Route path="issues" element={<Issues />} />
        <Route path="issues/:id" element={<IssueDetail />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="submit" element={<RequireAuth><SubmitPaper /></RequireAuth>} />
        <Route path="my" element={<RequireAuth><MySubmissions /></RequireAuth>} />
        <Route path="reviews" element={<RequireRole role="REVIEWER"><ReviewDashboard /></RequireRole>} />
        <Route path="editor" element={<RequireRole role="EDITOR"><EditorDashboard /></RequireRole>} />
        <Route path="*" element={<div><h2>페이지를 찾을 수 없습니다.</h2></div>} />
      </Route>
    </Routes>
  );
}
