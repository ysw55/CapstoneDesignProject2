import React, { useState } from 'react';
import styled from 'styled-components';
import { BadgeCheck } from 'lucide-react';
import { isLightColor } from '../../utils/format';

// 책마다 두께·키가 조금씩 달라 보이도록 id 로 고정된 값을 뽑는다 (새로고침해도 같은 모양)
const WIDTHS = [36, 40, 44, 38, 42, 46, 37];
const HEIGHTS = [168, 180, 160, 188, 172, 164, 184];

// 3단 꾸밈: 위·아래 색 블록 높이
const TOP_BLOCK = 26;
const BOTTOM_BLOCK = 22;
// 가운데 색과 이 거리(RGB)보다 가까운 표지 색은 위·아래 블록에 쓰지 않는다 (구분이 안 되므로)
const MIN_BLOCK_DISTANCE = 40;

const toRgb = (hex) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));

function colorDistance(a, b) {
  const [ar, ag, ab] = toRgb(a);
  const [br, bg, bb] = toRgb(b);
  return Math.hypot(ar - br, ag - bg, ab - bb);
}

// 책등 이미지는 세로로 길어야 책등이다 (높이/너비가 이 값보다 작으면 책등이 아닌 이미지로 보고 쓰지 않는다)
// 서버(BookSearchService.MIN_SPINE_ASPECT)와 같은 기준 — 예스24 진짜 책등은 400px 높이에 폭 11~64px, 공용 대체 이미지는 280x400
const MIN_SPINE_ASPECT = 2;
// 예스24 책등 이미지는 책등 전체를 찍은 사진(높이 1200px)이라 제목 글씨가 그중 작은 부분이다.
// 색 책등 높이(160~188px)로 줄이면 글자가 4px 정도가 되어 읽을 수 없고, 1.5배(약 240~270px) 안팎이면 큰 제목은 읽힌다.
const HEIGHT_SCALE = 1.45; // 이미지·색 책등 모두 같은 배율이라 책장 높이가 고르다

// 책장이 가지런해 보이도록 책등 이미지의 폭을 색 책등과 같은 30~40px 안으로 모은다.
// 실제 예스24 책등은 400px 높이에 폭 11~64px 로 천차만별이라, 그 두께 순서만 살려 이 범위로 눌러 담고
// 이미지는 옆으로 조금 늘어나도 그대로 채운다 (두꺼운 책일수록 폭이 넓다)
const IMAGE_WIDTH_RANGE = [34, 46];
const REAL_SPINE_WIDTH_RANGE = [11, 64];

/**
 * 예스24 이미지 주소는 끝의 크기 글자만 바꾸면 더 큰 이미지가 온다 (.../SIDE/L → .../SIDE/XL).
 * L 은 높이 400px·폭 11~64px 라 책장에서 옆으로 늘리면 뭉개지지만, XL 은 3배(높이 1200px)라 줄여서 쓰게 된다.
 * XL 을 불러오지 못하면 원래 주소(L)로 되돌아간다.
 */
const hiResSpineUrl = (url) => url.replace(/\/SIDE\/L(\?.*)?$/, '/SIDE/XL$1');

/** 이미지 원래 폭(px, 400px 높이 기준)을 책등 폭 30~40px 로 옮긴다 */
function spineWidthFor(naturalWidth) {
  const [realMin, realMax] = REAL_SPINE_WIDTH_RANGE;
  const [min, max] = IMAGE_WIDTH_RANGE;
  const t = Math.min(1, Math.max(0, (naturalWidth - realMin) / (realMax - realMin)));
  return Math.round(min + t * (max - min));
}

/** 가운데 색과 구분되는 표지 색 두 가지를 위·아래 블록 색으로 고른다. 쓸 색이 없으면 null */
function pickBlockColors(color, paletteColors) {
  const distinct = paletteColors.filter((c) => colorDistance(c, color) > MIN_BLOCK_DISTANCE);
  if (distinct.length === 0) return null;
  return { top: distinct[0], bottom: distinct[1] ?? distinct[0] };
}

