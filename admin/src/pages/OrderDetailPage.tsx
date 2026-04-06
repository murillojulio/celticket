import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { fetchOrderByReference } from '../api/ordersApi'

export function OrderDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const ref = reference ? decodeURIComponent(reference) : ''

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-order', ref],
    queryFn: () => fetchOrderByReference(ref),
    enabled: !!ref,
  })

  return (
    <div>
      <Link to="/orders" style={{ color: '#22c55e' }}>
        ← Pedidos
      </Link>
      <h1 style={{ color: '#e5e7eb' }}>Orden {ref}</h1>

      {isLoading && <p style={{ color: '#94a3b8' }}>Cargando…</p>}
      {error && <p style={{ color: '#f87171' }}>{(error as Error).message}</p>}

      {data && (
        <dl
          style={{
            display: 'grid',
            gridTemplateColumns: '160px 1fr',
            gap: '8px 16px',
            color: '#e5e7eb',
            fontSize: 14,
          }}
        >
          <dt style={{ color: '#94a3b8' }}>Estado</dt>
          <dd style={{ margin: 0 }}>{data.status}</dd>
          <dt style={{ color: '#94a3b8' }}>Evento ID</dt>
          <dd style={{ margin: 0 }}>{data.eventId}</dd>
          <dt style={{ color: '#94a3b8' }}>Total</dt>
          <dd style={{ margin: 0 }}>
            {data.amount.toLocaleString('es-CO', {
              style: 'currency',
              currency: data.currency || 'COP',
              minimumFractionDigits: 0,
            })}
          </dd>
          <dt style={{ color: '#94a3b8' }}>Subtotal / servicio</dt>
          <dd style={{ margin: 0 }}>
            {data.subtotal.toLocaleString('es-CO')} / {data.serviceFee.toLocaleString('es-CO')}
          </dd>
          <dt style={{ color: '#94a3b8' }}>Email</dt>
          <dd style={{ margin: 0 }}>{data.buyerEmail ?? '—'}</dd>
          <dt style={{ color: '#94a3b8' }}>Asientos</dt>
          <dd style={{ margin: 0 }}>{data.seatIds.join(', ') || '—'}</dd>
          <dt style={{ color: '#94a3b8' }}>Wompi tx</dt>
          <dd style={{ margin: 0 }}>{data.providerTransactionId ?? '—'}</dd>
          <dt style={{ color: '#94a3b8' }}>Creada</dt>
          <dd style={{ margin: 0 }}>{new Date(data.createdAt).toLocaleString()}</dd>
          <dt style={{ color: '#94a3b8' }}>Actualizada</dt>
          <dd style={{ margin: 0 }}>{new Date(data.updatedAt).toLocaleString()}</dd>
        </dl>
      )}
    </div>
  )
}
