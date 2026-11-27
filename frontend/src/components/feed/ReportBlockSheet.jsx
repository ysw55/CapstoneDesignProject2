import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { Ban, ChevronRight, Flag } from 'lucide-react';
import { BottomSheet, ConfirmDialog } from '../common/Modal';
import { useAppData } from '../../context/AppDataContext';

/** 서버 domain/moderation/ReportReason 과 코드를 맞춘다 */
const REASONS = [
  { code: 'SPAM', label: '스팸 · 광고' },
  { code: 'ABUSE', label: '욕설 · 비방 · 혐오 표현' },
  { code: 'ADULT', label: '음란하거나 불쾌한 내용' },
  { code: 'COPYRIGHT', label: '저작권 침해' },
  { code: 'OTHER', label: '기타' },
];

/**
 * 다른 사람 글의 점 3개를 눌렀을 때: 글 신고 / 작성자 차단
 * post 가 null 이면 닫힘. 처리 결과는 onDone(안내 문구) 으로 알린다.
 */
export default function ReportBlockSheet({ post, onClose, onDone }) {
  const { reportPost, blockUser } = useAppData();
  const [step, setStep] = useState('menu'); // 'menu' | 'report' | 'block'
  const [sending, setSending] = useState(false);

  useEffect(() => setStep('menu'), [post?.id]);

  const close = () => {
    setStep('menu');
    onClose();
  };

  const run = async (task, message) => {
    if (sending) return;
    setSending(true);
    try {
      await task();
      onDone(message);
    } catch (err) {
      onDone(err.message);
    } finally {
      setSending(false);
      close();
    }
  };

  return (
    <>
      <BottomSheet
        open={Boolean(post) && step !== 'block'}
        onClose={close}
        title={step === 'report' ? '신고하는 이유를 알려 주세요' : `${post?.userName ?? ''}님의 글`}
      >
        <List>
          {step === 'report' ? (
            REASONS.map((reason) => (
              <li key={reason.code}>
                <Item
                  type="button"
                  disabled={sending}
                  onClick={() => run(() => reportPost(post.id, reason.code), '신고했어요. 이 글은 더 보이지 않아요')}
                >
                  <span>{reason.label}</span>
                  <ChevronRight size={18} strokeWidth={2} aria-hidden />
                </Item>
              </li>
            ))
          ) : (
            <>
              <li>
                <Item type="button" onClick={() => setStep('report')}>
                  <Flag size={18} strokeWidth={2} aria-hidden />
                  <span>글 신고하기</span>
                </Item>
              </li>
              <li>
                <Item type="button" $danger onClick={() => setStep('block')}>
                  <Ban size={18} strokeWidth={2} aria-hidden />
                  <span>이 사용자 차단하기</span>
                </Item>
              </li>
            </>
          )}
        </List>
      </BottomSheet>

      <ConfirmDialog
        open={Boolean(post) && step === 'block'}
        title={`${post?.userName ?? ''}님을 차단할까요?`}
        message={'이 사용자의 글과 댓글이 보이지 않아요.\n설정에서 언제든 해제할 수 있어요.'}
        confirmLabel="차단"
        onConfirm={() => run(() => blockUser(post.userId), `${post.userName}님을 차단했어요`)}
        onCancel={close}
      />
    </>
  );
}

const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
`;

const Item = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  width: 100%;
  height: 52px;
  padding: 0 ${({ theme }) => theme.space(4)};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 600;
  text-align: left;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.textPrimary)};
  background: ${({ theme }) => theme.colors.surfaceMuted};

  span {
    flex: 1;
  }

  &:disabled {
    opacity: 0.5;
  }
`;
