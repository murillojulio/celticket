import { apiFetch, parseJson } from './client'
import type { CelticketEvent, LayoutPayload } from './types'

export async function fetchAdminEvents(): Promise<CelticketEvent[]> {
  const res = await apiFetch('/api/admin/events')
  if (!res.ok) throw new Error('No se pudieron cargar los eventos')
  return parseJson<CelticketEvent[]>(res)
}

export async function fetchPublicEvent(id: number): Promise<CelticketEvent> {
  const res = await apiFetch(`/api/events/${id}`)
  if (!res.ok) throw new Error('Evento no encontrado')
  return parseJson<CelticketEvent>(res)
}

export type CreateEventBody = {
  name: string
  eventDate: string
  venue: string
  rowCount: number
  columnCount: number
}

export async function createEvent(body: CreateEventBody): Promise<CelticketEvent> {
  const res = await apiFetch('/api/admin/events', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error((err as { mensaje?: string }).mensaje || 'Error al crear')
  }
  return parseJson<CelticketEvent>(res)
}

export type UpdateEventBody = Partial<{
  name: string
  eventDate: string
  venue: string
  rowCount: number
  columnCount: number
}>

export async function updateEvent(id: number, body: UpdateEventBody): Promise<CelticketEvent> {
  const res = await apiFetch(`/api/admin/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error((err as { mensaje?: string }).mensaje || 'Error al actualizar')
  }
  return parseJson<CelticketEvent>(res)
}

export async function deleteEvent(id: number): Promise<void> {
  const res = await apiFetch(`/api/admin/events/${id}`, { method: 'DELETE' })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error((err as { mensaje?: string }).mensaje || 'Error al eliminar')
  }
}

export async function publishLayout(id: number, layout: LayoutPayload): Promise<void> {
  const res = await apiFetch(`/api/admin/events/${id}/layout`, {
    method: 'PUT',
    body: JSON.stringify(layout),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || 'Error al publicar layout')
  }
}
