package dev.wgrgwg.somniverse.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.request.DreamUpdateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.exception.DreamErrorCode;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    class CreateDreamTests {

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
            when(memberService.getMemberOrThrow(testMember.getId())).thenReturn(testMember);
            when(dreamRepository.save(any(Dream.class))).thenReturn(testDream);

            // when
            DreamResponse response = dreamService.createDream(request, testMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 꿈");
            verify(memberService).getMemberOrThrow(testMember.getId());
            verify(dreamRepository).save(any(Dream.class));
        }
    }

    @Nested
    @DisplayName("꿈일기 단건 조회 테스트")
    class GetDreamTests {

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
    @DisplayName("꿈일기 목록 조회 테스트")
    class GetDreamListTests {

        private Pageable pageable;
        private Page<Dream> dreamPage;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);

            Dream dream1 = Dream.builder().member(testMember).title("꿈1").build();
            ReflectionTestUtils.setField(dream1, "id", 201L);
            Dream dream2 = Dream.builder().member(testMember).title("꿈2").build();
            ReflectionTestUtils.setField(dream2, "id", 202L);
            List<Dream> dreamList = List.of(dream1, dream2);

            dreamPage = new PageImpl<>(dreamList, pageable, dreamList.size());
        }

        @Test
        @DisplayName("내 꿈일기 목록 조회 성공 시 Page 응답 반환")
        void getMyDreams_whenCalledByOwner_shouldReturnPagedResponse() {
            // given
            when(dreamRepository.findAllByMemberIdAndIsDeletedFalse(testMember.getId(), pageable))
                .thenReturn(dreamPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getMyDreams(testMember.getId(),
                pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).hasSize(2);
            assertThat(resultPage.getContent().get(0).title()).isEqualTo("꿈1");
            assertThat(resultPage.getContent().get(1).authorUsername()).isEqualTo(
                testMember.getUsername());

            verify(dreamRepository).findAllByMemberIdAndIsDeletedFalse(testMember.getId(),
                pageable);
        }

        @Test
        @DisplayName("전체 공개 꿈일기 목록 조회 성공 시 Page 응답 반환")
        void getPublicDreams_whenCalled_shouldReturnPagedResponse() {
            // given
            when(dreamRepository.findAllByIsPublicTrueAndIsDeletedFalse(pageable)).thenReturn(
                dreamPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getPublicDreams(pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent().get(0).title()).isEqualTo("꿈1");

            verify(dreamRepository).findAllByIsPublicTrueAndIsDeletedFalse(pageable);
        }

        @Test
        @DisplayName("특정 사용자의 공개 꿈일기 목록 조회 성공 시 Page 응답 반환")
        void getPublicDreamsByMember_whenCalledWithMemberId_shouldReturnPagedResponse() {
            // given
            when(dreamRepository.findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(
                otherMember.getId(), pageable)).thenReturn(dreamPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getPublicDreamsByMember(
                otherMember.getId(), pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);

            verify(dreamRepository).findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(
                otherMember.getId(), pageable);
        }

        @Test
        @DisplayName("내 꿈일기 목록이 비어있으면 빈 페이지 반환")
        void getMyDreams_whenNoDreams_shouldReturnEmptyPage() {
            // given
            Page<Dream> emptyPage = Page.empty(pageable);
            when(dreamRepository.findAllByMemberIdAndIsDeletedFalse(testMember.getId(), pageable))
                .thenReturn(emptyPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getMyDreams(testMember.getId(),
                pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
            assertThat(resultPage.getContent()).isEmpty();
            assertThat(resultPage.isFirst()).isTrue();
            assertThat(resultPage.isLast()).isTrue();

            verify(dreamRepository).findAllByMemberIdAndIsDeletedFalse(testMember.getId(),
                pageable);
        }
    }

    @Nested
    @DisplayName("공개/비공개 꿈일기 목록 조회 테스트")
    class DreamsListVisibilityTest {

        private Pageable pageable;
        private Member testMember;
        private Dream publicDream;
        private Dream privateDream;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);

            testMember = Member.builder().id(1L).username("testuser").build();

            publicDream = Dream.builder().member(testMember).title("공개 꿈").isPublic(true).build();
            ReflectionTestUtils.setField(publicDream, "id", 301L);

            privateDream = Dream.builder().member(testMember).title("비공개 꿈").isPublic(false)
                .build();
            ReflectionTestUtils.setField(privateDream, "id", 302L);
        }

        @Test
        @DisplayName("내 꿈일기 목록 조회 시 공개/비공개 꿈 모두 포함해 Page 응답 반환")
        void getMyDreams_whenCalled_shouldReturnPublicAndPrivateDreams() {
            // given
            List<Dream> allMyDreams = List.of(publicDream, privateDream);
            Page<Dream> myDreamsPage = new PageImpl<>(allMyDreams, pageable, allMyDreams.size());

            when(dreamRepository.findAllByMemberIdAndIsDeletedFalse(testMember.getId(), pageable))
                .thenReturn(myDreamsPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getMyDreams(testMember.getId(),
                pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent())
                .extracting(DreamSimpleResponse::title)
                .containsExactlyInAnyOrder("공개 꿈", "비공개 꿈");

            verify(dreamRepository).findAllByMemberIdAndIsDeletedFalse(testMember.getId(),
                pageable);
        }

        @Test
        @DisplayName("전체 공개 꿈일기 목록 조회 시 비공개 꿈은 포함되지 않음")
        void getPublicDreams_whenCalled_shouldReturnOnlyPublicDreams() {
            // given
            List<Dream> publicDreamsOnly = List.of(publicDream);
            Page<Dream> publicDreamsPage = new PageImpl<>(publicDreamsOnly, pageable,
                publicDreamsOnly.size());

            when(dreamRepository.findAllByIsPublicTrueAndIsDeletedFalse(pageable))
                .thenReturn(publicDreamsPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getPublicDreams(pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent()).hasSize(1);
            assertThat(resultPage.getContent().get(0).title()).isEqualTo("공개 꿈");

            verify(dreamRepository).findAllByIsPublicTrueAndIsDeletedFalse(pageable);
        }

        @Test
        @DisplayName("특정 사용자의 공개 꿈일기 목록 조회 시 비공개 꿈은 제외되고 Page 응답 반환")
        void getPublicDreamsByMember_whenCalled_shouldReturnOnlyPublicDreamsOfMember() {
            // given
            List<Dream> publicDreamsOnly = List.of(publicDream);
            Page<Dream> publicDreamsPage = new PageImpl<>(publicDreamsOnly, pageable,
                publicDreamsOnly.size());

            when(dreamRepository.findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(
                testMember.getId(), pageable))
                .thenReturn(publicDreamsPage);

            // when
            Page<DreamSimpleResponse> resultPage = dreamService.getPublicDreamsByMember(
                testMember.getId(), pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).title()).isEqualTo("공개 꿈");

            verify(dreamRepository).findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(
                testMember.getId(), pageable);
        }
    }

    @Nested
    @DisplayName("관리자 꿈일기 전체 조회 테스트")
    class GetAllDreamsForAdminTest {

        private Pageable pageable;
        private Member admin;
        private Dream activeDream;
        private Dream deletedDream;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);

            admin = Member.builder()
                .id(100L)
                .username("admin_user")
                .build();

            activeDream = Dream.builder()
                .id(301L)
                .member(admin)
                .title("삭제되지 않은 꿈")
                .isDeleted(false)
                .build();
            ReflectionTestUtils.setField(activeDream, "id", 301L);

            deletedDream = Dream.builder()
                .id(302L)
                .member(admin)
                .title("삭제된 꿈")
                .isDeleted(true)
                .build();
            ReflectionTestUtils.setField(deletedDream, "id", 302L);
        }

        @Test
        @DisplayName("삭제된 꿈일기 포함 전체 조회 성공하면 전체 꿈일기 Page 응답 반환")
        void getAllDreams_whenIncludingDeleted_shouldReturnAllDreams() {
            // given
            List<Dream> allDreams = List.of(activeDream, deletedDream);
            Page<Dream> dreamsPage = new PageImpl<>(allDreams, pageable, allDreams.size());

            when(dreamRepository.findAll(pageable)).thenReturn(dreamsPage);

            // when
            Page<DreamSimpleResponse> result = dreamService.getAllDreamsForAdmin(pageable, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                .extracting(DreamSimpleResponse::title)
                .containsExactlyInAnyOrder("삭제되지 않은 꿈", "삭제된 꿈");

            verify(dreamRepository).findAll(pageable);
            verify(dreamRepository, never()).findAllByIsDeletedFalse(any());
        }

        @Test
        @DisplayName("삭제되지 않은 꿈일기만 조회 성공하면 삭제되지 않은 꿈일기 Page 응답 반환")
        void getAllDreams_whenExcludingDeleted_shouldReturnOnlyNotDeletedDreams() {
            // given
            List<Dream> notDeletedDreams = List.of(activeDream);
            Page<Dream> dreamsPage = new PageImpl<>(notDeletedDreams, pageable,
                notDeletedDreams.size());

            when(dreamRepository.findAllByIsDeletedFalse(pageable)).thenReturn(dreamsPage);

            // when
            Page<DreamSimpleResponse> result = dreamService.getAllDreamsForAdmin(pageable, false);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("삭제되지 않은 꿈");

            verify(dreamRepository).findAllByIsDeletedFalse(pageable);
            verify(dreamRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("꿈일기 수정 테스트")
    class UpdateDreamTests {

        @Test
        @DisplayName("본인의 꿈일기 수정하면 성공 DreamResponse 반환")
        void updateDream_WhenCalledByOwner_shouldReturnResponse() {
            // given
            DreamUpdateRequest request = new DreamUpdateRequest("수정된 제목", "수정된 내용",
                LocalDate.now().minusDays(1), false);
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when
            DreamResponse response = dreamService.updateDream(testDream.getId(), testMember.getId(),
                request);

            // then
            assertThat(response.title()).isEqualTo("수정된 제목");
            assertThat(response.isPublic()).isFalse();
        }

        @Test
        @DisplayName("다른 사람의 꿈일기 수정 시도하면 예외 발생")
        void updateDream_whenCalledByNotOwner_shouldThrowException() {
            // given
            DreamUpdateRequest request = new DreamUpdateRequest("수정된 제목", "수정된 내용",
                LocalDate.now().minusDays(1), true);
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when & then
            assertThatThrownBy(
                () -> dreamService.updateDream(testDream.getId(), otherMember.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("꿈일기 삭제 테스트")
    class DeleteDreamTests {

        @Test
        @DisplayName("본인의 꿈일기 삭제하면 성공")
        void deleteDream_whenCalledByOwner_shouldSucceed() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when
            dreamService.deleteDream(testDream.getId(), testMember.getId());

            // then
            verify(dreamRepository).findByIdAndIsDeletedFalse(testDream.getId());
            assertThat(testDream.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자의 꿈일기 삭제 시도하면 예외 발생")
        void deleteDream_whenCalledByNotOwner_shouldThrowException() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when & then
            assertThatThrownBy(
                () -> dreamService.deleteDream(testDream.getId(), otherMember.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 꿈일기 삭제 시도하면 예외 발생")
        void deleteDream_whenWithNonExistentDream_shouldThrowException() {
            // givens
            long nonExistentDreamId = 999L;
            when(dreamRepository.findByIdAndIsDeletedFalse(nonExistentDreamId)).thenReturn(
                Optional.empty());

            // when & then
            assertThatThrownBy(
                () -> dreamService.deleteDream(nonExistentDreamId, testMember.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(DreamErrorCode.DREAM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("관리자가 관리자 권한으로 댓글 삭제 시 성공")
        void deleteDreamByAdmin_whenCalledByAdmin_shouldSucceed() {
            // given
            when(dreamRepository.findByIdAndIsDeletedFalse(testDream.getId())).thenReturn(
                Optional.of(testDream));

            // when
            dreamService.deleteDreamByAdmin(testDream.getId());

            // then
            assertThat(testDream.isDeleted()).isTrue();
            verify(dreamRepository).findByIdAndIsDeletedFalse(testDream.getId());
        }
    }
}