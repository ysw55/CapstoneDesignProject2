import React, { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { Loader2, Search, X } from 'lucide-react';
import BookCover from '../book/BookCover';
import { searchBooks } from '../../api/books';
import { Input } from './FormScreen';

const DEBOUNCE_MS = 350;

/**
 * 도서 검색 (카카오 책 검색). 입력을 멈추면 자동으로 찾고, 결과를 누르면 onPick 으로 넘긴다.
 * 검색을 쓸 수 없으면(키 없음·네트워크 오류) 안내만 하고 아래 직접 입력으로 이어지게 한다.
 */
export default function BookSearch({ onPick }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState('idle'); // 'idle' | 'loading' | 'done' | 'unavailable'
  const seq = useRef(0); // 늦게 도착한 이전 검색 결과를 버리기 위한 요청 번호

  const run = async (q, nextPage) => {
    const mine = ++seq.current;
    setStatus('loading');
    try {
      const res = await searchBooks(q, nextPage);
      if (mine !== seq.current) return;
      setResults((prev) => (nextPage === 1 ? res.results : [...prev, ...res.results]));
      setHasMore(res.hasMore);
      setPage(nextPage);
      setStatus('done');
    } catch {
      if (mine === seq.current) setStatus('unavailable');
    }
  };

  useEffect(() => {
    const q = query.trim();
    if (!q) {
      seq.current++;
      setResults([]);
      setHasMore(false);
      setStatus('idle');
      return undefined;
    }
    const timer = setTimeout(() => run(q, 1), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query]);

  const pick = (book) => {
    onPick(book);
    setQuery('');
  };

  return (
    <Box>
      <InputWrap>
        <Search size={18} strokeWidth={2.2} aria-hidden />
        <SearchInput
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          // 폼 안이라 Enter 를 누르면 저장되어 버리므로 막는다 (입력을 멈추면 자동으로 검색됨)
          onKeyDown={(e) => e.key === 'Enter' && e.preventDefault()}
          placeholder="책 제목이나 저자로 검색"
          maxLength={100}
          autoComplete="off"
          aria-label="책 검색"
        />
        {status === 'loading' && <Spinner size={18} strokeWidth={2.4} aria-hidden />}
        {query && status !== 'loading' && (
          <ClearButton type="button" aria-label="검색어 지우기" onClick={() => setQuery('')}>
            <X size={16} strokeWidth={2.4} />
          </ClearButton>
        )}
      </InputWrap>

      {status === 'unavailable' && <Note>도서 검색을 지금은 쓸 수 없어요. 아래에 직접 입력해 주세요.</Note>}
      {status === 'done' && results.length === 0 && <Note>검색 결과가 없어요. 아래에 직접 입력할 수도 있어요.</Note>}

      {results.length > 0 && (
        <Results>
          {results.map((book, i) => (
            <li key={`${book.isbn ?? book.title}-${i}`}>
              <Result type="button" onClick={() => pick(book)}>
                <BookCover title={book.title} author={book.author} color="#E9ECEF" size="sm" imageUrl={book.coverImageUrl} />
                <Text>
                  <strong>{book.title}</strong>
                  <span>
                    {[book.author, book.publisher, book.publishedAt?.slice(0, 4)].filter(Boolean).join(' · ')}
                  </span>
                </Text>
              </Result>
            </li>
          ))}
          {hasMore && (
            <li>
              <MoreButton type="button" disabled={status === 'loading'} onClick={() => run(query.trim(), page + 1)}>
                더 보기
              </MoreButton>
            </li>
          )}
        </Results>
      )}
    </Box>
  );
}

const Box = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
`;

const InputWrap = styled.div`
  position: relative;
  display: flex;
  align-items: center;

  > svg:first-child {
    position: absolute;
    left: 14px;
    color: ${({ theme }) => theme.colors.textTertiary};
    pointer-events: none;
  }
`;

const SearchInput = styled(Input)`
  padding-left: 42px;
  padding-right: 42px;
`;

const Spinner = styled(Loader2)`
  position: absolute;
  right: 14px;
  color: ${({ theme }) => theme.colors.textTertiary};
  animation: spin 800ms linear infinite;

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;

const ClearButton = styled.button`
  position: absolute;
  right: 6px;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const Note = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const Results = styled.ul`
  max-height: 340px;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  list-style: none;

  li + li {
    border-top: 1px solid ${({ theme }) => theme.colors.border};
  }
`;

const Result = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  width: 100%;
  padding: 10px 12px;
  text-align: left;

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;

const Text = styled.span`
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: ${({ theme }) => theme.fontSize.md};
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
    color: ${({ theme }) => theme.colors.textSecondary};
  }
`;

const MoreButton = styled.button`
  width: 100%;
  height: 44px;
  font-size: 14px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};

  &:disabled {
    opacity: 0.5;
  }
`;
