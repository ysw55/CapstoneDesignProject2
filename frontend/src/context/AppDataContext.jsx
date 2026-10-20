import React, { createContext, useCallback, useContext, useEffect, useMemo, useReducer, useRef } from 'react';
import * as quotesApi from '../api/quotes';
import * as booksApi from '../api/books';
import { useAuth } from './AuthContext';
import { COVER_PALETTE, tagFromName } from '../mockData';
import { relativeTime } from '../utils/format';

/**
 * 앱 전역 데이터 — 백엔드 DB 와 동기화
 * - posts : 커뮤니티 피드 (GET /api/quotes, 비로그인도 조회 가능)
 * - books : 로그인한 계정의 내 책장 (GET /api/books/completions)
 *
 * 로그인/로그아웃/계정 전환 시 자동으로 다시 불러온다.
 *   로그아웃 → 책장 비움, 피드는 liked/mine 없이 다시 조회
 *   다른 계정 로그인 → 그 계정의 책장과 좋아요 상태로 교체
 */

// 이전 버전에서 localStorage 에 저장하던 피드/책장 (이제 서버가 원본이므로 정리)
try {
  localStorage.removeItem('bookcanvas:data:v1');
} catch {
  /* noop */
}

/* ───────── 서버 응답 → 화면용 모양 ───────── */

const initialOf = (name) => name?.trim().charAt(0).toUpperCase() || '?';

export const mapQuote = (q) => ({
  id: q.quoteId,
  userName: q.author.username,
  userInitial: initialOf(q.author.username),
  userSkin: { background: q.author.profileBackgroundColor, border: q.author.profileBorderColor },
  tag: tagFromName(q.tags[0]?.name ?? ''),
  bookTitle: q.book.title,
  author: q.book.author,
  quote: q.content,
  likeCount: q.likeCount,
  commentCount: q.commentCount,
  liked: q.liked,
  mine: q.mine,
  edited: q.edited,
  createdAtRaw: q.createdAt,
  createdAt: relativeTime(q.createdAt),
  comments: null, // 댓글 창을 열 때 불러옴
});

const mapComment = (c) => ({
  id: c.commentId,
  userName: c.author.username,
  userInitial: initialOf(c.author.username),
  text: c.content,
  createdAt: relativeTime(c.createdAt),
});

export const mapShelfBook = (b) => ({
  id: b.completionId,
  title: b.book.title,
  author: b.book.author,
  completedAt: b.completedAt,
  coverColor: b.coverColor,
  certified: b.certified,
});

/** 내가 쓴 글인지 (서버가 JWT 기준으로 계산해 준 값) */
export const isMyPost = (post) => Boolean(post.mine);

// 팔레트를 순서대로 돌되, 책장 맨 앞 책과 같은 색은 피한다 (새 책 표지 색 기본값)
export function pickCoverColor(books) {
  let i = books.length % COVER_PALETTE.length;
  if (COVER_PALETTE[i] === books[0]?.coverColor) i = (i + 1) % COVER_PALETTE.length;
  return COVER_PALETTE[i];
}

/* ───────── 상태 ───────── */

const initialState = {
  posts: [],
  feedStatus: 'loading', // 'loading' | 'ready' | 'error'
  feedError: null,
  books: [],
  shelfStatus: 'guest', // 'guest' | 'loading' | 'ready' | 'error'
  shelfError: null,
  highlightPostId: null, // 방금 추가/수정한 항목을 잠깐 강조
  highlightBookId: null,
  highlightBookIsNew: false,
};

const patchPost = (posts, id, fn) => posts.map((p) => (p.id === id ? fn(p) : p));

