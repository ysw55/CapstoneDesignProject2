import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const DURATION = 3000;

/**
 * 화면 하단 토스트 상태.
 * - showToast('삭제했어요') 로 직접 띄우거나
 * - navigate(path, { state: { toast: '...' } }) 로 다른 화면에서 넘겨줄 수 있다.
 * onHide 는 토스트가 사라질 때 호출된다 (강조 표시 해제 등).
 */
export default function useFlashToast(onHide) {
  const location = useLocation();
  const navigate = useNavigate();
  const [toast, setToast] = useState(null);

  // 다른 화면에서 넘어온 토스트 메시지 꺼내기
  useEffect(() => {
    const message = location.state?.toast;
    if (!message) return;
    setToast({ message, key: Date.now() });
    navigate(location.pathname, { replace: true, state: null }); // 새로고침 시 재표시 방지
  }, [location.state, location.pathname, navigate]);

  // 일정 시간 뒤 자동으로 사라짐 (key 가 바뀌면 타이머 재시작)
  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => {
      setToast(null);
      onHide?.();
    }, DURATION);
    return () => clearTimeout(timer);
  }, [toast, onHide]);

  const showToast = useCallback((message) => setToast({ message, key: Date.now() }), []);

  return [toast, showToast];
}
