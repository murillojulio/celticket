const TOKEN_KEY = 'celticket_admin_token'

export function getApiBase(): string {
  const raw = import.meta.env.VITE_API_BASE_URL as string | undefined
  return raw ? raw.replace(/\/$/, '') : ''
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}

export function loginPath(): string {
  const base = import.meta.env.BASE_URL || '/'
  return `${base}login`.replace(/\/{2,}/g, '/')
}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = getToken()
  const headers = new Headers(options.headers)
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  // Sin esto, fetch puede mandar text/plain y Spring devuelve 415 en @RequestBody JSON
  if (
    options.body != null &&
    typeof options.body === 'string' &&
    !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json')
  }
  const url = `${getApiBase()}${path}`
  const res = await fetch(url, { ...options, headers })
  if (res.status === 401) {
    clearToken()
    if (!path.includes('/auth/admin-login')) {
      window.location.assign(loginPath())
    }
  }
  return res
}

export async function parseJson<T>(res: Response): Promise<T> {
  return res.json() as Promise<T>
}
