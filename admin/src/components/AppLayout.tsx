import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const linkStyle = ({ isActive }: { isActive: boolean }) => ({
  color: isActive ? '#22c55e' : '#94a3b8',
  textDecoration: 'none',
  fontWeight: 600,
})

export function AppLayout() {
  const { logout } = useAuth()
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '1.5rem',
          padding: '12px 20px',
          borderBottom: '1px solid #334155',
          background: '#111827',
        }}
      >
        <strong style={{ color: '#e5e7eb' }}>Celticket Admin</strong>
        <nav style={{ display: 'flex', gap: '1rem' }}>
          <NavLink to="/events" style={linkStyle} end={false}>
            Eventos
          </NavLink>
          <NavLink to="/orders" style={linkStyle}>
            Pedidos
          </NavLink>
        </nav>
        <button
          type="button"
          onClick={() => logout()}
          style={{
            marginLeft: 'auto',
            background: '#1f2937',
            border: '1px solid #475569',
            color: '#e5e7eb',
            borderRadius: 8,
            padding: '6px 12px',
            cursor: 'pointer',
          }}
        >
          Salir
        </button>
      </header>
      <main style={{ flex: 1, padding: '20px' }}>
        <Outlet />
      </main>
    </div>
  )
}
