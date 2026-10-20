import React from 'react';
import styled from 'styled-components';
import { Crown, Heart, ThumbsUp } from 'lucide-react';
import ScreenHeader from '../components/common/ScreenHeader';
import BookCover from '../components/book/BookCover';
import { monthlyBestBooks, weeklyBestQuotes } from '../mockData';
import { formatCount } from '../utils/format';

export default function RankingPage() {
  const [champion, ...runnersUp] = monthlyBestBooks;

  return (
    <>
      <ScreenHeader title="랭킹" subtitle="이번 달 가장 많이 사랑받은 책과 문장" />

      {/* 1. 명예의 전당 */}
      <Section>
        <SectionHead>
          <SectionTitle>이달의 명예의 전당</SectionTitle>
          <SectionMeta>추천 수 기준</SectionMeta>
        </SectionHead>

        <ChampionCard>
          <BookCover title={champion.title} author={champion.author} color="#F8F9FA" size="lg" />
          <ChampionInfo>
            <ChampionBadge>
              <Crown size={14} strokeWidth={2.4} fill="currentColor" aria-hidden />
              1위
            </ChampionBadge>
            <ChampionTitle>{champion.title}</ChampionTitle>
            <ChampionAuthor>{champion.author}</ChampionAuthor>
            <ChampionCount>
              <ThumbsUp size={14} strokeWidth={2.2} aria-hidden />
              {formatCount(champion.recommendCount)}명이 추천했어요
            </ChampionCount>
          </ChampionInfo>
        </ChampionCard>

        <List>
          {runnersUp.map((book) => (
            <BookRow key={book.rank}>
              <RankNumber>{book.rank}</RankNumber>
              <BookCover title={book.title} color={book.coverColor} size="sm" />
              <RowText>
                <RowTitle>{book.title}</RowTitle>
                <RowSub>{book.author}</RowSub>
              </RowText>
              <Count>
                <ThumbsUp size={14} strokeWidth={2.2} aria-hidden />
                {formatCount(book.recommendCount)}
              </Count>
            </BookRow>
          ))}
        </List>
      </Section>

      {/* 2. 주간 베스트 문장 */}
      <Section>
        <SectionHead>
          <SectionTitle>주간 베스트 문장</SectionTitle>
          <SectionMeta>좋아요 수 기준</SectionMeta>
        </SectionHead>

        <QuoteList>
          {weeklyBestQuotes.map((item) => (
            <QuoteItem key={item.rank} $first={item.rank === 1}>
              <QuoteRank $first={item.rank === 1}>
                {item.rank === 1 && <Crown size={16} strokeWidth={2.4} fill="currentColor" aria-hidden />}
                {String(item.rank).padStart(2, '0')}
              </QuoteRank>
              <QuoteText>{item.quote}</QuoteText>
              <QuoteFooter>
                <span>
                  <strong>{item.bookTitle}</strong> · {item.userName}
                </span>
                <Count>
                  <Heart size={14} strokeWidth={2.2} fill="currentColor" aria-hidden />
                  {formatCount(item.likeCount)}
                </Count>
              </QuoteFooter>
            </QuoteItem>
          ))}
        </QuoteList>
      </Section>
    </>
  );
}

const Section = styled.section`
  padding: 0 ${({ theme }) => theme.layout.gutter};
  margin-bottom: ${({ theme }) => theme.space(10)};
`;

const SectionHead = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: ${({ theme }) => theme.space(3)};
`;

const SectionTitle = styled.h2`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const SectionMeta = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

/* 1위: 딥그린 배경의 하이라이트 카드 */
const ChampionCard = styled.article`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(5)};
  padding: ${({ theme }) => theme.space(5)};
  border-radius: ${({ theme }) => theme.radius.lg};
  color: ${({ theme }) => theme.colors.onAccent};
  background:
    radial-gradient(circle at 100% 0%, rgba(255, 255, 255, 0.12), transparent 50%),
    ${({ theme }) => theme.colors.accent};
`;

const ChampionInfo = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 0;
`;

const ChampionBadge = styled.span`
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 4px;
  padding: 4px 10px 4px 8px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 12px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.onAccent};
`;

const ChampionTitle = styled.h3`
  margin-top: ${({ theme }) => theme.space(3)};
  font-size: ${({ theme }) => theme.fontSize.xl};
  font-weight: 800;
  letter-spacing: -0.04em;
`;

const ChampionAuthor = styled.p`
  font-size: ${({ theme }) => theme.fontSize.sm};
  opacity: 0.75;
`;

const ChampionCount = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: ${({ theme }) => theme.space(4)};
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 600;
`;

const List = styled.ol`
  margin-top: ${({ theme }) => theme.space(2)};
`;

const BookRow = styled.li`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(4)};
  padding: ${({ theme }) => `${theme.space(3)} ${theme.space(1)}`};

  & + & {
    border-top: 1px solid ${({ theme }) => theme.colors.border};
  }
`;

const RankNumber = styled.span`
  width: 20px;
  text-align: center;
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 800;
  color: ${({ theme }) => theme.colors.accent};
  font-variant-numeric: tabular-nums;
`;

const RowText = styled.div`
  flex: 1;
  min-width: 0;
`;

const RowTitle = styled.p`
  font-weight: 700;
  letter-spacing: -0.02em;
`;

const RowSub = styled.p`
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.textSecondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Count = styled.span`
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 4px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
  font-variant-numeric: tabular-nums;
`;

const QuoteList = styled.ol`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
`;

const QuoteItem = styled.li`
  padding: ${({ theme }) => theme.space(5)};
  border: 1px solid ${({ $first, theme }) => ($first ? theme.colors.accent : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ $first, theme }) => ($first ? theme.colors.accentSoft : theme.colors.surface)};
`;

const QuoteRank = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 800;
  letter-spacing: 0.02em;
  color: ${({ $first, theme }) => ($first ? theme.colors.accent : theme.colors.textTertiary)};
`;

const QuoteText = styled.p`
  margin: ${({ theme }) => `${theme.space(2)} 0 ${theme.space(4)}`};
  font-family: ${({ theme }) => theme.font.serif};
  font-size: 16px;
  line-height: 1.7;
  letter-spacing: -0.02em;
  word-break: keep-all;
`;

const QuoteFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.space(3)};
  font-size: 12.5px;
  color: ${({ theme }) => theme.colors.textSecondary};

  > span:first-child {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: ${({ theme }) => theme.colors.textPrimary};
  }
`;