function reducer(state, action) {
  switch (action.type) {
    case 'FEED_LOADING':
      return { ...state, feedStatus: state.posts.length ? state.feedStatus : 'loading', feedError: null };
    case 'FEED_LOADED':
      return { ...state, posts: action.posts, feedStatus: 'ready' };
    case 'FEED_ERROR':
      return { ...state, feedStatus: 'error', feedError: action.error };

    case 'POST_ADDED':
      return { ...state, posts: [action.post, ...state.posts], highlightPostId: action.post.id };
    case 'POST_UPDATED':
      return {
        ...state,
        // 이미 불러온 댓글은 유지
        posts: patchPost(state.posts, action.post.id, (p) => ({ ...action.post, comments: p.comments })),
        highlightPostId: action.post.id,
      };
    case 'POST_REMOVED':
      return { ...state, posts: state.posts.filter((p) => p.id !== action.id) };
    case 'POST_PATCH':
      return { ...state, posts: patchPost(state.posts, action.id, (p) => ({ ...p, ...action.patch })) };
    case 'COMMENTS_LOADED':
      return {
        ...state,
        posts: patchPost(state.posts, action.id, (p) => ({ ...p, comments: action.comments, commentCount: action.comments.length })),
      };
    case 'COMMENT_ADDED':
      return {
        ...state,
        posts: patchPost(state.posts, action.id, (p) => ({
          ...p,
          comments: [...(p.comments ?? []), action.comment],
          commentCount: p.commentCount + 1,
        })),
      };

    case 'SHELF_LOADING':
      return { ...state, shelfStatus: 'loading', shelfError: null };
    case 'SHELF_LOADED':
      return { ...state, books: action.books, shelfStatus: 'ready' };
    case 'SHELF_ERROR':
      return { ...state, shelfStatus: 'error', shelfError: action.error };
    case 'SHELF_RESET':
      return { ...state, books: [], shelfStatus: 'guest', shelfError: null, highlightBookId: null };
    case 'BOOK_UPSERT': {
      const exists = state.books.some((b) => b.id === action.book.id);
      return {
        ...state,
        books: exists ? state.books.map((b) => (b.id === action.book.id ? action.book : b)) : [action.book, ...state.books],
        highlightBookId: action.book.id,
        highlightBookIsNew: action.isNew,
      };
    }
    case 'BOOK_REMOVED':
      return { ...state, books: state.books.filter((b) => b.id !== action.id) };
    case 'HIGHLIGHT_BOOK':
      return { ...state, highlightBookId: action.id, highlightBookIsNew: action.isNew };

    case 'CLEAR_HIGHLIGHT':
      return { ...state, highlightPostId: null, highlightBookId: null, highlightBookIsNew: false };

    default:
      throw new Error(`알 수 없는 액션: ${action.type}`);
  }
}

const AppDataContext = createContext(null);

