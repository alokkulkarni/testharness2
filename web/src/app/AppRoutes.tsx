import { Navigate, Outlet, Route, Routes } from 'react-router';
import { LoginPage } from '../auth/LoginPage';
import { PublicRoute, RequireAuth } from '../auth/RequireAuth';
import { SignupPage } from '../auth/SignupPage';
import { TodoDetailPage } from '../todos/TodoDetailPage';
import { TodoFormPage } from '../todos/TodoFormPage';
import { TodoListPage } from '../todos/TodoListPage';
import { AppShell } from './AppShell';
import { NotFoundPage } from './NotFoundPage';

/** The route table (sdd §8). The catch-all is behind RequireAuth, like the to-do routes (REQ-NAV-001 AC-3, AC-8). */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/todos" replace />} />
        <Route
          path="/signup"
          element={
            <PublicRoute>
              <SignupPage />
            </PublicRoute>
          }
        />
        <Route
          path="/login"
          element={
            <PublicRoute goesToReturnTo>
              <LoginPage />
            </PublicRoute>
          }
        />
        <Route
          element={
            <RequireAuth>
              <Outlet />
            </RequireAuth>
          }
        >
          <Route path="/todos" element={<TodoListPage />} />
          <Route path="/todos/new" element={<TodoFormPage mode="create" />} />
          <Route path="/todos/:id" element={<TodoDetailPage />} />
          <Route path="/todos/:id/edit" element={<TodoFormPage mode="edit" />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
