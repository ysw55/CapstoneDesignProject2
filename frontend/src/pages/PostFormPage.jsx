import React, { useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { Globe } from 'lucide-react';
import FormScreen, { Counter, Field, Label, LabelRow, Textarea } from '../components/form/FormScreen';
import LoadingScreen from '../components/common/LoadingScreen';
import BookPicker, { initialBookPick, resolveBookPick } from '../components/form/BookPicker';
import { isMyPost, useAppData } from '../context/AppDataContext';
import { TAG_OPTIONS } from '../mockData';

const QUOTE_MAX = 300;

/** 커뮤니티 글쓰기 / 수정 (/posts/new, /posts/:postId/edit) — 서버 데이터가 준비된 뒤 폼을 연다 */
export default function PostFormPage() {
  const { postId } = useParams();
  const { posts, feedStatus, shelfStatus } = useAppData();

  // 주소로 바로 들어온 경우 피드·책장을 불러올 때까지 대기 (폼 초기값이 비지 않도록)
  if ((postId && feedStatus === 'loading') || shelfStatus === 'loading') return <LoadingScreen />;

  const editing = postId ? posts.find((p) => String(p.id) === postId) : null;
  // 없는 글이거나 남의 글이면 수정 불가
  if (postId && (!editing || !isMyPost(editing))) return <Navigate to="/" replace />;

  return <PostForm editing={editing} />;
}

function PostForm({ editing }) {
  const navigate = useNavigate();
  const { books, addPost, updatePost } = useAppData();
  const isEdit = Boolean(editing);
  const [error, setError] = useState(null);

  const [form, setForm] = useState(() =>
    editing
      ? {
          // 수정: 책장에 같은 책이 있으면 '책장에서 고르기' 상태로 연다
          ...initialBookPick(books, { title: editing.bookTitle, author: editing.author }),
          quote: editing.quote,
          tagLabel: editing.tag.label,
        }
      : { ...initialBookPick(books), quote: '', tagLabel: '' }
  );
  const [saving, setSaving] = useState(false);

  const set = (patch) => {
    setError(null);
    setForm((f) => ({ ...f, ...patch }));
  };

  // 어느 방식이든 최종적으로 쓰는 책 정보
  const { title: bookTitle, author } = resolveBookPick(books, form);
  const canSubmit = bookTitle && author && form.quote.trim() && form.tagLabel;

  const handleSubmit = async () => {
    setSaving(true);
    setError(null);
    const post = {
      bookTitle,
      author,
      quote: form.quote.trim(),
      tag: TAG_OPTIONS.find((t) => t.label === form.tagLabel),
    };
    try {
      if (isEdit) await updatePost(editing.id, post);
      else await addPost(post);
      navigate('/', { replace: true, state: { toast: isEdit ? '글을 수정했어요' : '커뮤니티에 공유했어요' } });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <FormScreen
      title={isEdit ? '글 수정' : '커뮤니티에 공유'}
      submitLabel={isEdit ? '수정 완료' : '공유하기'}
      onSubmit={handleSubmit}
      canSubmit={canSubmit}
      saving={saving}
      hint="책, 문장, 상황 태그를 모두 입력해 주세요"
      fallbackPath="/"
      errorMessage={error}
    >
      <BookPicker books={books} value={form} onChange={set} />

      <Field>
        <LabelRow>
          <Label htmlFor="quote">인상 깊은 문장</Label>
          <Counter $full={form.quote.length >= QUOTE_MAX}>
            {form.quote.length}/{QUOTE_MAX}
          </Counter>
        </LabelRow>
        <Textarea
          id="quote"
          value={form.quote}
          onChange={(e) => set({ quote: e.target.value })}
          placeholder="마음에 남은 한 문장을 그대로 옮겨 적어 보세요"
          maxLength={QUOTE_MAX}
          rows={5}
        />
      </Field>

      <Field as="fieldset">
        <Label as="legend">어떤 순간에 읽으면 좋을까요?</Label>
        <TagGrid>
          {TAG_OPTIONS.map((tag) => {
            const selected = form.tagLabel === tag.label;
            return (
              <TagOption key={tag.label} $selected={selected}>
                <input
                  type="radio"
                  name="tag"
                  value={tag.label}
                  checked={selected}
                  onChange={() => set({ tagLabel: tag.label })}
                />
                <span aria-hidden>{tag.emoji}</span>
                {tag.label}
              </TagOption>
            );
          })}
        </TagGrid>
      </Field>

      <PublicNote>
        <Globe size={14} strokeWidth={2.2} aria-hidden />
        커뮤니티 피드에 공개돼요. 내 책장에는 추가되지 않아요.
      </PublicNote>
    </FormScreen>
  );
}

const hiddenRadio = `
  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }
`;

const TagGrid = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.space(2)};
  margin-top: ${({ theme }) => theme.space(1)};
`;

const TagOption = styled.label`
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.accent : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 14px;
  font-weight: 600;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.accent : theme.colors.textSecondary)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.accentSoft : theme.colors.surface)};
  cursor: pointer;
  user-select: none;
  transition:
    background ${({ theme }) => theme.motion.fast},
    border-color ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.95);
  }

  ${hiddenRadio}

  &:has(input:focus-visible) {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 2px;
  }
`;

const PublicNote = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;
