import React, { useEffect, useRef, useState } from 'react';
import styled, { keyframes } from 'styled-components';
import { ArrowUp } from 'lucide-react';
import { useAppData } from '../../context/AppDataContext';
import { useAuth } from '../../context/AuthContext';

const MAX_LENGTH = 200;

/** 피드 카드 아래로 펼쳐지는 댓글 목록 + 입력란 (서버 GET/POST /api/quotes/{id}/comments) */
export default function CommentPanel({ post }) {
  const { loadComments, addComment } = useAppData();
  const { user, isLoggedIn, openLogin } = useAuth();
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  const trimmed = text.trim();
  const comments = post.comments;
  const loading = comments === null && !loadError;

  // 처음 열 때 서버에서 댓글 목록을 불러옴
  useEffect(() => {
    if (post.comments !== null) return;
    loadComments(post.id).catch((err) => setLoadError(err));
  }, [post.id, post.comments, loadComments]);

  // 열리자마자 바로 입력할 수 있게 포커스
  useEffect(() => {
    if (isLoggedIn) inputRef.current?.focus({ preventScroll: true });
  }, [isLoggedIn]);

  // 새 댓글이 달리면 목록(페이지 말고 목록만) 맨 아래로 스크롤
  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTo({ top: list.scrollHeight, behavior: 'smooth' });
  }, [comments?.length]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!trimmed || sending) return;
    setSending(true);
    setError(null);
    try {
      await addComment(post.id, trimmed);
      setText(''); // 입력란 초기화
    } catch (err) {
      setError(err.message); // 입력한 내용은 그대로 남겨 다시 보낼 수 있게
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  };

  return (
    <Panel>
      {loading && <MoreLabel>댓글을 불러오는 중…</MoreLabel>}
      {loadError && <MoreLabel role="alert">{loadError.message}</MoreLabel>}
      {comments?.length === 0 && <MoreLabel>아직 댓글이 없어요. 첫 댓글을 남겨 보세요.</MoreLabel>}

      {comments?.length > 0 && (
        <List ref={listRef}>
          {comments.map((c) => (
            <Comment key={c.id}>
              <SmallAvatar aria-hidden>{c.userInitial}</SmallAvatar>
              <Body>
                <CommentMeta>
                  <strong>{c.userName}</strong>
                  <span>{c.createdAt}</span>
                </CommentMeta>
                <CommentText>{c.text}</CommentText>
              </Body>
            </Comment>
          ))}
        </List>
      )}

      {isLoggedIn ? (
        <Form onSubmit={handleSubmit}>
          <SmallAvatar aria-hidden>{user.username.charAt(0).toUpperCase()}</SmallAvatar>
          <InputWrap>
            <Input
              ref={inputRef}
              value={text}
              onChange={(e) => setText(e.target.value.slice(0, MAX_LENGTH))}
              placeholder="따뜻한 한마디를 남겨 주세요"
              aria-label="댓글 입력"
              enterKeyHint="send"
              disabled={sending}
            />
            <SendButton type="submit" disabled={!trimmed || sending} aria-label="댓글 등록">
              <ArrowUp size={16} strokeWidth={2.6} />
            </SendButton>
          </InputWrap>
        </Form>
      ) : (
        <LoginPrompt type="button" onClick={() => openLogin()}>
          로그인하고 댓글 남기기
        </LoginPrompt>
      )}
      {error && <SendError role="alert">{error}</SendError>}
    </Panel>
  );
}

const LoginPrompt = styled.button`
  width: 100%;
  height: 40px;
  border: 1px dashed ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.surface};
`;

const SendError = styled.p`
  margin-top: ${({ theme }) => theme.space(2)};
  font-size: 12.5px;
  color: ${({ theme }) => theme.colors.danger};
`;

const slideDown = keyframes`
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
`;

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
`;

const Panel = styled.div`
  margin: ${({ theme }) => `${theme.space(1)} -${theme.space(2)} ${theme.space(2)}`};
  padding: ${({ theme }) => theme.space(3)};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.backgroundAlt};
  animation: ${slideDown} 200ms ease-out;
`;

const MoreLabel = styled.p`
  margin: 0 0 ${({ theme }) => theme.space(2)} 4px;
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
  max-height: 240px;
  margin-bottom: ${({ theme }) => theme.space(3)};
  overflow-y: auto;
`;

const Comment = styled.li`
  display: flex;
  gap: ${({ theme }) => theme.space(2)};
  animation: ${fadeIn} 250ms ease-out;
`;

const SmallAvatar = styled.span`
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const Body = styled.div`
  min-width: 0;
`;

const CommentMeta = styled.p`
  display: flex;
  gap: 6px;
  font-size: 12px;

  strong {
    font-weight: 700;
  }

  span {
    color: ${({ theme }) => theme.colors.textTertiary};
  }
`;

const CommentText = styled.p`
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
`;

const Form = styled.form`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(2)};
`;

const InputWrap = styled.div`
  position: relative;
  flex: 1;
`;

const Input = styled.input`
  width: 100%;
  height: 40px;
  padding: 0 44px 0 14px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.pill};
  /* iOS 는 16px 미만 입력란에 포커스하면 화면을 확대하므로 16px 유지 */
  font: inherit;
  font-size: 16px;
  color: ${({ theme }) => theme.colors.textPrimary};
  background: ${({ theme }) => theme.colors.surface};
  outline: none;
  transition: border-color ${({ theme }) => theme.motion.fast};

  &::placeholder {
    color: ${({ theme }) => theme.colors.textTertiary};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.accent};
  }
`;

const SendButton = styled.button`
  position: absolute;
  top: 50%;
  right: 5px;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.onAccent};
  background: ${({ theme }) => theme.colors.accent};
  transform: translateY(-50%);
  transition:
    opacity ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:disabled {
    opacity: 0.25;
    cursor: default;
  }

  &:active:not(:disabled) {
    transform: translateY(-50%) scale(0.88);
  }
`;
