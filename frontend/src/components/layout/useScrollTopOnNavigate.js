import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/** 화면이 바뀌면 새 화면은 맨 위에서 시작 */
export default function useScrollTopOnNavigate() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
}
