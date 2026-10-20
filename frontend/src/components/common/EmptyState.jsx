import React from 'react';
import styled from 'styled-components';

/** 아직 콘텐츠가 없는 화면에 쓰는 안내 블록. action 에 버튼을 넘기면 아래에 표시된다. */
export default function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <Wrap>
      {Icon && (
        <IconCircle>
          <Icon size={28} strokeWidth={1.6} aria-hidden />
        </IconCircle>
      )}
      <Title>{title}</Title>
      {description && <Description>{description}</Description>}
      {action && <Action>{action}</Action>}
    </Wrap>
  );
}

const Wrap = styled.section`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: ${({ theme }) => `${theme.space(20)} ${theme.layout.gutter}`};
`;

const IconCircle = styled.div`
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: ${({ theme }) => theme.space(4)};
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const Title = styled.h2`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const Description = styled.p`
  margin-top: ${({ theme }) => theme.space(2)};
  max-width: 260px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  line-height: 1.6;
  color: ${({ theme }) => theme.colors.textSecondary};
  white-space: pre-line;
`;

const Action = styled.div`
  margin-top: ${({ theme }) => theme.space(6)};
`;
