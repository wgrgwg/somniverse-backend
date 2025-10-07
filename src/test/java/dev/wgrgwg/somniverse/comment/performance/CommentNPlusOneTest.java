package dev.wgrgwg.somniverse.comment.performance;

import dev.wgrgwg.somniverse.comment.repository.CommentRepository;
import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.member.service.MemberService;
import jakarta.persistence.EntityManager;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@DataJpaTest
@Import(CommentService.class)
@Transactional
class CommentNPlusOneTest {

    @Autowired
    private CommentService commentService;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private MemberService memberService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private EntityManager em;

    private Long dreamId;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        var dream = dreamRepository.save(
            dev.wgrgwg.somniverse.dream.domain.Dream.builder().title("꿈 제목").content("꿈 내용").member(
                memberRepository.save(
                    dev.wgrgwg.somniverse.member.domain.Member.builder().email("owner@test.com")
                        .password("pw").username("owner").role(Role.ADMIN).build())).build());
        dreamId = dream.getId();

        IntStream.rangeClosed(1, 100).forEach(i -> {
            Member member = memberRepository.save(
                dev.wgrgwg.somniverse.member.domain.Member.builder().email("user" + i + "@test.com")
                    .password("pw").username("user" + i).role(Role.USER).build());

            commentRepository.save(
                dev.wgrgwg.somniverse.comment.domain.Comment.builder().content("댓글 " + i)
                    .dream(dream).member(member).build());
        });

        em.flush();
        em.clear();

        stats.clear();
    }

    @Test
    @DisplayName("댓글 조회 N+1 성능 테스트")
    void detectNPlusOnePerformance() {

        commentService.getPagedParentCommentsByDream(dreamId, false, PageRequest.of(0, 100));

        System.out.println("총 실행된 쿼리 수 = " + stats.getPrepareStatementCount());
    }
}
