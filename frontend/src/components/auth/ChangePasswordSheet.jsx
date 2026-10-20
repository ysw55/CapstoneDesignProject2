import React, { useEffect, useState } from 'react';
import styled, { keyframes } from 'styled-components';
import { AlertCircle, Loader2 } from 'lucide-react';
import { BottomSheet } from '../common/Modal';
import Button from '../common/Button';
import { Field, Input, Label } from '../form/FormScreen';
import { useAuth } from '../../context/AuthContext';

// 서버(PasswordChangeRequestDto)와 같은 규칙
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;
const EMPTY = { current: '', next: '', confirm: '' };

/**
 * 비밀번호 변경 바텀시트 (POST /api/auth/password)
 * 현재 비밀번호로 본인 확인 → 새 비밀번호 규칙 확인 → 두 번 입력 일치 확인
 */
export default function ChangePasswordSheet({ open, onClose, onChanged }) {
  const { changePassword } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setForm(EMPTY);
    setTouched({});
    setServerError(null);
  }, [open]);

  const errors = {};
  if (!form.current) errors.current = '현재 비밀번호를 입력해 주세요.';
  if (!PASSWORD_RE.test(form.next)) errors.next = '영문과 숫자를 섞어 8자 이상 입력해 주세요.';
  else if (form.next === form.current) errors.next = '현재 비밀번호와 다른 비밀번호를 입력해 주세요.';
  if (form.confirm !== form.next) errors.confirm = '새 비밀번호가 서로 달라요.';
  const isValid = Object.keys(errors).length === 0;

  const update = (field) => (e) => {
    setForm((f) => ({ ...f, [field]: e.target.value }));
    setServerError(null);
  };
  const blur = (field) => () => setTouched((t) => ({ ...t, [field]: true }));
  const fieldError = (field) => {
    const serverField = { current: 'currentPassword', next: 'newPassword' }[field];
    return (serverField && serverError?.fieldMessage?.(serverField)) || (touched[field] ? errors[field] : undefined);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched({ current: true, next: true, confirm: true });
    if (!isValid || submitting) return;

    setSubmitting(true);
    try {
      await changePassword(form.current, form.next);
      onClose();
      onChanged?.();
    } catch (err) {
      setServerError(err);
    } finally {
      setSubmitting(false);
    }
  };

  // '현재 비밀번호 불일치' 같은 서버 메시지는 상단 배너로
  const banner = serverError && !serverError.fieldErrors?.length ? serverError.message : null;

  return (
    <BottomSheet open={open} onClose={onClose} title="비밀번호 변경">
      <Form onSubmit={handleSubmit} noValidate>
        {banner && (
          <ErrorBanner role="alert">
            <AlertCircle size={16} strokeWidth={2.2} aria-hidden />
            {banner}
          </ErrorBanner>
        )}

        <Field>
          <Label htmlFor="pw-current">현재 비밀번호</Label>
          <Input
            id="pw-current"
            type="password"
            autoComplete="current-password"
            value={form.current}
            onChange={update('current')}
            onBlur={blur('current')}
            aria-invalid={Boolean(fieldError('current'))}
          />
          {fieldError('current') && <FieldError>{fieldError('current')}</FieldError>}
        </Field>

        <Field>
          <Label htmlFor="pw-next">새 비밀번호</Label>
          <Input
            id="pw-next"
            type="password"
            autoComplete="new-password"
            placeholder="영문 + 숫자 8자 이상"
            maxLength={64}
            value={form.next}
            onChange={update('next')}
            onBlur={blur('next')}
            aria-invalid={Boolean(fieldError('next'))}
          />
          {fieldError('next') && <FieldError>{fieldError('next')}</FieldError>}
        </Field>

        <Field>
          <Label htmlFor="pw-confirm">새 비밀번호 확인</Label>
          <Input
            id="pw-confirm"
            type="password"
            autoComplete="new-password"
            maxLength={64}
            value={form.confirm}
            onChange={update('confirm')}
            onBlur={blur('confirm')}
            aria-invalid={Boolean(fieldError('confirm'))}
          />
          {fieldError('confirm') && <FieldError>{fieldError('confirm')}</FieldError>}
        </Field>

        <Button type="submit" $variant="primary" $block disabled={submitting}>
          {submitting ? (
            <>
              <Spinner size={18} strokeWidth={2.4} aria-hidden />
              변경 중…
            </>
          ) : (
            '비밀번호 변경'
          )}
        </Button>
      </Form>
    </BottomSheet>
  );
}

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

const Spinner = styled(Loader2)`
  animation: ${spin} 0.8s linear infinite;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(4)};
`;

const ErrorBanner = styled.p`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};

  svg {
    flex-shrink: 0;
  }
`;

const FieldError = styled.p`
  font-size: 12.5px;
  color: ${({ theme }) => theme.colors.danger};
`;
