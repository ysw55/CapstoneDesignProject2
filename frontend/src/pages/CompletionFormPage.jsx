import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { AlertCircle, BadgeCheck, Coins, Palette } from 'lucide-react';
import FormScreen, { Field, Input, Label, LabelRow, Textarea } from '../components/form/FormScreen';
import BookPicker, { initialBookPick, resolveBookPick } from '../components/form/BookPicker';
import LoadingScreen from '../components/common/LoadingScreen';
import { pickCoverColor, useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import { MAX_REVIEW_LENGTH, MIN_REVIEW_LENGTH, certifyCompletion } from '../api/books';
import { todayISO } from '../utils/format';

const DRAFT_KEY = 'bookcanvas:reviewDraft';

const loadDraft = () => {
  try {
    return sessionStorage.getItem(DRAFT_KEY) ?? '';
  } catch {
    return '';
  }
};
const saveDraft = (text) => {
  try {
    if (text) sessionStorage.setItem(DRAFT_KEY, text);
    else sessionStorage.removeItem(DRAFT_KEY);
  } catch {
    /* noop */
  }
};

/**
 * 완독 인증 (/books/certify, 책장에서 들어오면 ?bookId=) — 로그인 필수(RequireAuth)
 * 독후감 200자 이상 → 서버 POST /api/books/completion → 보상 스킨·포인트 지급 → 서버 책장 다시 불러오기
 */
export default function CompletionFormPage() {
  const { shelfStatus } = useAppData();
  // 책장(인증 여부 포함)을 불러온 뒤에 폼을 연다
  if (shelfStatus === 'loading') return <LoadingScreen />;
  return <CompletionForm />;
}

function CompletionForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { books, afterCertified } = useAppData();
  const { applyCompletionReward } = useAuth();

  const [form, setForm] = useState(() => {
    // 책장의 특정 책에서 '완독 인증'으로 들어온 경우 그 책을 미리 선택
    const preset = books.find((b) => String(b.id) === searchParams.get('bookId') && !b.certified);
    return {
      ...initialBookPick(books),
      ...(preset && { bookId: preset.id }),
      completedAt: preset?.completedAt ?? todayISO(),
      reviewText: loadDraft(),
    };
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null); // ApiError

  // 작성 중인 독후감은 탭을 닫기 전까지 임시 저장 (실수로 닫아도 복구)
  useEffect(() => {
    saveDraft(form.reviewText);
  }, [form.reviewText]);

  const set = (patch) => {
    setError(null);
    setForm((f) => {
      const next = { ...f, ...patch };
      // 책장에서 책을 고르면 그 책의 완독 날짜를 가져온다
      if (patch.bookId) {
        const book = books.find((b) => b.id === patch.bookId);
        if (book?.completedAt) next.completedAt = book.completedAt;
      }
      return next;
    });
  };

  const { title, author } = resolveBookPick(books, form);
  const today = todayISO();

  // 서버와 같은 기준: 앞뒤 공백을 뺀 글자 수
  const reviewLength = form.reviewText.trim().length;
  const remaining = Math.max(0, MIN_REVIEW_LENGTH - reviewLength);
  const reviewOk = reviewLength >= MIN_REVIEW_LENGTH;
  const progress = Math.min(1, reviewLength / MIN_REVIEW_LENGTH);

  const canSubmit = title && author && form.completedAt && form.completedAt <= today && reviewOk;

  const handleSubmit = async () => {
    setSaving(true);
    setError(null);
    const wasOnShelf = books.some((b) => b.title === title && b.author === author);
    try {
      const result = await certifyCompletion({
        title,
        author,
        completedAt: form.completedAt,
        reviewText: form.reviewText.trim(),
        coverColor: pickCoverColor(books), // 책장에 없던 책을 새로 꽂을 때의 표지 색
      });

      applyCompletionReward(result.reward);
      saveDraft('');
      // 서버 책장을 다시 불러와 ✓ 배지 반영 (실패해도 인증 자체는 완료된 상태)
      await afterCertified(result.completionId, wasOnShelf).catch(() => {});

      navigate('/bookshelf', {
        replace: true,
        state: { toast: `완독 인증 완료! +${result.reward.earnedPoint}P와 새 스킨을 받았어요` },
      });
    } catch (err) {
      setError(err);
      setSaving(false);
    }
  };

  const hint = !title || !author
    ? '인증할 책을 선택해 주세요'
    : !reviewOk
      ? `독후감을 ${remaining}자 더 작성해 주세요`
      : '완독 날짜를 확인해 주세요';

  return (
    <>
      <FormScreen
        title="완독 인증"
        submitLabel="인증하고 보상 받기"
        onSubmit={handleSubmit}
        canSubmit={canSubmit}
        saving={saving}
        hint={hint}
        fallbackPath="/bookshelf"
        errorMessage={error?.message}
      >
        <RewardCard>
          <RewardItem>
            <Coins size={18} strokeWidth={2} aria-hidden />
            <span>
              <strong>+100P</strong> 포인트
            </span>
          </RewardItem>
          <RewardItem>
            <Palette size={18} strokeWidth={2} aria-hidden />
            <span>
              <strong>프로필 스킨</strong> 지급
            </span>
          </RewardItem>
        </RewardCard>

        <BookPicker
          books={books}
          value={form}
          onChange={set}
          label="어떤 책을 다 읽었나요?"
          isDisabled={(book) => Boolean(book.certified)}
        />

        <Field>
          <Label htmlFor="completedAt">다 읽은 날짜</Label>
          <Input
            id="completedAt"
            type="date"
            value={form.completedAt}
            max={today}
            onChange={(e) => set({ completedAt: e.target.value })}
          />
        </Field>

        <Field>
          <LabelRow>
            <Label htmlFor="reviewText">독후감</Label>
            <ReviewCounter $ok={reviewOk} aria-live="polite">
              <strong>{reviewLength.toLocaleString('ko-KR')}</strong> / {MIN_REVIEW_LENGTH}자
            </ReviewCounter>
          </LabelRow>
          <Textarea
            id="reviewText"
            value={form.reviewText}
            onChange={(e) => set({ reviewText: e.target.value })}
            placeholder={'책을 읽으며 느낀 점, 기억에 남는 장면, 나에게 남은 변화를\n200자 이상 자유롭게 적어 주세요.'}
            maxLength={MAX_REVIEW_LENGTH}
            rows={9}
            aria-describedby="review-status"
            aria-invalid={reviewLength > 0 && !reviewOk}
          />
          <ProgressTrack aria-hidden>
            <ProgressBar style={{ transform: `scaleX(${progress})` }} $ok={reviewOk} />
          </ProgressTrack>
          <ReviewStatus id="review-status" $ok={reviewOk}>
            {reviewOk ? (
              <>
                <BadgeCheck size={14} strokeWidth={2.4} aria-hidden />
                인증 조건을 채웠어요
              </>
            ) : (
              <>
                <AlertCircle size={14} strokeWidth={2.4} aria-hidden />
                200자 이상 작성해야 인증할 수 있어요 · {remaining}자 남음
              </>
            )}
          </ReviewStatus>
        </Field>

      </FormScreen>
    </>
  );
}

const RewardCard = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.space(2)};
`;

const RewardItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 13px;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.backgroundAlt};

  svg {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.accent};
  }

  strong {
    font-weight: 800;
    color: ${({ theme }) => theme.colors.textPrimary};
  }
`;

const ReviewCounter = styled.span`
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: ${({ theme }) => theme.colors.textTertiary};

  strong {
    font-weight: 800;
    color: ${({ $ok, theme }) => ($ok ? theme.colors.accent : theme.colors.textPrimary)};
  }
`;

const ProgressTrack = styled.div`
  height: 4px;
  overflow: hidden;
  border-radius: ${({ theme }) => theme.radius.pill};
  background: ${({ theme }) => theme.colors.surfaceMuted};
`;

const ProgressBar = styled.div`
  height: 100%;
  border-radius: inherit;
  background: ${({ $ok, theme }) => ($ok ? theme.colors.accent : theme.colors.textTertiary)};
  transform-origin: left;
  transition:
    transform 200ms ease-out,
    background ${({ theme }) => theme.motion.normal};
`;

const ReviewStatus = styled.p`
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 600;
  color: ${({ $ok, theme }) => ($ok ? theme.colors.accent : theme.colors.danger)};
`;
