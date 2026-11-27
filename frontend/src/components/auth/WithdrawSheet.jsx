import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { BottomSheet } from '../common/Modal';
import { Field, Input, Label } from '../form/FormScreen';
import { useAuth } from '../../context/AuthContext';

/**
 * 회원탈퇴 바텀시트 (POST /api/auth/withdraw)
 * 지워지는 것을 알리고 동의를 받는다. 이메일 계정은 비밀번호로 본인 확인.
 */
export default function WithdrawSheet({ open, onClose, onWithdrawn }) {
  const { withdraw, isSocialAccount } = useAuth();
  const [password, setPassword] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setPassword('');
    setAgreed(false);
    setError(null);
  }, [open]);

  const canSubmit = agreed && (isSocialAccount || password) && !submitting;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      await withdraw(isSocialAccount ? null : password);
      onWithdrawn();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <BottomSheet open={open} onClose={onClose} title="회원탈퇴">
      <Form onSubmit={handleSubmit} noValidate>
        <Notice>
          탈퇴하면 아래 정보가 모두 삭제되고 <strong>되돌릴 수 없어요.</strong>
          <ul>
            <li>계정과 프로필</li>
            <li>작성한 문장과 댓글, 좋아요 · 저장 기록</li>
            <li>책장, 완독 인증과 독후감</li>
            <li>보상 포인트와 구매한 상점 상품</li>
          </ul>
        </Notice>

        {!isSocialAccount && (
          <Field>
            <Label htmlFor="withdraw-password">비밀번호 확인</Label>
            <Input
              id="withdraw-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setError(null);
              }}
              aria-invalid={Boolean(error)}
            />
          </Field>
        )}

        <Agree>
          <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
          안내를 확인했고 탈퇴에 동의해요
        </Agree>

        {error && <ErrorText role="alert">{error}</ErrorText>}

        <SubmitButton type="submit" disabled={!canSubmit}>
          {submitting ? '탈퇴하는 중…' : '탈퇴하기'}
        </SubmitButton>
      </Form>
    </BottomSheet>
  );
}

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(4)};
`;

const Notice = styled.div`
  padding: ${({ theme }) => theme.space(4)};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 14px;
  line-height: 1.6;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.dangerSoft};

  strong {
    color: ${({ theme }) => theme.colors.danger};
  }

  ul {
    margin-top: ${({ theme }) => theme.space(2)};
    padding-left: 18px;
    list-style: disc;
  }
`;

const Agree = styled.label`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(2)};
  font-size: 14px;
  font-weight: 600;

  input {
    width: 18px;
    height: 18px;
    accent-color: ${({ theme }) => theme.colors.danger};
  }
`;

const ErrorText = styled.p`
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.danger};
`;

const SubmitButton = styled.button`
  height: 52px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 700;
  color: #ffffff;
  background: ${({ theme }) => theme.colors.danger};

  &:disabled {
    opacity: 0.4;
  }
`;