/**
 * 책장에 꽂힌 책의 옆면(책등). 제목은 세로로 쓴다.
 * 정면 표지는 BookCover 를 쓴다.
 *
 * 도서 검색은 책등 사진을 주지 않으므로, 서버가 표지에서 뽑아 준 색으로 3단을 꾸민다.
 *  - 가운데(제목 칸): color  — 표지의 가장 큰 대표 색 (또는 사용자가 고른 색)
 *  - 위·아래 블록   : paletteColors 중 가운데 색과 구분되는 두 색
 * 표지 색이 없는 책(직접 입력)은 가운데 색 하나 + 얇은 장식 선만 나온다.
 *
 * 예스24 책등 이미지(spineImageUrl)가 있으면 그 이미지를 책등으로 쓴다. 제목이 읽히도록 색 책등보다 2배쯤 높고, 폭은 책 두께
 * 순서를 따르되 30~40px 안으로 모은다(이미지는 옆으로 조금 늘어난다). 불러오지 못하거나 세로로 긴 모양이
 * 아니면(대체 이미지 등) 위의 색 꾸밈으로 되돌아간다.
 */
/** 책장 줄에서 이 책이 차지할 폭의 비중 (책마다 조금씩 달라 두께가 들쭉날쭉해 보인다) */
export const spineWeight = (seed = 0) => WIDTHS[Math.abs(Number(seed) || 0) % WIDTHS.length];

export default function BookSpine({
  title,
  author,
  color,
  paletteColors = [],
  spineImageUrl,
  seed = 0,
  certified = false,
}) {
  const n = Math.abs(Number(seed) || 0);
  const height = HEIGHTS[(n * 3 + 1) % HEIGHTS.length];
  const [image, setImage] = useState(null); // 쓸 수 있는 책등 이미지면 { width }
  const [imageFailed, setImageFailed] = useState(false);
  const [useOriginalUrl, setUseOriginalUrl] = useState(false); // 고해상도(XL)를 못 불러와 원래 주소로 되돌아갔는지
  const showImage = Boolean(spineImageUrl) && !imageFailed;
  const hasImage = showImage && image !== null;
  const blocks = hasImage ? null : pickBlockColors(color, paletteColors);

  const handleImageLoad = (e) => {
    const { naturalWidth: w, naturalHeight: h } = e.currentTarget;
    if (!w || !h || h / w < MIN_SPINE_ASPECT) {
      setImageFailed(true);
      return;
    }
    // 400px 높이로 맞췄을 때의 폭으로 두께를 가늠한다 (이미지 높이가 달라도 같은 기준)
    setImage({ width: spineWidthFor((w * 400) / h) });
  };

  return (
    <Spine
      $color={color}
      $light={isLightColor(color)}
      $blocks={Boolean(blocks)}
      $image={hasImage}
      $width={image?.width ?? WIDTHS[n % WIDTHS.length]}
      $height={Math.round(height * HEIGHT_SCALE)}
      aria-label={`${title} 책등`}
    >
      {showImage && (
        <SpineImage
          src={useOriginalUrl ? spineImageUrl : hiResSpineUrl(spineImageUrl)}
          alt=""
          $ready={hasImage}
          onLoad={handleImageLoad}
          onError={() => (useOriginalUrl || hiResSpineUrl(spineImageUrl) === spineImageUrl ? setImageFailed(true) : setUseOriginalUrl(true))}
        />
      )}
      {blocks && (
        <>
          <Block $edge="top" $height={TOP_BLOCK} $color={blocks.top} aria-hidden />
          <Block $edge="bottom" $height={BOTTOM_BLOCK} $color={blocks.bottom} aria-hidden />
        </>
      )}
      {/* 책등 이미지에는 제목이 이미 들어 있으므로 글자는 쓰지 않는다 */}
      {!hasImage && <SpineTitle>{title}</SpineTitle>}
      {certified && (
        <Mark aria-label="완독 인증 완료" $overImage={hasImage}>
          <BadgeCheck size={13} strokeWidth={2.6} aria-hidden />
        </Mark>
      )}
      {!hasImage && author && <SpineAuthor>{author}</SpineAuthor>}
    </Spine>
  );
}

