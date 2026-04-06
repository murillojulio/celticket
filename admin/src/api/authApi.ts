import { getApiBase } from './client'

export type AdminLoginResponse = {
  token: string
  expiresAt: number
}

export async function adminLogin(password: string): Promise<AdminLoginResponse> {
  const res = await fetch(`${getApiBase()}/api/auth/admin-login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    body: JSON.stringify({ password }),
  })
  const data = (await res.json()) as AdminLoginResponse & { mensaje?: string }
  if (!res.ok) {
    throw new Error(data.mensaje || 'Error de login')
  }
  return data
}
