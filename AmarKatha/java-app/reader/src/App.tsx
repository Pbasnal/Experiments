import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import SeriesPage from './pages/SeriesPage';
import ChapterReaderPage from './pages/ChapterReaderPage';
import ProfilePage from './pages/ProfilePage';

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/read/s/:seriesSlug" element={<SeriesPage />} />
        <Route path="/read/s/:seriesSlug/c/:chapterSlug" element={<ChapterReaderPage />} />
        <Route path="/read/profile" element={<ProfilePage />} />
      </Routes>
    </Layout>
  );
}
