import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { CSSProperties } from 'react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  createEvent,
  deleteEvent,
  fetchAdminEvents,
  updateEvent,
  type CreateEventBody,
  type UpdateEventBody,
} from '../api/eventsApi'
import type { CelticketEvent } from '../api/types'

function toLocalInput(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** Mantiene hora local en formato ISO sin desfase UTC (compatible con LocalDateTime). */
function fromLocalInput(v: string): string {
  if (!v.includes('T')) return v
  const [date, time] = v.split('T')
  const t = time.length === 5 ? `${time}:00` : time
  return `${date}T${t}`
}

export function EventsPage() {
  const qc = useQueryClient()
  const { data: events, isLoading, error } = useQuery({
    queryKey: ['admin-events'],
    queryFn: fetchAdminEvents,
  })

  const [modal, setModal] = useState<'create' | 'edit' | null>(null)
  const [editing, setEditing] = useState<CelticketEvent | null>(null)

  const createMut = useMutation({
    mutationFn: createEvent,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-events'] })
      setModal(null)
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Parameters<typeof updateEvent>[1] }) =>
      updateEvent(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-events'] })
      setModal(null)
      setEditing(null)
    },
  })

  const deleteMut = useMutation({
    mutationFn: deleteEvent,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-events'] }),
  })

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20 }}>
        <h1 style={{ margin: 0, color: '#e5e7eb' }}>Eventos</h1>
        <button
          type="button"
          onClick={() => {
            setEditing(null)
            setModal('create')
          }}
          style={{
            padding: '8px 14px',
            borderRadius: 8,
            border: 'none',
            background: '#22c55e',
            color: '#052e16',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Nuevo evento
        </button>
      </div>

      {isLoading && <p style={{ color: '#94a3b8' }}>Cargando…</p>}
      {error && <p style={{ color: '#f87171' }}>{(error as Error).message}</p>}

      {events && events.length === 0 && (
        <p style={{ color: '#94a3b8' }}>No hay eventos. Crea uno para empezar.</p>
      )}

      {events && events.length > 0 && (
        <div style={{ overflowX: 'auto' }}>
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: 14,
            }}
          >
            <thead>
              <tr style={{ textAlign: 'left', color: '#94a3b8' }}>
                <th style={{ padding: 8, borderBottom: '1px solid #334155' }}>Nombre</th>
                <th style={{ padding: 8, borderBottom: '1px solid #334155' }}>Fecha</th>
                <th style={{ padding: 8, borderBottom: '1px solid #334155' }}>Lugar</th>
                <th style={{ padding: 8, borderBottom: '1px solid #334155' }}>Rejilla</th>
                <th style={{ padding: 8, borderBottom: '1px solid #334155' }} />
              </tr>
            </thead>
            <tbody>
              {events.map((ev) => (
                <tr key={ev.id} style={{ color: '#e5e7eb' }}>
                  <td style={{ padding: 8, borderBottom: '1px solid #1f2937' }}>{ev.name}</td>
                  <td style={{ padding: 8, borderBottom: '1px solid #1f2937' }}>
                    {new Date(ev.eventDate).toLocaleString()}
                  </td>
                  <td style={{ padding: 8, borderBottom: '1px solid #1f2937' }}>{ev.venue}</td>
                  <td style={{ padding: 8, borderBottom: '1px solid #1f2937' }}>
                    {ev.rowCount} × {ev.columnCount}
                  </td>
                  <td style={{ padding: 8, borderBottom: '1px solid #1f2937' }}>
                    <Link
                      to={`/events/${ev.id}/layout`}
                      style={{ color: '#22c55e', marginRight: 12 }}
                    >
                      Layout
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        setEditing(ev)
                        setModal('edit')
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#38bdf8',
                        cursor: 'pointer',
                        marginRight: 12,
                      }}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (
                          confirm(
                            `¿Eliminar el evento "${ev.name}"? No debe tener pedidos asociados.`,
                          )
                        ) {
                          deleteMut.mutate(ev.id)
                        }
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#f87171',
                        cursor: 'pointer',
                      }}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal === 'create' && (
        <EventFormModal
          title="Nuevo evento"
          onClose={() => setModal(null)}
          onCreate={(body) => createMut.mutate(body)}
          error={createMut.error instanceof Error ? createMut.error.message : null}
          loading={createMut.isPending}
        />
      )}

      {modal === 'edit' && editing && (
        <EventFormModal
          title="Editar evento"
          initial={editing}
          onClose={() => {
            setModal(null)
            setEditing(null)
          }}
          onUpdate={(body) => updateMut.mutate({ id: editing.id, body })}
          error={updateMut.error instanceof Error ? updateMut.error.message : null}
          loading={updateMut.isPending}
        />
      )}
    </div>
  )
}

