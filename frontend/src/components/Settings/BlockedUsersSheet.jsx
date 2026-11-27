import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { BottomSheet } from '../common/Modal';
import ProfileAvatar from '../common/ProfileAvatar';
import { useAppData } from '../../context/AppDataContext';
import * as moderationApi from '../../api/moderation';

/** 설정 > 차단한 사용자: 목록을 보고 차단을 해제한다 */
export default function BlockedUsersSheet({ open, onClose }) {
  const { unblockUser } = useAppData();
  const [users, setUsers] = useState(null); // null 이면 불러오는 중
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return undefined;
    let cancelled = false;
    setUsers(null);
    setError(null);
    moderationApi
      .fetchBlockedUsers()
      .then((list) => !cancelled && setUsers(list))
      .catch((err) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, [open]);

  const handleUnblock = async (userId) => {
    setError(null);
    try {
      await unblockUser(userId);
      setUsers((prev) => prev.filter((u) => u.userId !== userId));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <BottomSheet open={open} onClose={onClose} title="차단한 사용자">
      {error && <Message role="alert">{error}</Message>}
      {users === null && !error && <Message>불러오는 중…</Message>}
      {users?.length === 0 && <Message>차단한 사용자가 없어요</Message>}
      {users?.map((u) => (
        <UserRow key={u.userId}>
          <ProfileAvatar
            name={u.username}
            background={u.profileBackgroundColor}
            border={u.profileBorderColor}
            image={u.profileImage}
            frame={u.profileFrame}
            size={36}
          />
          <strong>{u.username}</strong>
          <UnblockButton type="button" onClick={() => handleUnblock(u.userId)}>
            차단 해제
          </UnblockButton>
        </UserRow>
      ))}
    </BottomSheet>
  );
}

const Message = styled.p`
  padding: ${({ theme }) => theme.space(6)} 0;
  text-align: center;
  font-size: 14px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const UserRow = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  padding: ${({ theme }) => theme.space(2)} 0;

  strong {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
  }
`;

const UnblockButton = styled.button`
  height: 36px;
  padding: 0 14px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 700;
  background: ${({ theme }) => theme.colors.surfaceMuted};
`;
