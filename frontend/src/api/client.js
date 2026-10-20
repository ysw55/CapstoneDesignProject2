/**
 * 백엔드 API 공통 클라이언트
 * - JWT 를 localStorage 에 보관하고, 모든 요청에 Authorization: Bearer <token> 을 붙인다.
 * - 서버 에러 응답({ status, code, message, fieldErrors })을 ApiError 로 변환한다.
 * - 401 을 받으면 저장된 토큰을 지우고 'bookcanvas:unauthorized' 이벤트를 보내 로그아웃시킨다.
 */

// 개발: Vite 프록시가 /api → localhost:8080 으로 전달하므로 빈 값.
// 배포/웹뷰: .env 에 VITE_API_BASE_URL=https://api.bookcanvas.com 처럼 지정
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

const TOKEN_KEY = 'bookcanvas:accessToken';
export const UNAUTHORIZED_EVENT = 'bookcanvas:unauthorized';

/* ───────── 토큰 저장소 ───────── */

export const tokenStorage = {
  get() {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set(token) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* 저장 실패해도 이번 실행 동안은 메모리의 상태로 동작 */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* noop */
    }
  },
};

/* ───────── 에러 ───────── */

export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors = [] }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  /** 특정 입력란의 에러 메시지 (없으면 undefined) */
  fieldMessage(field) {
    return this.fieldErrors.find((e) => e.field === field)?.message;
  }
}

/* ───────── 요청 ───────── */

/**
 * @param {string} path    '/api/auth/login' 처럼 /api 로 시작하는 경로
 * @param {object} options { method, body, auth } — auth=false 면 토큰을 붙이지 않음
 */
export async function apiFetch(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = auth ? tokenStorage.get() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError({
      status: 0,
      code: 'NETWORK_ERROR',
      message: '서버에 연결할 수 없어요. 네트워크 상태를 확인해 주세요.',
    });
  }

  const data = await parseJson(response);

  if (!response.ok) {
    // 토큰을 보냈는데 401 → 만료/위조. 저장된 토큰을 지우고 앱 전체에 알림
    if (response.status === 401 && token) {
      tokenStorage.clear();
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT, { detail: data }));
    }
    throw new ApiError({
      status: response.status,
      code: data?.code ?? `HTTP_${response.status}`,
      message: data?.message ?? defaultMessage(response.status),
      fieldErrors: data?.fieldErrors,
    });
  }

  return data;
}

async function parseJson(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null; // 프록시 오류 페이지(HTML) 등
  }
}

function defaultMessage(status) {
  if (status === 502 || status === 503 || status === 504) return '서버가 응답하지 않아요. 잠시 후 다시 시도해 주세요.';
  if (status >= 500) return '일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.';
  return '요청을 처리하지 못했어요.';
}
