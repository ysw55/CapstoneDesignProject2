import React, { useState } from 'react';
import { Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { BadgeCheck, Check, ChevronRight, Lock } from 'lucide-react';
import FormScreen, { Field, Input, Label } from '../components/form/FormScreen';
import LoadingScreen from '../components/common/LoadingScreen';
import BookCover from '../components/book/BookCover';
import BookSearch from '../components/form/BookSearch';
import { pickCoverColor, useAppData } from '../context/AppDataContext';
import { COVER_PALETTE } from '../mockData';
import { isLightColor, todayISO } from '../utils/format';

/** 내 책장에 책 등록 / 수정 (/books/new, /books/:bookId/edit) — 서버 책장을 불러온 뒤 폼을 연다 */
export default function BookFormPage() {
  const { bookId } = useParams();
  const { books, shelfStatus } = useAppData();

  if (shelfStatus === 'loading') return <LoadingScreen />;

  const editing = bookId ? books.find((b) => String(b.id) === bookId) : null;
  // 없는 책(또는 다른 계정의 책)을 수정하려고 하면 책장으로
  if (bookId && !editing) return <Navigate to="/bookshelf" replace />;

  return <BookForm editing={editing} />;
}

function BookForm({ editing }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { books, addBook, updateBook } = useAppData();
  const isEdit = Boolean(editing);
  // 책장 화면에서 + 로 들어오면 그때 보던 책장(?shelfId=)에 꽂는다. 없으면 서버가 첫 번째 책장에 꽂는다
  const shelfId = Number(searchParams.get('shelfId')) || undefined;
  // 완독 인증한 책은 보상 중복을 막기 위해 서버가 표지 색 외 변경을 막는다 → 화면에서도 잠금
  const locked = Boolean(editing?.certified);
  const [error, setError] = useState(null);

  const [form, setForm] = useState(() =>
    editing
      ? { title: editing.title, author: editing.author, completedAt: editing.completedAt, coverColor: editing.coverColor }
      : { title: '', author: '', completedAt: todayISO(), coverColor: '' }
  );
  const [saving, setSaving] = useState(false);
  // 도서 검색으로 고른 책의 ISBN · 출판사 · 표지 (제목/저자를 직접 고치면 다른 책이 되므로 비운다)
  const [picked, setPicked] = useState(null);

  const update = (field) => (e) => {
    setError(null);
    if (field === 'title' || field === 'author') setPicked(null);
    setForm((f) => ({ ...f, [field]: e.target.value }));
  };

  const handlePick = (book) => {
    setError(null);
    setForm((f) => ({ ...f, title: book.title, author: book.author ?? '' }));
    setPicked({ isbn: book.isbn, publisher: book.publisher, coverImageUrl: book.coverImageUrl });
  };
  const today = todayISO();
  const canSubmit = form.title.trim() && form.author.trim() && form.completedAt && form.completedAt <= today;

  const handleSubmit = async () => {
    setSaving(true);
    setError(null);
    const book = {
      ...form,
      title: form.title.trim(),
      author: form.author.trim(),
      coverColor: form.coverColor || pickCoverColor(books), // 고르지 않았으면 자동 선택
      ...(!isEdit && picked), // 검색으로 고른 책이면 ISBN · 출판사 · 표지도 함께 저장
    };
    try {
      if (isEdit) await updateBook(editing.id, book);
      else await addBook({ ...book, shelfId });
      navigate('/bookshelf', { replace: true, state: { toast: isEdit ? '책 정보를 수정했어요' : '책장에 꽂아 뒀어요' } });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <FormScreen
      title={isEdit ? '책 정보 수정' : '책장에 추가'}
      submitLabel={isEdit ? '수정 완료' : '책장에 꽂기'}
      onSubmit={handleSubmit}
      canSubmit={canSubmit}
      saving={saving}
      hint="제목, 저자, 완독 날짜를 입력해 주세요"
      fallbackPath="/bookshelf"
      errorMessage={error}
    >
      <Preview>
        <PreviewCover aria-hidden>
          <BookCover
            title={form.title || '책 제목'}
            author={form.author || '저자'}
            color={form.coverColor || pickCoverColor(books)}
            imageUrl={picked?.coverImageUrl ?? editing?.coverImageUrl}
          />
        </PreviewCover>
        <PrivateNote>
          <Lock size={14} strokeWidth={2.2} aria-hidden />내 책장은 나만 볼 수 있어요
        </PrivateNote>
        {locked && (
          <CertifiedNote>
            <BadgeCheck size={14} strokeWidth={2.4} aria-hidden />
            완독 인증 완료 · 표지 색만 바꿀 수 있어요
          </CertifiedNote>
        )}
        {editing && !editing.certified && (
          <CertifyLink type="button" onClick={() => navigate(`/books/certify?bookId=${editing.id}`)}>
            이 책 완독 인증하고 보상 받기
            <ChevronRight size={16} strokeWidth={2.2} aria-hidden />
          </CertifyLink>
        )}
      </Preview>

      {!isEdit && (
        <Field>
          <Label as="span">책 검색</Label>
          <BookSearch onPick={handlePick} />
        </Field>
      )}

      <Field>
        <Label htmlFor="title">책 제목</Label>
        <Input id="title" value={form.title} onChange={update('title')} placeholder="예) 데미안" maxLength={60} autoComplete="off" disabled={locked} />
      </Field>

      <Field>
        <Label htmlFor="author">저자</Label>
        <Input id="author" value={form.author} onChange={update('author')} placeholder="예) 헤르만 헤세" maxLength={40} autoComplete="off" disabled={locked} />
      </Field>

      <Field>
        <Label htmlFor="completedAt">다 읽은 날짜</Label>
        <Input id="completedAt" type="date" value={form.completedAt} max={today} onChange={update('completedAt')} disabled={locked} />
      </Field>

      <Field as="fieldset">
        <Label as="legend">표지 색상</Label>
        <Swatches>
          {COVER_PALETTE.map((color) => {
            const selected = form.coverColor === color;
            return (
              <Swatch key={color} $color={color} $selected={selected}>
                <input type="radio" name="coverColor" value={color} checked={selected} onChange={update('coverColor')} aria-label={`표지 색 ${color}`} />
                {selected && <Check size={18} strokeWidth={3} color={isLightColor(color) ? '#212529' : '#fff'} aria-hidden />}
              </Swatch>
            );
          })}
        </Swatches>
      </Field>
    </FormScreen>
  );
}

const Preview = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
`;

const PreviewCover = styled.div`
  width: 96px;
`;

const PrivateNote = styled.p`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.backgroundAlt};
`;

const CertifiedNote = styled.p`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
`;

const CertifyLink = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 36px;
  padding: 0 12px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 13px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};

  &:active {
    transform: scale(0.97);
  }
`;

const Swatches = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.space(3)};
`;

const Swatch = styled.label`
  position: relative;
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: ${({ $color }) => $color};
  box-shadow: ${({ $selected, theme }) =>
    $selected ? `0 0 0 2px ${theme.colors.surface}, 0 0 0 4px ${theme.colors.accent}` : `inset 0 0 0 1px rgba(17,17,17,0.08)`};
  cursor: pointer;
  transition:
    box-shadow ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.9);
  }

  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  &:has(input:focus-visible) {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 4px;
  }
`;
