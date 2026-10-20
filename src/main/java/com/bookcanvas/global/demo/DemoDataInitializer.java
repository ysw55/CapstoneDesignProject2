package com.bookcanvas.global.demo;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.book.service.BookFinder;
import com.bookcanvas.domain.quote.entity.Quote;
import com.bookcanvas.domain.quote.entity.QuoteComment;
import com.bookcanvas.domain.quote.entity.QuoteLike;
import com.bookcanvas.domain.quote.entity.QuoteTagMap;
import com.bookcanvas.domain.quote.repository.QuoteCommentRepository;
import com.bookcanvas.domain.quote.repository.QuoteLikeRepository;
import com.bookcanvas.domain.quote.repository.QuoteRepository;
import com.bookcanvas.domain.tag.repository.TagRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.domain.user.service.DemoAccounts;
import com.bookcanvas.global.config.DemoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 캡스톤 시연용 샘플 데이터 (app.demo.seed-enabled=true 이고 시연 계정이 아직 없을 때 1회만 실행)
 *
 * <ul>
 *   <li>커뮤니티 사용자 5명 (비밀번호 {@value #DEMO_PASSWORD}) + 피드 글 7개, 좋아요, 댓글</li>
 *   <li>카카오 시연 계정(moonreader_): 책장 6권, 그중 3권 완독 인증 (300P)</li>
 *   <li>구글 시연 계정(구글독자): 책장 2권, 그중 1권 완독 인증 (100P) — 계정 전환 시연용</li>
 * </ul>
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    static final String DEMO_PASSWORD = "demo1234";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DemoProperties demoProperties;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final QuoteCommentRepository quoteCommentRepository;
    private final BookCompletionRepository bookCompletionRepository;
    private final BookFinder bookFinder;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoProperties.seedEnabled() || userRepository.existsByEmail(DemoAccounts.KAKAO.email())) {
            return;
        }
        log.info("[Demo] 시연용 샘플 데이터를 생성합니다.");

        // ── 사용자 ──
        Map<String, User> users = new LinkedHashMap<>();
        for (String[] u : new String[][]{
                {"새벽책방", "dawn"}, {"느린산책", "slowwalk"}, {"종이비행기", "paperplane"},
                {"책갈피수집가", "bookmark"}, {"오늘도한장", "onepage"}}) {
            users.put(u[0], userRepository.save(User.builder()
                    .username(u[0])
                    .email(u[1] + "@demo.bookcanvas.app")
                    .password(passwordEncoder.encode(DEMO_PASSWORD))
                    .build()));
        }
        User kakao = userRepository.save(socialUser(DemoAccounts.KAKAO));
        User google = userRepository.save(socialUser(DemoAccounts.GOOGLE));
        users.put(kakao.getUsername(), kakao);

        // ── 피드 (오래된 글부터 넣어야 최신순 피드에서 아래 순서의 역순으로 보임) ──
        Quote gatsby = quote(users.get("새벽책방"), "위대한 개츠비", "F. 스콧 피츠제럴드", "꿈을 응원받고 싶을 때",
                "그래서 우리는 물결을 거스르는 배처럼, 쉴 새 없이 과거 속으로 밀려나면서도 앞으로 나아가는 것이다.");
        Quote frankl = quote(users.get("오늘도한장"), "죽음의 수용소에서", "빅터 프랭클", "다시 일어서고 싶을 때",
                "왜 살아야 하는지 아는 사람은 그 어떤 상황도 견딜 수 있다.");
        Quote alchemist = quote(users.get("책갈피수집가"), "연금술사", "파울로 코엘료", "꿈을 응원받고 싶을 때",
                "자네가 무언가를 간절히 원할 때, 온 우주는 자네의 소망이 실현되도록 도와준다네.");
        Quote oldMan = quote(users.get("종이비행기"), "노인과 바다", "어니스트 헤밍웨이", "다시 일어서고 싶을 때",
                "인간은 파멸당할 수는 있어도 패배하지는 않아.");
        Quote momo = quote(users.get("느린산책"), "모모", "미하엘 엔데", "마음이 조급할 때",
                "한꺼번에 도로 전체를 생각해서는 안 돼. 다음 걸음, 다음 숨, 다음 비질만 생각해야 하는 거야.");
        Quote demian = quote(kakao, "데미안", "헤르만 헤세", "새로운 시작 앞에서",
                "새는 알에서 나오려고 투쟁한다. 알은 세계이다. 태어나려는 자는 하나의 세계를 깨뜨려야 한다.");
        Quote prince = quote(users.get("새벽책방"), "어린 왕자", "앙투안 드 생텍쥐페리", "위로가 필요할 때",
                "가장 중요한 것은 눈에 보이지 않아.");

        // ── 좋아요 (커뮤니티 사용자끼리만 — 시연 계정은 직접 눌러 볼 수 있도록 비워 둠) ──
        List<User> community = users.values().stream().filter(u -> u != kakao).toList();
        likeBy(oldMan, community, 5);
        likeBy(frankl, community, 4);
        likeBy(demian, community, 4);
        likeBy(prince, community, 3);
        likeBy(gatsby, community, 3);
        likeBy(alchemist, community, 2);
        likeBy(momo, community, 2);

        // ── 댓글 ──
        quoteCommentRepository.save(new QuoteComment(users.get("느린산책"), prince, "오늘 딱 필요한 문장이었어요."));
        quoteCommentRepository.save(new QuoteComment(users.get("오늘도한장"), prince, "어릴 때 읽을 때랑 느낌이 완전히 달라요."));
        quoteCommentRepository.save(new QuoteComment(users.get("종이비행기"), demian, "이직 앞두고 계속 곱씹고 있는 구절이에요."));

        // ── 시연 계정 책장 ──
        shelf(kakao, "데미안", "헤르만 헤세", "2026-10-12", "#1B4D3E", true);
        shelf(kakao, "모모", "미하엘 엔데", "2026-10-03", "#E9ECEF", true);
        shelf(kakao, "미움받을 용기", "기시미 이치로 · 고가 후미타케", "2026-09-21", "#212529", true);
        shelf(kakao, "노인과 바다", "어니스트 헤밍웨이", "2026-09-02", "#2F6B57", false);
        shelf(kakao, "어린 왕자", "앙투안 드 생텍쥐페리", "2026-08-15", "#ADB5BD", false);
        shelf(kakao, "위대한 개츠비", "F. 스콧 피츠제럴드", "2026-07-28", "#495057", false);

        shelf(google, "연금술사", "파울로 코엘료", "2026-10-05", "#2F6B57", true);
        shelf(google, "어린 왕자", "앙투안 드 생텍쥐페리", "2026-09-14", "#ADB5BD", false);

        log.info("[Demo] 샘플 데이터 생성 완료 — 시연 계정: 카카오({}), 구글({}) / 커뮤니티 계정 비밀번호: {}",
                kakao.getUsername(), google.getUsername(), DEMO_PASSWORD);
    }

    // =========================================================================

    private User socialUser(DemoAccounts account) {
        return User.builder()
                .email(account.email())
                .username(account.username())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider(account.provider())
                .build();
    }

    private Quote quote(User user, String title, String author, String tagName, String content) {
        Book book = bookFinder.findOrCreate(title, author);
        Quote quote = Quote.builder().content(content).isPublic(true).user(user).book(book).build();
        QuoteTagMap.of(quote, tagRepository.findByName(tagName).orElseThrow());
        return quoteRepository.saveAndFlush(quote);
    }

    private void likeBy(Quote quote, List<User> users, int count) {
        users.stream().limit(count).forEach(u -> quoteLikeRepository.save(new QuoteLike(u, quote)));
    }

    private void shelf(User user, String title, String author, String date, String color, boolean certified) {
        BookCompletion entry = BookCompletion.builder()
                .user(user)
                .book(bookFinder.findOrCreate(title, author))
                .completedAt(LocalDate.parse(date))
                .coverColor(color)
                .build();
        if (certified) {
            entry.certify(sampleReview(title), entry.getCompletedAt(), 100, LocalDateTime.now(KST));
            user.addRewardPoint(100);
            user.applyProfileSkin("#1B4D3E", "#133A2F");
        }
        bookCompletionRepository.save(entry);
    }

    /** 200자 이상 샘플 독후감 */
    private static String sampleReview(String title) {
        String review = "『" + title + "』을 다 읽고 나서 한동안 책을 덮지 못했다. "
                + "처음에는 이야기를 따라가기에 바빴지만, 중반을 넘기면서부터는 문장 하나하나가 지금의 나에게 건네는 말처럼 느껴졌다. "
                + "특히 주인공이 스스로의 선택을 받아들이는 장면에서는 지난 몇 달 동안 미뤄 두었던 고민들이 떠올라 한참을 멈춰 있었다. "
                + "누군가 정답을 대신 정해 주기를 바랐던 마음이 부끄러워졌고, 결국 내 속도대로 한 걸음씩 나아가면 된다는 사실이 큰 위로가 되었다. "
                + "시간이 지나 다시 읽으면 또 다른 문장이 마음에 남을 것 같아, 책장에서 가장 잘 보이는 자리에 꽂아 두기로 했다.";
        if (review.length() < 200) throw new IllegalStateException("샘플 독후감은 200자 이상이어야 합니다.");
        return review;
    }
}
