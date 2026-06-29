import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import SeriesPage from './pages/SeriesPage';

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/read/s/:seriesSlug" element={<SeriesPage />} />
      </Routes>
    </Layout>
  );
}
