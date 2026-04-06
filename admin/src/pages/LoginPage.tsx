import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { adminLogin } from '../api/authApi'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const { token } = await adminLogin(password)
      login(token)
      navigate('/events', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        padding: 24,
      }}
    >
      <form
        onSubmit={onSubmit}
        style={{
          width: '100%',
          maxWidth: 360,
          background: '#111827',
          border: '1px solid #334155',
          borderRadius: 12,
          padding: 24,
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
        }}
      >
        <h1 style={{ margin: 0, color: '#22c55e', fontSize: 22 }}>Admin Celticket</h1>
        <p style={{ margin: 0, color: '#94a3b8', fontSize: 14 }}>
          Introduce la contraseña de administrador configurada en el backend (ADMIN_PASSWORD).
        </p>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Contraseña</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{
              padding: 10,
              borderRadius: 8,
              border: '1px solid #334155',
              background: '#020617',
              color: '#e5e7eb',
            }}
          />
        </label>
        {error && (
          <p style={{ margin: 0, color: '#f87171', fontSize: 14 }}>{error}</p>
        )}
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '10px 16px',
            borderRadius: 8,
            border: 'none',
            background: '#22c55e',
            color: '#052e16',
            fontWeight: 700,
            cursor: loading ? 'wait' : 'pointer',
          }}
        >
          {loading ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
