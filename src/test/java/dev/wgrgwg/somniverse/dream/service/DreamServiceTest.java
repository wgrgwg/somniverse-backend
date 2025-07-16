package dev.wgrgwg.somniverse.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamRepository dreamRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private DreamService dreamService;

    private Member testMember;
    private Member otherMember;
    private Dream testDream;
    private Dream privateTestDream;

    @BeforeEach
    void setUp() {
        testMember = Member.builder().id(1L).username("testuser").role(Role.USER).build();
        otherMember = Member.builder().id(2L).username("otheruser").role(Role.USER).build();

        testDream = Dream.builder()
            .member(testMember)
            .title("테스트 꿈")
            .content("테스트 내용")
            .dreamDate(LocalDate.now())
            .isPublic(true)
            .build();
        ReflectionTestUtils.setField(testDream, "id", 101L);

        privateTestDream = Dream.builder()
            .member(testMember)
            .title("비공개 꿈")
            .content("비공개 내용")
            .dreamDate(LocalDate.now())
            .isPublic(false)
            .build();
        ReflectionTestUtils.setField(privateTestDream, "id", 102L);
    }

    @Nested
    @DisplayName("꿈 일기 생성 테스트")
    class createDreamTests {

        @Test
        @DisplayName("꿈일기 생성 성공 시 DreamResponse 반환")
        void createDream_success() {
            // given
            DreamCreateRequest request = new DreamCreateRequest(
                "테스트 꿈",
                "테스트 내용",
                LocalDate.now(),
                true
            );
            when(memberService.findById(testMember.getId())).thenReturn(testMember);
            when(dreamRepository.save(any(Dream.class))).thenReturn(testDream);

            // when
            DreamResponse response = dreamService.createDream(request, testMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 꿈");
            verify(memberService).findById(testMember.getId());
            verify(dreamRepository).save(any(Dream.class));
        }
    }
    
}