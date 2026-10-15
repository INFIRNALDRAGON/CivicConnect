import React from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import { homePathFor } from './utils/roles';
import ProtectedRoute from './components/common/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';
import EmptyState from './components/common/EmptyState';
import { buttonClass } from './components/common/Button';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import SubmitRequestPage from './pages/requester/SubmitRequestPage';
import MyRequestsPage from './pages/requester/MyRequestsPage';
import RequesterRequestDetailPage from './pages/requester/RequestDetailPage';
import RequestQueuePage from './pages/staff/RequestQueuePage';
import StaffRequestDetailPage from './pages/staff/RequestDetailPage';
import DashboardPage from './pages/management/DashboardPage';

const Home: React.FC = () => {
  const { user } = useAuth();
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />;
};

const NotFound: React.FC = () => {
  const { user } = useAuth();
  return (
    <div className='max-w-3xl p-6'>
      <EmptyState
        title="This page does not exist"
        action={
          <Link className={buttonClass('primary', 'md')} to={user ? homePathFor(user.role) : '/login'}>
            Go to your home page
          </Link>
        }
      >
        Check the address, or go back to where you started.
      </EmptyState>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Requester */}
      <Route element={<ProtectedRoute roles={['REQUESTER']} />}>
        <Route element={<AppLayout />}>
          <Route path="/submit" element={<SubmitRequestPage />} />
          <Route path="/requests" element={<MyRequestsPage />} />
          <Route path="/requests/:id" element={<RequesterRequestDetailPage />} />
        </Route>
      </Route>

      {/* Staff */}
      <Route element={<ProtectedRoute roles={['STAFF']} />}>
        <Route element={<AppLayout />}>
          <Route path="/staff/queue" element={<RequestQueuePage />} />
          <Route path="/staff/requests/:id" element={<StaffRequestDetailPage />} />
        </Route>
      </Route>

      {/* Management */}
      <Route element={<ProtectedRoute roles={['MANAGER']} />}>
        <Route element={<AppLayout />}>
          <Route path="/management/dashboard" element={<DashboardPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default App;
