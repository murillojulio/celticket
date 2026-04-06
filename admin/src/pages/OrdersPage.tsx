import { useQuery } from '@tanstack/react-query'
import type { CSSProperties } from 'react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAdminEvents } from '../api/eventsApi'
import { fetchOrders } from '../api/ordersApi'

export function OrdersPage() {
  const [eventId, setEventId] = useState<string>('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)

  const { data: events } = useQuery({
    queryKey: ['admin-events'],
    queryFn: fetchAdminEvents,
  })

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-orders', eventId, status, page],
    queryFn: () =>
      fetchOrders({
        eventId: eventId ? Number(eventId) : undefined,
        status: status || undefined,
        page,
        size: 20,
      }),
  })

  return (
    <div>
      <h1 style={{ marginTop: 0, color: '#e5e7eb' }}>Pedidos</h1>

      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          marginBottom: 20,
          alignItems: 'flex-end',
        }}
      >
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Evento</span>
          <select
            value={eventId}
            onChange={(e) => {
              setEventId(e.target.value)
              setPage(0)
            }}
            style={{
              minWidth: 200,
              padding: 8,
              borderRadius: 8,
              border: '1px solid #334155',
              background: '#020617',
              color: '#e5e7eb',
            }}
          >
            <option value="">Todos</option>
            {events?.map((ev) => (
              <option key={ev.id} value={ev.id}>
                {ev.name}
              </option>
            ))}
          </select>
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Estado</span>
          <input
            placeholder="ej. APPROVED"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
            style={{
              padding: 8,
              borderRadius: 8,
              border: '1px solid #334155',
              background: '#020617',
              color: '#e5e7eb',
            }}
          />
        </label>
      </div>

      {isLoading && <p style={{ color: '#94a3b8' }}>Cargando…</p>}
      {error && <p style={{ color: '#f87171' }}>{(error as Error).message}</p>}

      {data && data.content.length === 0 && (
        <p style={{ color: '#94a3b8' }}>No hay órdenes con estos filtros.</p>
      )}

      {data && data.content.length > 0 && (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
              <thead>
                <tr style={{ textAlign: 'left', color: '#94a3b8' }}>
                  <th style={th}>Referencia</th>
                  <th style={th}>Estado</th>
                  <th style={th}>Evento</th>
                  <th style={th}>Total</th>
                  <th style={th}>Email</th>
                  <th style={th}>Creada</th>
                  <th style={th} />
                </tr>
              </thead>
              <tbody>
                {data.content.map((o) => (
                  <tr key={o.reference} style={{ color: '#e5e7eb' }}>
                    <td style={td}>{o.reference}</td>
                    <td style={td}>{o.status}</td>
                    <td style={td}>{o.eventId}</td>
                    <td style={td}>
                      {o.amount.toLocaleString('es-CO', {
                        style: 'currency',
                        currency: o.currency || 'COP',
                        minimumFractionDigits: 0,
                      })}
                    </td>
                    <td style={td}>{o.buyerEmail ?? '—'}</td>
                    <td style={td}>{new Date(o.createdAt).toLocaleString()}</td>
                    <td style={td}>
                      <Link
                        to={`/orders/${encodeURIComponent(o.reference)}`}
                        style={{ color: '#22c55e' }}
                      >
                        Ver
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ marginTop: 16, display: 'flex', gap: 8, alignItems: 'center' }}>
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              style={pageBtn}
            >
              Anterior
            </button>
            <span style={{ color: '#94a3b8', fontSize: 14 }}>
              Página {data.page + 1} de {Math.max(1, data.totalPages)} ({data.totalElements}{' '}
              órdenes)
            </span>
            <button
              type="button"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              style={pageBtn}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
    </div>
  )
}

const th: CSSProperties = { padding: 8, borderBottom: '1px solid #334155' }
const td: CSSProperties = { padding: 8, borderBottom: '1px solid #1f2937' }
const pageBtn: CSSProperties = {
  padding: '6px 12px',
  borderRadius: 8,
  border: '1px solid #475569',
  background: '#1f2937',
  color: '#e5e7eb',
  cursor: 'pointer',
}
