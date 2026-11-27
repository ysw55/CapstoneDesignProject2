import React, { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { X } from 'lucide-react';
import Button from './Button';

const VIEW = 240; // 원형 영역 지름(px)
const STAGE = 320; // 사진 전체가 보이는 작업 영역(px) — 원 밖은 흐리게
const OUT = 192; // 저장할 사진 크기(px)
const MAX_ZOOM = 4;

/**
 * 프로필 사진 자르기 — 원 안에 보일 부분을 드래그(이동)·슬라이더/휠/핀치(확대)로 정한다.
 * @param {{ file: File, onCancel: () => void, onDone: (dataUrl: string) => void }} props
 */
export default function PhotoCropper({ file, onCancel, onDone }) {
  const [img, setImg] = useState(null);
  const [zoom, setZoom] = useState(1);
  const [off, setOff] = useState({ x: 0, y: 0 }); // 이미지 중심의 원 중심 기준 이동량
  const [failed, setFailed] = useState(false);
  const pointers = useRef(new Map());
  const pinch = useRef(null);

  const [url, setUrl] = useState(null);
  useEffect(() => {
    // StrictMode 가 효과를 두 번 실행해도 안전하도록 주소 생성/해제를 한 효과 안에서 처리
    const objectUrl = URL.createObjectURL(file);
    let cancelled = false;
    const el = new Image();
    el.onload = () => {
      if (cancelled) return;
      setUrl(objectUrl);
      setImg(el);
    };
    el.onerror = () => !cancelled && setFailed(true);
    el.src = objectUrl;
    return () => {
      cancelled = true;
      URL.revokeObjectURL(objectUrl);
    };
  }, [file]);

  const base = img ? VIEW / Math.min(img.width, img.height) : 1; // 원을 꽉 채우는 최소 배율
  const scale = base * zoom;

  const clamp = (o, z = zoom) => {
    if (!img) return o;
    const s = base * z;
    const mx = Math.max(0, (img.width * s - VIEW) / 2);
    const my = Math.max(0, (img.height * s - VIEW) / 2);
    return { x: Math.min(mx, Math.max(-mx, o.x)), y: Math.min(my, Math.max(-my, o.y)) };
  };

  const changeZoom = (z) => {
    const next = Math.min(MAX_ZOOM, Math.max(1, z));
    setZoom(next);
    setOff((o) => clamp(o, next));
  };

  const onPointerDown = (e) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    pinch.current = null;
  };

  const onPointerMove = (e) => {
    const prev = pointers.current.get(e.pointerId);
    if (!prev) return;
    pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const pts = [...pointers.current.values()];
    if (pts.length >= 2) {
      const d = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      if (pinch.current) changeZoom(zoom * (d / pinch.current));
      pinch.current = d;
    } else {
      setOff((o) => clamp({ x: o.x + e.clientX - prev.x, y: o.y + e.clientY - prev.y }));
    }
  };

  const onPointerUp = (e) => {
    pointers.current.delete(e.pointerId);
    pinch.current = null;
  };

  const photoStyle = img && {
    width: img.width * scale,
    height: img.height * scale,
    transform: `translate(calc(-50% + ${off.x}px), calc(-50% + ${off.y}px))`,
  };

  const handleDone = () => {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = OUT;
    const left = VIEW / 2 + off.x - (img.width * scale) / 2;
    const top = VIEW / 2 + off.y - (img.height * scale) / 2;
    const side = VIEW / scale;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, OUT, OUT);
    ctx.drawImage(img, -left / scale, -top / scale, side, side, 0, 0, OUT, OUT);
    onDone(canvas.toDataURL('image/jpeg', 0.85));
  };

  return (
    <Overlay role="dialog" aria-modal="true" aria-label="프로필 사진 자르기">
      <Header>
        <button type="button" aria-label="닫기" onClick={onCancel}>
          <X size={24} />
        </button>
        <span>프로필에 보일 부분 고르기</span>
        <i aria-hidden style={{ width: 44 }} />
      </Header>

      {failed ? (
        <Message>이미지를 불러오지 못했어요.</Message>
      ) : (
        <>
          <Stage
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onWheel={(e) => changeZoom(zoom * (e.deltaY < 0 ? 1.08 : 0.92))}
          >
            {img && (
              <>
                <Photo src={url} alt="" draggable={false} style={photoStyle} $dim />
                <Bright>
                  <Photo src={url} alt="" draggable={false} style={photoStyle} />
                </Bright>
              </>
            )}
            <Ring aria-hidden />
          </Stage>
          <Hint>드래그해서 위치를, 아래 슬라이더로 크기를 조절해요</Hint>
          <Zoom
            type="range"
            min="1"
            max={MAX_ZOOM}
            step="0.01"
            value={zoom}
            aria-label="확대"
            onChange={(e) => changeZoom(Number(e.target.value))}
          />
        </>
      )}

      <Footer>
        <Button type="button" $variant="secondary" onClick={onCancel}>
          취소
        </Button>
        <Button type="button" $variant="primary" disabled={!img} onClick={handleDone}>
          적용
        </Button>
      </Footer>
    </Overlay>
  );
}

const Overlay = styled.div`
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: env(safe-area-inset-top, 0px) 16px 24px;
  color: #fff;
  background: #111;
`;

const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 56px;
  font-weight: 700;

  button {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    color: #fff;
  }
`;

const Stage = styled.div`
  position: relative;
  width: ${STAGE}px;
  height: ${STAGE}px;
  max-width: 100%;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 16px;
  background: #000;
  touch-action: none;
  cursor: grab;
  user-select: none;

  &:active {
    cursor: grabbing;
  }
`;

const Photo = styled.img`
  position: absolute;
  top: 50%;
  left: 50%;
  max-width: none;
  pointer-events: none;
  ${({ $dim }) => $dim && 'opacity: 0.35; filter: blur(1.5px);'}
`;

/* 원 안쪽만 선명하게 보이는 레이어 */
const Bright = styled.div`
  position: absolute;
  inset: 0;
  clip-path: circle(${VIEW / 2}px at 50% 50%);
  pointer-events: none;
`;

const Ring = styled.span`
  position: absolute;
  top: 50%;
  left: 50%;
  width: ${VIEW}px;
  height: ${VIEW}px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.85);
  pointer-events: none;
`;

const Hint = styled.p`
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
`;

const Zoom = styled.input`
  width: ${VIEW}px;
`;

const Message = styled.p`
  margin: 80px 0;
`;

const Footer = styled.div`
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 360px;
  margin-top: auto;
`;
