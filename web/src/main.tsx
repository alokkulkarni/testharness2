import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { AppErrorBoundary } from './app/AppErrorBoundary';
import { AppRoutes } from './app/AppRoutes';
import { SessionProvider } from './auth/SessionProvider';
import './styles/tokens.css';
import './styles/app.css';

// Composition (sdd §6.1, §8): router, then the session state, then the error boundary around the routes.
createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <SessionProvider>
      <AppErrorBoundary>
        <AppRoutes />
      </AppErrorBoundary>
    </SessionProvider>
  </BrowserRouter>,
);
