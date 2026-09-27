import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { FeatureProvider } from './features/FeatureContext';
import { initTelemetry } from './telemetry/initTelemetry';
import './index.css';

initTelemetry();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <FeatureProvider>
        <App />
      </FeatureProvider>
    </BrowserRouter>
  </StrictMode>,
);
