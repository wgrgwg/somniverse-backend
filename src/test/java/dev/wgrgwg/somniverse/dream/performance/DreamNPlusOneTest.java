package dev.wgrgwg.somniverse.dream.performance;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import dev.wgrgwg.somniverse.comment.repository.CommentRepository;
import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.member.service.MemberService;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@Import(DreamService.class)
class DreamNPlusOneTest {

    @Autowired
    private DreamService dreamService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private MemberService memberService;

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Statistics stats;
    private Long dreamId;
    private Long memberId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Member testMember = memberRepository.save(
            Member.builder()
                .email("user@test.com")
                .password("pw")
                .username("user")
                .role(Role.USER)
                .build()
        );
        memberId = testMember.getId();

        Dream testDream = dreamRepository.save(
            Dream.builder()
                .title("테스트 꿈")
                .content("테스트 내용")
                .member(testMember)
                .isPublic(true)
                .build()
        );
        dreamId = testDream.getId();

        for (int i = 1; i <= 100; i++) {
            Comment comment = Comment.builder()
                .dream(testDream)
                .member(testMember)
                .content("댓글 " + i)
                .build();
            commentRepository.save(comment);
        }

        for (int i = 1; i <= 100; i++) {
            Member member = memberRepository.save(
                Member.builder()
                    .email("user" + i + "@test.com")
                    .password("pw")
                    .username("user" + i)
                    .role(Role.USER)
                    .build()
            );

            Dream dream = dreamRepository.save(
                Dream.builder()
                    .title("테스트 꿈" + i)
                    .content("테스트 내용" + i)
                    .member(member)
                    .isPublic(true)
                    .build()
            );
        }

        pageable = PageRequest.of(0, 100);

        em.flush();
        em.clear();
        stats.clear();
    }

    @Test
    @DisplayName("Dream 삭제 N+1 성능(쿼리 수) 테스트")
    void deleteDreamQueryCount() {
        // when
        dreamService.deleteDream(dreamId, memberId);

        em.flush();
        em.clear();

        // then
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("총 실행된 쿼리 수 = " + queryCount);
        Assertions.assertThat(queryCount).isLessThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("공개 꿈 목록 조회 시 N+1 성능(쿼리 수) 테스트")
    void checkPublicDreamsQueryCount() {
        // when
        dreamService.getPublicDreams(pageable);

        // then
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("[공개 꿈 목록] 실행된 쿼리 수 = " + queryCount);
        Assertions.assertThat(queryCount).isLessThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("전체 꿈 목록(관리자용, 삭제된 꿈 포함) 조회 시 N+1 성능(쿼리 수) 테스트")
    void checkAllDreamsIncludingDeletedDreamsForAdminQueryCount() {
        // when
        dreamService.getAllDreamsForAdmin(pageable, true);

        // then
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("[전체 꿈 목록(관리자용)] 실행된 쿼리 수 = " + queryCount);
        Assertions.assertThat(queryCount).isLessThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("전체 꿈 목록(관리자용, 삭제된 꿈 미포함) 조회 시 N+1 성능(쿼리 수) 테스트")
    void checkAllDreamsForAdminQueryCount() {
        // when
        dreamService.getAllDreamsForAdmin(pageable, false);

        // then
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("[전체 꿈 목록(관리자용)] 실행된 쿼리 수 = " + queryCount);
        Assertions.assertThat(queryCount).isLessThanOrEqualTo(2L);
    }
}
