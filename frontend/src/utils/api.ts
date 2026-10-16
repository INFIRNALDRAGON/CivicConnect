import { Session } from '../types';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const SESSION_KEY = 'cc_session';

export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
export const SHOW_DEMO_ACCOUNTS = USE_MOCK || import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'true';

export type Params = Record<string, string | undefined>;
export type Body = Record<string, unknown>;

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export const getSession = (): Session | null => {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null');
  } catch {
    return null;
  }
};

export const saveSession = (session: Session): void => {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
};

export const clearSession = (): void => {
  sessionStorage.removeItem(SESSION_KEY);
};

const queryString = (params: Params = {}): string => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) query.append(key, value);
  });
  const text = query.toString();
  return text ? `?${text}` : '';
};

const sendToServer = async (method: string, path: string, body?: Body, params?: Params, token?: string): Promise<unknown> => {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}${queryString(params)}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(20000)
    });
  } catch {
    throw new ApiError(0, 'We could not reach the server. Check your connection and try again.');
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.message || 'Something went wrong. Please try again.');
  }
  return data;
};

const request = async <T>(method: string, path: string, body?: Body, params?: Params): Promise<T> => {
  const token = getSession()?.token;
  try {
    if (USE_MOCK) {
      const { handleMockRequest } = await import('./mockApi');
      return (await handleMockRequest(method, path, body, params, token)) as T;
    }
    return (await sendToServer(method, path, body, params, token)) as T;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && !path.startsWith('/auth/')) {
      clearSession();
      window.location.assign('/login');
    }
    throw error;
  }
};

export const api = {
  get: <T>(path: string, params?: Params): Promise<T> => request<T>('GET', path, undefined, params),
  post: <T>(path: string, body?: Body): Promise<T> => request<T>('POST', path, body)
};
