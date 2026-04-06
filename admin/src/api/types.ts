export type CelticketEvent = {
  id: number
  name: string
  eventDate: string
  venue: string
  rowCount: number
  columnCount: number
  layoutConfig: string | null
}

export type AdminOrder = {
  reference: string
  status: string
  eventId: number
  amount: number
  subtotal: number
  serviceFee: number
  currency: string
  buyerEmail: string | null
  sessionId: string
  seatIds: string[]
  providerTransactionId: string | null
  createdAt: string
  updatedAt: string
}

export type PagedOrders = {
  content: AdminOrder[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type LayoutPayload = Record<string, unknown>
