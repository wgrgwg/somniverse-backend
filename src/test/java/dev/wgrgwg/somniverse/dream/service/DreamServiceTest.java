package dev.wgrgwg.somniverse.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.request.DreamUpdateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.exception.DreamErrorCode;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.time.LocalDate;
import java.util.Optional;
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
    @DisplayName("꿈일기 생성 테스트")
    class createDreamTests {

        @Test
        @DisplayName("꿈일기 생성 성공하면 DreamResponse 반환")
        void createDream_whenWithValidInfo_shouldReturnResponse() {
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

    @Nested
    @DisplayName("꿈일기 단건 조회 테스트")
    class getDreamTests {

        @Test
        @DisplayName("꿈일기 조회 성공하면 DreamResponse 반환")
        void getMyDream_whenCalledByOwner_shouldReturnResponse() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when
            DreamResponse response = dreamService.getMyDream(testDream.getId(), testMember.getId());

            // then
            assertThat(response.id()).isEqualTo(testDream.getId());
            assertThat(response.author().id()).isEqualTo(testMember.getId());
        }

        @Test
        @DisplayName("본인 꿈일기가 아니면 예외 발생")
        void getMyDream_whenCalledByNotOwner_shouldThrowException() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when & then
            assertThatThrownBy(
                () -> dreamService.getMyDream(testDream.getId(), otherMember.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("다른 사용자 공개 꿈일기 조회 성공하면 DreamResponse 반환")
        void getDreamWithAccessControl_whenForPublicDream_shouldReturnResponse() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when
            DreamResponse response = dreamService.getDreamWithAccessControl(testDream.getId(),
                otherMember.getId(), false);

            // then
            assertThat(response.id()).isEqualTo(testDream.getId());
        }

        @Test
        @DisplayName("다른 사용자 비공개 꿈일기 조회하면 예외 발생")
        void getDreamWithAccessControl_whenForPrivateDreamByNotAdmin_shouldThrowException() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(privateTestDream.getId())).thenReturn(
                Optional.of(privateTestDream));

            // when & then
            assertThatThrownBy(
                () -> dreamService.getDreamWithAccessControl(privateTestDream.getId(),
                    otherMember.getId(), false))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("관리자는 비공개 꿈일기 조회 성공하면 DreamResponse 반환")
        void getDreamWithAccessControl_whenForPrivateDreamByAdmin_shouldReturnResponse() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(privateTestDream.getId())).thenReturn(
                Optional.of(privateTestDream));

            // when
            DreamResponse response = dreamService.getDreamWithAccessControl(
                privateTestDream.getId(), otherMember.getId(), true);

            // then
            assertThat(response.id()).isEqualTo(privateTestDream.getId());
        }
    }

    @Nested
    @DisplayName("꿈일기 수정 테스트")
    class UpdateDreamTests {

        @Test
        @DisplayName("본인의 꿈일기 수정하면 성공 DreamResponse 반환")
        void updateDream_WhenCalledByOwner_shouldReturnResponse() {
            // given
            DreamUpdateRequest request = new DreamUpdateRequest("수정된 제목", "수정된 내용", LocalDate.now().minusDays(1), false);
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(Optional.of(testDream));

            // when
            DreamResponse response = dreamService.updateDream(testDream.getId(), testMember.getId(), request);

            // then
            assertThat(response.title()).isEqualTo("수정된 제목");
            assertThat(response.isPublic()).isFalse();
        }

        @Test
        @DisplayName("다른 사람의 꿈일기 수정 시도하면 예외 발생")
        void updateDream_whenCalledByNotOwner_shouldThrowException() {
            // given
            DreamUpdateRequest request = new DreamUpdateRequest("수정된 제목", "수정된 내용", LocalDate.now().minusDays(1), true);
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(Optional.of(testDream));

            // when & then
            assertThatThrownBy(() -> dreamService.updateDream(testDream.getId(), otherMember.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_FORBIDDEN.getMessage());
        }
    }

    
}