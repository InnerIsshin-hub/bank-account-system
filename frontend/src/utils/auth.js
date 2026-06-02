const TOKEN_KEY = 'bankAccessToken'
const USER_KEY = 'bankUser'

export function saveSession(data) {
  localStorage.setItem(TOKEN_KEY, data.accessToken)
  localStorage.setItem(USER_KEY, JSON.stringify(data.user || {}))
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUser() {
  return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem('userInfo')
}

export function isTokenExpired(token = getToken()) {
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return Number(payload.exp || 0) * 1000 <= Date.now()
  } catch {
    return true
  }
}
