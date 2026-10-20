import React, { useEffect, useState } from 'react';
import styled, { keyframes } from 'styled-components';
import { AlertCircle, Eye, EyeOff, Loader2 } from 'lucide-react';
import { BottomSheet } from '../common/Modal';
import Button from '../common/Button';
import { Field, Input, Label, SegmentButton, Segmented } from '../form/FormScreen';
import { useAuth } from '../../context/AuthContext';
import SocialLoginButtons from './SocialLoginButtons';

const LOGIN = 'login';
const SIGNUP = 'signup';
const EMPTY = { email: '', password: '', username: '' };

// 서버(SignupRequestDto)와 같은 규칙 — 제출 전에 미리 알려 주기 위한 것이고, 최종 판단은 서버가 한다
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;

function validate(mode, form) {
  const errors = {};
  if (!EMAIL_RE.test(form.email.trim())) errors.email = '이메일 형식이 올바르지 않아요.';
  if (mode === SIGNUP) {
    if (!PASSWORD_RE.test(form.password)) errors.password = '영문과 숫자를 섞어 8자 이상 입력해 주세요.';
    const name = form.username.trim();
    if (name.length < 2 || name.length > 20) errors.username = '닉네임은 2~20자로 입력해 주세요.';
  } else if (!form.password) {
    errors.password = '비밀번호를 입력해 주세요.';
  }
  return errors;
}

/**
 * 로그인 / 회원가입 바텀시트
 * 성공하면 토큰이 저장되고(AuthContext) 시트가 닫힌다.
 */
export default function AuthSheet({ open, onClose, initialMode = LOGIN, onSuccess }) {
  const { login, signup } = useAuth();
  const [mode, setMode] = useState(initialMode);
  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null); // ApiError

  // 열 때마다 깨끗한 상태로
  useEffect(() => {
    if (!open) return;
    setMode(initialMode);
    setForm(EMPTY);
    setTouched({});
    setServerError(null);
    setShowPassword(false);
  }, [open, initialMode]);

  const errors = validate(mode, form);
  const isValid = Object.keys(errors).length === 0;

  const update = (field) => (e) => {
    setForm((f) => ({ ...f, [field]: e.target.value }));
    setServerError(null);
  };
  const blur = (field) => () => setTouched((t) => ({ ...t, [field]: true }));

  // 입력란별 에러: 서버 필드 에러 우선, 그다음 (한 번 건드린 칸만) 클라이언트 검증
  const fieldError = (field) => serverError?.fieldMessage?.(field) ?? (touched[field] ? errors[field] : undefined);

  const switchMode = (next) => {
    setMode(next);
    setServerError(null);
    setTouched({});
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched({ email: true, password: true, username: true });
    if (!isValid || submitting) return;

    setSubmitting(true);
    setServerError(null);
    try {
      const user =
        mode === LOGIN
          ? await login(form.email.trim(), form.password)
          : await signup(form.email.trim(), form.password, form.username.trim());
      onClose();
      onSuccess?.(user, mode);
    } catch (err) {
      setServerError(err);
    } finally {
      setSubmitting(false);
    }
  };

  // 필드 에러가 아닌 서버 메시지(로그인 실패, 중복 이메일, 네트워크 오류 등)
  const bannerMessage = serverError && !serverError.fieldErrors?.length ? serverError.message : null;

  return (
    <BottomSheet open={open} onClose={onClose} title={mode === LOGIN ? '다시 만나서 반가워요' : 'Book Canvas 시작하기'}>
      <Form onSubmit={handleSubmit} noValidate>
        <Segmented role="tablist" aria-label="로그인 또는 회원가입">
          <SegmentButton type="button" role="tab" aria-selected={mode === LOGIN} $active={mode === LOGIN} onClick={() => switchMode(LOGIN)}>
            로그인
          </SegmentButton>
          <SegmentButton type="button" role="tab" aria-selected={mode === SIGNUP} $active={mode === SIGNUP} onClick={() => switchMode(SIGNUP)}>
            회원가입
          </SegmentButton>
        </Segmented>

        {bannerMessage && (
          <ErrorBanner role="alert">
            <AlertCircle size={16} strokeWidth={2.2} aria-hidden />
            {bannerMessage}
          </ErrorBanner>
        )}

        <Field>
          <Label htmlFor="auth-email">이메일</Label>
          <Input
            id="auth-email"
            type="email"
            inputMode="email"
            autoComplete="email"
            autoCapitalize="none"
            placeholder="reader@bookcanvas.com"
            value={form.email}
            onChange={update('email')}
            onBlur={blur('email')}
            aria-invalid={Boolean(fieldError('email'))}
          />
          {fieldError('email') && <FieldError>{fieldError('email')}</FieldError>}
        </Field>

        {mode === SIGNUP && (
          <Field>
            <Label htmlFor="auth-username">닉네임</Label>
            <Input
              id="auth-username"
              autoComplete="nickname"
              placeholder="커뮤니티에 표시될 이름 (2~20자)"
              maxLength={20}
              value={form.username}
              onChange={update('username')}
              onBlur={blur('username')}
              aria-invalid={Boolean(fieldError('username'))}
            />
            {fieldError('username') && <FieldError>{fieldError('username')}</FieldError>}
          </Field>
        )}

        <Field>
          <Label htmlFor="auth-password">비밀번호</Label>
          <PasswordWrap>
            <Input
              id="auth-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete={mode === LOGIN ? 'current-password' : 'new-password'}
              placeholder={mode === SIGNUP ? '영문 + 숫자 8자 이상' : '비밀번호'}
              maxLength={64}
              value={form.password}
              onChange={update('password')}
              onBlur={blur('password')}
              aria-invalid={Boolean(fieldError('password'))}
            />
            <RevealButton
              type="button"
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
              onClick={() => setShowPassword((v) => !v)}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </RevealButton>
          </PasswordWrap>
          {fieldError('password') && <FieldError>{fieldError('password')}</FieldError>}
        </Field>

        <Button type="submit" $variant="primary" $block disabled={submitting}>
          {submitting ? (
            <>
              <Spinner size={18} strokeWidth={2.4} aria-hidden />
              {mode === LOGIN ? '로그인 중…' : '가입 중…'}
            </>
          ) : mode === LOGIN ? (
            '로그인'
          ) : (
            '가입하고 시작하기'
          )}
        </Button>

        <OrDivider>또는</OrDivider>
        <SocialLoginButtons
          onSuccess={(user) => {
            onClose();
            onSuccess?.(user, 'social');
          }}
        />
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

const OrDivider = styled.p`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: ${({ theme }) => theme.colors.border};
  }
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

const PasswordWrap = styled.div`
  position: relative;

  input {
    padding-right: 52px;
  }
`;

const RevealButton = styled.button`
  position: absolute;
  top: 50%;
  right: 6px;
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.textTertiary};
  transform: translateY(-50%);

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;
