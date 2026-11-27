import React, { useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { Camera, Check, ChevronLeft, Trash2 } from 'lucide-react';
import Button from '../components/common/Button';
import PhotoCropper from '../components/common/PhotoCropper';
import ProfileAvatar from '../components/common/ProfileAvatar';
import { BACKGROUNDS, FRAME_ITEMS } from '../utils/shopItems';
import { useAuth } from '../context/AuthContext';

/** 프로필 > 프로필 꾸미기 (/profile/edit) — 배경색·사진·테두리 장식 선택 후 서버에 저장 */
export default function ProfileEditPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, updateProfileSkin } = useAuth();
  const [bg, setBg] = useState(user?.profileBackgroundColor ?? null);
  const [image, setImage] = useState(user?.profileImage ?? null);
  const [frame, setFrame] = useState(user?.profileFrame ?? null);
  const ownedFrames = FRAME_ITEMS.filter((i) => user?.ownedItems?.includes(i.code));
  const [cropFile, setCropFile] = useState(null);
  const fileRef = useRef(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleBack = () => (location.key === 'default' ? navigate('/profile', { replace: true }) : navigate(-1));

  const handlePickPhoto = (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    if (!file.type.startsWith('image/')) return setError('이미지 파일만 올릴 수 있어요.');
    setError('');
    setCropFile(file); // 자르기 화면에서 보일 부분을 고른 뒤 적용
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      await updateProfileSkin(bg, image, frame);
      handleBack();
    } catch (e) {
      setError(e?.message || '저장하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setSaving(false);
    }
  };

  const renderSwatches = (colors, value, onChange, label) => (
    <>
    <Swatches role="radiogroup" aria-label={label}>
      <Swatch type="button" $empty aria-label="없음" aria-checked={value === null} role="radio" onClick={() => onChange(null)}>
        {value === null && <Check size={16} strokeWidth={3} />}
      </Swatch>
      {colors.map((c) => (
        <Swatch key={c} type="button" role="radio" aria-label={c} aria-checked={value === c} $color={c} onClick={() => onChange(c)}>
          {value === c && <Check size={16} strokeWidth={3} color={c === '#FFFFFF' || c === '#FDE68A' ? '#212529' : '#fff'} />}
        </Swatch>
      ))}
    </Swatches>
    {colors.length === 0 && <Empty>산 색이 없어요. 프로필 &gt; 상점에서 50P에 구할 수 있어요.</Empty>}
    </>
  );

  return (
    <>
      <TopBar>
        <IconBtn type="button" aria-label="뒤로 가기" onClick={handleBack}>
          <ChevronLeft size={26} strokeWidth={2} />
        </IconBtn>
        <TopTitle>프로필 꾸미기</TopTitle>
        <span aria-hidden style={{ width: 44 }} />
      </TopBar>

      <Content>
        <Preview>
          <ProfileAvatar name={user.username} background={bg ?? undefined} image={image ?? undefined} frame={frame ?? undefined} size={96} />
          <strong>{user.username}</strong>
          <PhotoActions>
            <Button type="button" $variant="secondary" onClick={() => fileRef.current?.click()}>
              <Camera size={16} strokeWidth={2.2} aria-hidden />
              {image ? '사진 바꾸기' : '사진 올리기'}
            </Button>
            {image && (
              <Button type="button" $variant="secondary" onClick={() => setImage(null)}>
                <Trash2 size={16} strokeWidth={2.2} aria-hidden />
                삭제
              </Button>
            )}
          </PhotoActions>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={handlePickPhoto} />
        </Preview>

        <Section>
          <h3>프로필 배경색</h3>
          {renderSwatches(BACKGROUNDS, bg, setBg, '프로필 배경색')}
        </Section>

        <Section>
          <h3>테두리 장식</h3>
          {ownedFrames.length === 0 ? (
            <Empty>
              산 장식이 없어요. 프로필 &gt; 상점에서 포인트로 구할 수 있어요.
            </Empty>
          ) : (
            <Swatches role="radiogroup" aria-label="테두리 장식">
              <Swatch type="button" $empty role="radio" aria-label="없음" aria-checked={frame === null} onClick={() => setFrame(null)}>
                {frame === null && <Check size={16} strokeWidth={3} />}
              </Swatch>
              {ownedFrames.map((i) => (
                <Swatch key={i.code} type="button" role="radio" aria-label={i.name} aria-checked={frame === i.code} $color={i.color} onClick={() => setFrame(i.code)}>
                  {frame === i.code && <Check size={16} strokeWidth={3} color={i.color === '#FFFFFF' ? '#212529' : '#fff'} />}
                </Swatch>
              ))}
            </Swatches>
          )}
        </Section>

        {error && <ErrorText role="alert">{error}</ErrorText>}
        <Button type="button" $variant="primary" $block disabled={saving} onClick={handleSave}>
          {saving ? '저장 중…' : '저장하기'}
        </Button>
      </Content>

      {cropFile && (
        <PhotoCropper
          file={cropFile}
          onCancel={() => setCropFile(null)}
          onDone={(dataUrl) => {
            setImage(dataUrl);
            setCropFile(null);
          }}
        />
      )}
    </>
  );
}

const TopBar = styled.header`
  position: sticky;
  top: env(safe-area-inset-top, 0px);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 ${({ theme }) => theme.space(2)};
  background: ${({ theme }) => theme.colors.background};
`;

const IconBtn = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
`;

const TopTitle = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 800;
`;

const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(5)};
  padding: ${({ theme }) => theme.space(2)} ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
`;

const Preview = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  padding: ${({ theme }) => theme.space(6)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
`;

const PhotoActions = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.space(2)};

  button {
    height: 40px;
    padding: 0 ${({ theme }) => theme.space(4)};
    font-size: 13px;
  }
`;

const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};

  h3 {
    font-size: 14px;
    font-weight: 700;
  }
`;

const Swatches = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
`;

const Swatch = styled.button`
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid ${({ theme }) => theme.colors.border};
  background: ${({ $color, theme }) => $color ?? theme.colors.surfaceMuted};
  ${({ $empty, theme }) => $empty && `color: ${theme.colors.textSecondary};`}
`;

const Empty = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const ErrorText = styled.p`
  font-size: 13px;
  color: #e03131;
`;
