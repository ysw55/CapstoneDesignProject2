import React from 'react';
import styled from 'styled-components';
import { BadgeCheck, CheckCircle2 } from 'lucide-react';
import BookCover from '../book/BookCover';
import { Field, Input, Label, SegmentButton, Segmented } from './FormScreen';

export const SHELF = 'shelf';
export const DIRECT = 'direct';

/** 처음 상태: 책장에 책이 있으면 '책장에서 고르기', 없으면 '직접 입력' */
export const initialBookPick = (books, preset) => {
  const shelfBook = preset && books.find((b) => b.title === preset.title && b.author === preset.author);
  if (preset && !shelfBook) {
    return { source: DIRECT, bookId: null, bookTitle: preset.title, author: preset.author };
  }
  return {
    source: shelfBook || books.length ? SHELF : DIRECT,
    bookId: shelfBook?.id ?? null,
    bookTitle: '',
    author: '',
  };
};

/** 어느 방식으로 골랐든 최종적으로 쓰는 { title, author, book } */
export const resolveBookPick = (books, pick) => {
  if (pick.source === SHELF) {
    const book = books.find((b) => b.id === pick.bookId);
    return { title: book?.title ?? '', author: book?.author ?? '', book };
  }
  return { title: pick.bookTitle.trim(), author: pick.author.trim(), book: null };
};

/**
 * '내 책장에서 고르기 / 직접 입력' 책 선택 UI (커뮤니티 글쓰기, 완독 인증에서 공용)
 * - value: { source, bookId, bookTitle, author }
 * - onChange(patch): 바뀐 필드만 넘김
 * - isDisabled(book): true 면 선택 불가 (예: 이미 완독 인증한 책)
 */
export default function BookPicker({ books, value, onChange, label = '어떤 책인가요?', isDisabled }) {
  const picked = books.find((b) => b.id === value.bookId);

  return (
    <Field as="fieldset">
      <Label as="legend">{label}</Label>
      <Segmented role="tablist">
        <SegmentButton
          type="button"
          role="tab"
          aria-selected={value.source === SHELF}
          $active={value.source === SHELF}
          onClick={() => onChange({ source: SHELF })}
        >
          내 책장에서 고르기
        </SegmentButton>
        <SegmentButton
          type="button"
          role="tab"
          aria-selected={value.source === DIRECT}
          $active={value.source === DIRECT}
          onClick={() =>
            // 책장에서 골라 둔 책이 있으면 그 정보를 채운 채로 직접 입력 전환
            onChange(
              picked && !value.bookTitle && !value.author
                ? { source: DIRECT, bookTitle: picked.title, author: picked.author }
                : { source: DIRECT }
            )
          }
        >
          직접 입력
        </SegmentButton>
      </Segmented>

      {value.source === SHELF ? (
        books.length ? (
          <ShelfList role="radiogroup" aria-label="내 책장의 책">
            {books.map((book) => {
              const selected = value.bookId === book.id;
              const disabled = isDisabled?.(book) ?? false;
              return (
                <ShelfItem key={book.id} $selected={selected} $disabled={disabled}>
                  <input
                    type="radio"
                    name="shelfBook"
                    checked={selected}
                    disabled={disabled}
                    onChange={() => onChange({ bookId: book.id })}
                  />
                  <BookCover title={book.title} color={book.coverColor} size="sm" />
                  <ShelfText>
                    <strong>{book.title}</strong>
                    <span>{book.author}</span>
                  </ShelfText>
                  {disabled ? (
                    <DoneTag>
                      <BadgeCheck size={14} strokeWidth={2.2} aria-hidden />
                      인증 완료
                    </DoneTag>
                  ) : (
                    selected && <CheckCircle2 size={22} strokeWidth={2.2} aria-hidden />
                  )}
                </ShelfItem>
              );
            })}
          </ShelfList>
        ) : (
          <EmptyShelf>책장이 비어 있어요. ‘직접 입력’으로 책 정보를 적어 주세요.</EmptyShelf>
        )
      ) : (
        <DirectInputs>
          <Input
            value={value.bookTitle}
            onChange={(e) => onChange({ bookTitle: e.target.value })}
            placeholder="책 제목"
            aria-label="책 제목"
            maxLength={60}
            autoComplete="off"
          />
          <Input
            value={value.author}
            onChange={(e) => onChange({ author: e.target.value })}
            placeholder="저자"
            aria-label="저자"
            maxLength={40}
            autoComplete="off"
          />
        </DirectInputs>
      )}
    </Field>
  );
}

const ShelfList = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  max-height: 268px;
  margin-top: ${({ theme }) => theme.space(1)};
  overflow-y: auto;
`;

const ShelfItem = styled.label`
  position: relative;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  padding: 10px 14px;
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.accent : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.accentSoft : theme.colors.surface)};
  cursor: ${({ $disabled }) => ($disabled ? 'not-allowed' : 'pointer')};
  opacity: ${({ $disabled }) => ($disabled ? 0.55 : 1)};
  transition:
    background ${({ theme }) => theme.motion.fast},
    border-color ${({ theme }) => theme.motion.fast};

  > svg {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.accent};
  }

  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  &:has(input:focus-visible) {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 2px;
  }
`;

const ShelfText = styled.span`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;

  strong {
    font-size: 15px;
    font-weight: 700;
  }

  span {
    font-size: 13px;
    color: ${({ theme }) => theme.colors.textSecondary};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const DoneTag = styled.span`
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 3px;
  font-size: 12px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
`;

const EmptyShelf = styled.p`
  padding: ${({ theme }) => theme.space(4)};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 14px;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.backgroundAlt};
`;

const DirectInputs = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  margin-top: ${({ theme }) => theme.space(1)};
`;