function EventFormModal({
  title,
  initial,
  onClose,
  onCreate,
  onUpdate,
  error,
  loading,
}: {
  title: string
  initial?: CelticketEvent
  onClose: () => void
  onCreate?: (body: CreateEventBody) => void
  onUpdate?: (body: UpdateEventBody) => void
  error: string | null
  loading: boolean
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [venue, setVenue] = useState(initial?.venue ?? '')
  const [eventDate, setEventDate] = useState(
    initial ? toLocalInput(initial.eventDate) : '',
  )
  const [rowCount, setRowCount] = useState(String(initial?.rowCount ?? 8))
  const [columnCount, setColumnCount] = useState(String(initial?.columnCount ?? 10))

  function submit(e: React.FormEvent) {
    e.preventDefault()
    const r = parseInt(rowCount, 10)
    const c = parseInt(columnCount, 10)
    const body = {
      name,
      venue,
      eventDate: fromLocalInput(eventDate),
      rowCount: r,
      columnCount: c,
    }
    if (initial) {
      onUpdate?.(body)
    } else {
      onCreate?.(body)
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(15,23,42,0.85)',
        display: 'grid',
        placeItems: 'center',
        zIndex: 50,
        padding: 16,
      }}
    >
      <form
        onSubmit={submit}
        style={{
          width: '100%',
          maxWidth: 420,
          background: '#111827',
          border: '1px solid #334155',
          borderRadius: 12,
          padding: 20,
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <h2 style={{ margin: 0, color: '#e5e7eb' }}>{title}</h2>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Nombre</span>
          <input
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={inputStyle}
          />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Fecha y hora</span>
          <input
            required
            type="datetime-local"
            value={eventDate}
            onChange={(e) => setEventDate(e.target.value)}
            style={inputStyle}
          />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Lugar</span>
          <input
            required
            value={venue}
            onChange={(e) => setVenue(e.target.value)}
            style={inputStyle}
          />
        </label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={{ color: '#94a3b8', fontSize: 12 }}>Filas</span>
            <input
              required
              type="number"
              min={1}
              value={rowCount}
              onChange={(e) => setRowCount(e.target.value)}
              style={inputStyle}
            />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={{ color: '#94a3b8', fontSize: 12 }}>Columnas</span>
            <input
              required
              type="number"
              min={1}
              value={columnCount}
              onChange={(e) => setColumnCount(e.target.value)}
              style={inputStyle}
            />
          </label>
        </div>
        {error && <p style={{ color: '#f87171', margin: 0 }}>{error}</p>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button type="button" onClick={onClose} style={btnGhost}>
            Cancelar
          </button>
          <button type="submit" disabled={loading} style={btnPrimary}>
            {loading ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  )
}

const inputStyle: CSSProperties = {
  padding: 10,
  borderRadius: 8,
  border: '1px solid #334155',
  background: '#020617',
  color: '#e5e7eb',
}

const btnPrimary: CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: 'none',
  background: '#22c55e',
  color: '#052e16',
  fontWeight: 600,
  cursor: 'pointer',
}

const btnGhost: CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: '1px solid #475569',
  background: 'transparent',
  color: '#e5e7eb',
  cursor: 'pointer',
}