export function AppDataProvider({ children }) {
  const { user, status: authStatus } = useAuth();
  const [state, dispatch] = useReducer(reducer, initialState);
  const userId = user?.userId ?? null;

  // 계정이 빠르게 바뀔 때 늦게 도착한 이전 계정의 응답을 버리기 위한 요청 번호
  const feedSeq = useRef(0);
  const shelfSeq = useRef(0);

  const loadFeed = useCallback(async () => {
    const seq = ++feedSeq.current;
    dispatch({ type: 'FEED_LOADING' });
    try {
      const page = await quotesApi.fetchFeed();
      if (seq === feedSeq.current) dispatch({ type: 'FEED_LOADED', posts: page.content.map(mapQuote) });
    } catch (error) {
      if (seq === feedSeq.current) dispatch({ type: 'FEED_ERROR', error });
    }
  }, []);

  const loadShelf = useCallback(async () => {
    const seq = ++shelfSeq.current;
    dispatch({ type: 'SHELF_LOADING' });
    try {
      const shelf = await booksApi.fetchShelf();
      if (seq === shelfSeq.current) dispatch({ type: 'SHELF_LOADED', books: shelf.map(mapShelfBook) });
    } catch (error) {
      if (seq === shelfSeq.current) dispatch({ type: 'SHELF_ERROR', error });
    }
  }, []);

  // 로그인 상태가 확정되면(복원 중 제외) 계정 기준으로 피드·책장을 다시 불러옴
  useEffect(() => {
    if (authStatus === 'restoring') return;
    loadFeed(); // liked / mine 이 계정마다 다름

    if (userId) {
      loadShelf();
    } else {
      shelfSeq.current++; // 진행 중인 이전 계정 요청 무시
      dispatch({ type: 'SHELF_RESET' });
    }
  }, [authStatus, userId, loadFeed, loadShelf]);

  const actions = useMemo(
    () => ({
      reloadFeed: loadFeed,
      reloadShelf: loadShelf,

      /* ── 커뮤니티 ── */
      addPost: async ({ bookTitle, author, quote, tag }) => {
        const res = await quotesApi.createQuote({ bookTitle, author, content: quote, tagName: tag.label });
        dispatch({ type: 'POST_ADDED', post: mapQuote(res) });
      },
      updatePost: async (id, { bookTitle, author, quote, tag }) => {
        const res = await quotesApi.updateQuote(id, { bookTitle, author, content: quote, tagName: tag.label });
        dispatch({ type: 'POST_UPDATED', post: mapQuote(res) });
      },
      deletePost: async (id) => {
        await quotesApi.deleteQuote(id);
        dispatch({ type: 'POST_REMOVED', id });
      },

      /** 낙관적 업데이트: 화면을 먼저 바꾸고, 서버 응답의 실제 숫자로 맞춘다. 실패하면 되돌림 */
      toggleLike: async (post) => {
        const nextLiked = !post.liked;
        dispatch({
          type: 'POST_PATCH',
          id: post.id,
          patch: { liked: nextLiked, likeCount: post.likeCount + (nextLiked ? 1 : -1) },
        });
        try {
          const res = nextLiked ? await quotesApi.likeQuote(post.id) : await quotesApi.unlikeQuote(post.id);
          dispatch({ type: 'POST_PATCH', id: post.id, patch: { liked: res.liked, likeCount: res.likeCount } });
        } catch (error) {
          dispatch({ type: 'POST_PATCH', id: post.id, patch: { liked: post.liked, likeCount: post.likeCount } });
          throw error;
        }
      },

      loadComments: async (postId) => {
        const list = await quotesApi.fetchComments(postId);
        dispatch({ type: 'COMMENTS_LOADED', id: postId, comments: list.map(mapComment) });
      },
      addComment: async (postId, text) => {
        const res = await quotesApi.createComment(postId, text);
        dispatch({ type: 'COMMENT_ADDED', id: postId, comment: mapComment(res) });
      },

      /* ── 내 책장 ── */
      addBook: async (book) => {
        const res = await booksApi.addShelfBook(book);
        dispatch({ type: 'BOOK_UPSERT', book: mapShelfBook(res), isNew: true });
      },
      updateBook: async (id, book) => {
        const res = await booksApi.updateShelfBook(id, book);
        dispatch({ type: 'BOOK_UPSERT', book: mapShelfBook(res), isNew: false });
      },
      deleteBook: async (id) => {
        await booksApi.deleteShelfBook(id);
        dispatch({ type: 'BOOK_REMOVED', id });
      },
      /** 완독 인증 후: 서버 책장을 다시 불러와 ✓ 배지를 반영하고 그 책을 강조 */
      afterCertified: async (completionId, wasOnShelf) => {
        await loadShelf();
        dispatch({ type: 'HIGHLIGHT_BOOK', id: completionId, isNew: !wasOnShelf });
      },

      clearHighlight: () => dispatch({ type: 'CLEAR_HIGHLIGHT' }),
    }),
    [loadFeed, loadShelf]
  );

  const value = useMemo(() => ({ ...state, ...actions }), [state, actions]);

  return <AppDataContext.Provider value={value}>{children}</AppDataContext.Provider>;
}

export function useAppData() {
  const ctx = useContext(AppDataContext);
  if (!ctx) throw new Error('useAppData 는 AppDataProvider 안에서만 사용할 수 있습니다.');
  return ctx;
}
