import { useMutation, useQuery } from '@tanstack/react-query'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchPublicEvent, publishLayout } from '../api/eventsApi'
import type { LayoutPayload } from '../api/types'

const DEFAULT_LAYOUT = JSON.stringify(
  {
    meta: { seatSize: 12, seatGap: 4 },
    sections: [
      {
        name: 'Principal',
        rows: 6,
        cols: 8,
        basePrice: 20000,
        x: 40,
        y: 40,
      },
    ],
    tarimas: [],
  },
  null,
  2,
)

function layoutBuilderUrl(): string {
  const env = import.meta.env.VITE_LAYOUT_BUILDER_URL as string | undefined
  if (env) return env
  return `${window.location.origin}/layout-builder`
}

export function LayoutEditorPage() {
  const { id } = useParams<{ id: string }>()
  const eventId = Number(id)
  const iframeRef = useRef<HTMLIFrameElement>(null)
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const { data: event, isLoading, error } = useQuery({
    queryKey: ['event', eventId],
    queryFn: () => fetchPublicEvent(eventId),
    enabled: Number.isFinite(eventId),
  })

  const publishMut = useMutation({
    mutationFn: (layout: LayoutPayload) => publishLayout(eventId, layout),
    onSuccess: () => setMessage('Layout publicado correctamente.'),
    onError: (e: Error) => setMessage(e.message),
  })

  const pushLayoutToIframe = useCallback(() => {
    const win = iframeRef.current?.contentWindow
    if (!win || !event) return
    const payload = event.layoutConfig?.trim() ? event.layoutConfig : DEFAULT_LAYOUT
    win.postMessage({ type: 'celticket-load-layout', payload }, '*')
  }, [event])

  useEffect(() => {
    if (!loaded || !event) return
    pushLayoutToIframe()
  }, [loaded, event, pushLayoutToIframe])

  function requestLayoutAndPublish() {
    setMessage(null)
    const handler = (e: MessageEvent) => {
      const data = e.data
      if (!data || data.type !== 'celticket-layout-snapshot') return
      window.removeEventListener('message', handler)
      try {
        const layout = JSON.parse(data.payload as string) as LayoutPayload
        publishMut.mutate(layout)
      } catch {
        setMessage('No se pudo interpretar el JSON del layout.')
      }
    }
    window.addEventListener('message', handler)
    const win = iframeRef.current?.contentWindow
    if (!win) {
      setMessage('El editor no está listo.')
      return
    }
    win.postMessage({ type: 'celticket-request-layout' }, '*')
  }

  if (!Number.isFinite(eventId)) {
    return <p style={{ color: '#f87171' }}>ID inválido</p>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <Link to="/events" style={{ color: '#22c55e' }}>
          ← Eventos
        </Link>
        <h1 style={{ margin: 0, color: '#e5e7eb' }}>
          Layout · {event?.name ?? `Evento ${eventId}`}
        </h1>
        <button
          type="button"
          onClick={requestLayoutAndPublish}
          disabled={publishMut.isPending}
          style={{
            marginLeft: 'auto',
            padding: '10px 18px',
            borderRadius: 8,
            border: 'none',
            background: '#22c55e',
            color: '#052e16',
            fontWeight: 700,
            cursor: publishMut.isPending ? 'wait' : 'pointer',
          }}
        >
          {publishMut.isPending ? 'Publicando…' : 'Publicar en el servidor'}
        </button>
      </div>

      {isLoading && <p style={{ color: '#94a3b8' }}>Cargando evento…</p>}
      {error && <p style={{ color: '#f87171' }}>{(error as Error).message}</p>}
      {message && (
        <p style={{ color: message.startsWith('Layout') ? '#22c55e' : '#f87171' }}>{message}</p>
      )}

      <p style={{ margin: 0, color: '#94a3b8', fontSize: 13 }}>
        Edita el mapa en el panel. Al publicar se regeneran los asientos en el backend según el
        layout.
      </p>

      <iframe
        ref={iframeRef}
        title="Layout Studio"
        src={layoutBuilderUrl()}
        onLoad={() => setLoaded(true)}
        style={{
          width: '100%',
          minHeight: '78vh',
          border: '1px solid #334155',
          borderRadius: 12,
          background: '#0f172a',
        }}
      />
    </div>
  )
}
