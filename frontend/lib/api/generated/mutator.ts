const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

type FetchBody = BodyInit | Record<string, unknown> | undefined;

function normalizeBody(body: FetchBody): BodyInit | undefined {
  if (body == null) {
    return undefined;
  }

  if (typeof body === 'string' || body instanceof FormData || body instanceof URLSearchParams || body instanceof Blob) {
    return body;
  }

  return JSON.stringify(body);
}

async function refreshAccessToken(): Promise<string | null> {
  if (typeof window === 'undefined') {
    return null;
  }

  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    return null;
  }

  const response = await fetch(`${API_URL}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    return null;
  }

  const data = (await response.json()) as { accessToken: string; refreshToken: string };
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data.accessToken;
}

export async function customFetch<T>(url: string, options: RequestInit = {}): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  if (token) {
    (headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }

  const requestInit: RequestInit = {
    ...options,
    headers,
    body: normalizeBody(options.body as FetchBody),
  };

  const requestUrl = url.startsWith('http') ? url : `${API_URL}${url}`;
  let response = await fetch(requestUrl, requestInit);

  if (response.status === 401 && token) {
    const renewedToken = await refreshAccessToken();
    if (renewedToken) {
      (headers as Record<string, string>).Authorization = `Bearer ${renewedToken}`;
      response = await fetch(requestUrl, {
        ...requestInit,
        headers,
      });
    }
  }

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return {
      data: undefined,
      status: response.status,
      headers: response.headers,
    } as T;
  }

  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  return {
    data,
    status: response.status,
    headers: response.headers,
  } as T;
}
