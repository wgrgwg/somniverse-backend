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
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@DataJpaTest
@Import(DreamService.class)
@Transactional
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

    @BeforeEach
    void setUp() {
        stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Member member = memberRepository.save(
            Member.builder()
                .email("user@test.com")
                .password("pw")
                .username("user")
                .role(Role.USER)
                .build()
        );
        memberId = member.getId();

        Dream dream = dreamRepository.save(
            Dream.builder()
                .title("테스트 꿈")
                .content("테스트 내용")
                .member(member)
                .build()
        );
        dreamId = dream.getId();

        IntStream.rangeClosed(1, 100).forEach(i -> {
            Comment comment = Comment.builder()
                .dream(dream)
                .member(member)
                .content("댓글 " + i)
                .build();
            commentRepository.save(comment);
        });

        em.flush();
        em.clear();
        stats.clear();
    }

    @Test
    @DisplayName("Dream 삭제 시 발생하는 쿼리 수 측정")
    void deleteDreamQueryCount() {
        // when
        dreamService.deleteDream(dreamId, memberId);

        em.flush();
        em.clear();

        // then
        System.out.println("총 실행된 쿼리 수 = " + stats.getPrepareStatementCount());
    }
}
