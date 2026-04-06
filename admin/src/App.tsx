import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { useAuth } from './context/AuthContext'
import { EventsPage } from './pages/EventsPage'
import { LayoutEditorPage } from './pages/LayoutEditorPage'
import { LoginPage } from './pages/LoginPage'
import { OrderDetailPage } from './pages/OrderDetailPage'
import { OrdersPage } from './pages/OrdersPage'

function RequireAuth() {
  const { authed } = useAuth()
  if (!authed) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

function LoginRoute() {
  const { authed } = useAuth()
  if (authed) {
    return <Navigate to="/events" replace />
  }
  return <LoginPage />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/events" replace />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/events/:id/layout" element={<LayoutEditorPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/orders/:reference" element={<OrderDetailPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/events" replace />} />
    </Routes>
  )
}