const Spine = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 100%; /* 폭은 책장 줄(SortableCell)이 정한다 */
  height: ${({ $height }) => $height}px;
  /* 위·아래 색 블록이 있으면 그만큼 안쪽에서 글자가 시작한다 (책등 이미지는 꽉 채운다) */
  padding: ${({ $blocks, $image }) => ($image ? '0' : $blocks ? `${TOP_BLOCK + 8}px 0 ${BOTTOM_BLOCK + 6}px` : '14px 0 12px')};
  overflow: hidden;
  border-radius: 2px 2px 1px 1px;
  color: ${({ $light }) => ($light ? '#212529' : '#FFFFFF')};
  background: ${({ $color }) => $color};
  box-shadow:
    1px 0 0 rgba(0, 0, 0, 0.25),
    0 -1px 2px rgba(0, 0, 0, 0.2);

  /* 둥근 책등의 음영 (색 블록 위, 글자 아래) */
  &::before {
    content: '';
    position: absolute;
    z-index: 1;
    inset: 0;
    background: linear-gradient(
      90deg,
      rgba(0, 0, 0, 0.22) 0,
      rgba(255, 255, 255, 0.14) 22%,
      transparent 52%,
      rgba(0, 0, 0, 0.2) 100%
    );
    pointer-events: none;
  }

  /* 3단 꾸밈이 없는 책은 얇은 장식 선만 (위 두 줄 · 아래 한 줄) */
  ${({ $blocks, $image }) =>
    !$blocks &&
    !$image &&
    `
    &::after {
      content: '';
      position: absolute;
      z-index: 1;
      inset: 0;
      opacity: 0.45;
      background:
        linear-gradient(currentColor, currentColor) top 7px left 4px / calc(100% - 8px) 1px no-repeat,
        linear-gradient(currentColor, currentColor) top 10px left 4px / calc(100% - 8px) 1px no-repeat,
        linear-gradient(currentColor, currentColor) bottom 7px left 4px / calc(100% - 8px) 1px no-repeat;
      pointer-events: none;
    }
  `}
`;

/* 예스24 책등 이미지. 불러오는 동안은 숨겨 두고(색 책등이 먼저 보임) 쓸 수 있다고 확인되면 책등을 덮는다 */
const SpineImage = styled.img`
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
  opacity: ${({ $ready }) => ($ready ? 1 : 0)};
`;

/* 책등 위·아래의 색 블록. 가운데 칸과 맞닿는 쪽에 가는 경계선을 둔다 */
const Block = styled.div`
  position: absolute;
  right: 0;
  left: 0;
  ${({ $edge }) => $edge}: 0;
  height: ${({ $height }) => $height}px;
  background: ${({ $color }) => $color};
  box-shadow: ${({ $edge }) =>
    $edge === 'top'
      ? 'inset 0 -1px 0 rgba(0, 0, 0, 0.25), inset 0 -2px 0 rgba(255, 255, 255, 0.12)'
      : 'inset 0 1px 0 rgba(0, 0, 0, 0.25), inset 0 2px 0 rgba(255, 255, 255, 0.12)'};
`;

const SpineTitle = styled.p`
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  writing-mode: vertical-rl;
  font-family: ${({ theme }) => theme.font.serif};
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.02em;
`;

const SpineAuthor = styled.p`
  position: relative;
  z-index: 2;
  margin-top: 6px;
  max-height: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  writing-mode: vertical-rl;
  font-size: 9px;
  opacity: 0.75;
`;

/* 제목 바로 아래(저자명 위)에 놓인다 */
const Mark = styled.span`
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  margin-top: 6px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.furniture.trim};

  /* 책등 이미지 위에서는 이미지 아래쪽에 작은 원 배경을 깔아 읽히게 한다 */
  ${({ $overImage }) =>
    $overImage &&
    `
    position: absolute;
    bottom: 4px;
    left: 50%;
    width: 18px;
    height: 18px;
    margin: 0;
    transform: translateX(-50%);
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.55);
  `}
`;
