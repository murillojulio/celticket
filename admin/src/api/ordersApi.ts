import { apiFetch, parseJson } from './client'
import type { AdminOrder, PagedOrders } from './types'

export async function fetchOrders(params: {
  eventId?: number
  status?: string
  page?: number
  size?: number
}): Promise<PagedOrders> {
  const sp = new URLSearchParams()
  if (params.eventId != null) sp.set('eventId', String(params.eventId))
  if (params.status) sp.set('status', params.status)
  sp.set('page', String(params.page ?? 0))
  sp.set('size', String(params.size ?? 20))
  const res = await apiFetch(`/api/admin/orders?${sp.toString()}`)
  if (!res.ok) throw new Error('No se pudieron cargar las órdenes')
  return parseJson<PagedOrders>(res)
}

export async function fetchOrderByReference(reference: string): Promise<AdminOrder> {
  const res = await apiFetch(`/api/admin/orders/${encodeURIComponent(reference)}`)
  if (!res.ok) throw new Error('Orden no encontrada')
  return parseJson<AdminOrder>(res)
}
