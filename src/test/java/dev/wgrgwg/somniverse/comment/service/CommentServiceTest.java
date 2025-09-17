package dev.wgrgwg.somniverse.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import dev.wgrgwg.somniverse.comment.dto.request.CommentCreateRequest;
import dev.wgrgwg.somniverse.comment.dto.request.CommentUpdateRequest;
import dev.wgrgwg.somniverse.comment.dto.response.CommentResponse;
import dev.wgrgwg.somniverse.comment.exception.CommentErrorCode;
import dev.wgrgwg.somniverse.comment.repository.CommentRepository;
import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.util.ArrayList;
import java.util.Collections;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private DreamService dreamService;

    @InjectMocks
    private CommentService commentService;

    private Member testMember;
    private Member otherMember;
    private Dream testDream;
    private Comment parentComment;
    private Comment childComment;

    @BeforeEach
    void setUp() {
        testMember = Member.builder().id(1L).username("testuser").role(Role.USER).build();
        otherMember = Member.builder().id(2L).username("otheruser").role(Role.USER).build();

        testDream = Dream.builder().id(101L).member(otherMember).build();

        parentComment = Comment.builder().dream(testDream).member(otherMember).content("부모 댓글")
            .build();
        ReflectionTestUtils.setField(parentComment, "id", 201L);
        ReflectionTestUtils.setField(parentComment, "children", new ArrayList<>());

        childComment = Comment.builder().dream(testDream).member(testMember).content("대댓글")
            .parent(parentComment).build();
        ReflectionTestUtils.setField(childComment, "id", 202L);
    }

    @Nested
    @DisplayName("댓글 작성 테스트")
    class CreateCommentTests {

        @Test
        @DisplayName("최상위 댓글 생성 성공 시 CommentResponse 반환")
        void createParentComment_whenWithValidInfo_shouldReturnResponse() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("새 댓글", null);
            when(dreamService.getDreamOrThrow(testDream.getId())).thenReturn(testDream);
            when(memberService.getMemberOrThrow(testMember.getId())).thenReturn(testMember);
            when(commentRepository.save(any(Comment.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

            // when
            CommentResponse response = commentService.createComment(testDream.getId(), request,
                testMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("새 댓글");
            assertThat(response.author().username()).isEqualTo(testMember.getUsername());
            assertThat(response.parentId()).isNull();
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("대댓글 생성 성공 시 CommentResponse 반환")
        void createChildComment_whenWithValidParent_shouldReturnResponse() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("대댓글", parentComment.getId());
            when(dreamService.getDreamOrThrow(testDream.getId())).thenReturn(testDream);
            when(memberService.getMemberOrThrow(testMember.getId())).thenReturn(testMember);
            when(commentRepository.findByIdAndIsDeletedFalse(parentComment.getId())).thenReturn(
                Optional.of(parentComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

            // when
            CommentResponse response = commentService.createComment(testDream.getId(), request,
                testMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("대댓글");
            assertThat(response.parentId()).isEqualTo(parentComment.getId());
            verify(commentRepository).findByIdAndIsDeletedFalse(parentComment.getId());
        }

        @Test
        @DisplayName("삭제된 댓글에 대댓글 작성 시도 시 예외 발생")
        void createChildComment_whenParentIsDeleted_shouldThrowException() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("대댓글", parentComment.getId());
            ReflectionTestUtils.setField(parentComment, "isDeleted", true);
            when(dreamService.getDreamOrThrow(testDream.getId())).thenReturn(testDream);
            when(memberService.getMemberOrThrow(testMember.getId())).thenReturn(testMember);
            when(commentRepository.findByIdAndIsDeletedFalse(parentComment.getId())).thenReturn(
                Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(testDream.getId(), request,
                testMember.getId())).isInstanceOf(CustomException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("댓글 수정 테스트")
    class UpdateCommentTests {

        @Test
        @DisplayName("댓글 수정 성공 시 CommentResponse 반환")
        void updateComment_whenCalledByOwner_shouldReturnResponse() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용", null);
            when(commentRepository.findByIdAndIsDeletedFalse(childComment.getId())).thenReturn(
                Optional.of(childComment));

            // when
            CommentResponse response = commentService.updateComment(childComment.getId(), request,
                testMember.getId());

            // then
            assertThat(childComment.getContent()).isEqualTo("수정된 내용");
            assertThat(response.content()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("작성자가 아닌 경우 댓글   수정 시 예외 발생")
        void updateComment_whenCalledByNotOwner_shouldThrowException() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정 시도", null);
            when(commentRepository.findByIdAndIsDeletedFalse(parentComment.getId())).thenReturn(
                Optional.of(parentComment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(parentComment.getId(), request,
                testMember.getId())).isInstanceOf(CustomException.class)
                .hasMessage(CommentErrorCode.COMMENT_FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("댓글 삭제 테스트")
    class DeleteCommentTests {

        @Test
        @DisplayName("작성자가 댓글 삭제 시 성공")
        void deleteComment_whenCalledByOwner_shouldSucceed() {
            // given
            when(commentRepository.findByIdAndIsDeletedFalse(childComment.getId())).thenReturn(
                Optional.of(childComment));

            // when
            commentService.deleteComment(childComment.getId(), testMember.getId());

            // then
            assertThat(childComment.isDeleted()).isTrue();
            verify(commentRepository).findByIdAndIsDeletedFalse(childComment.getId());
        }

        @Test
        @DisplayName("관리자가 댓글 삭제 시 성공")
        void deleteCommentByAdmin_whenCalledByAdmin_shouldSucceed() {
            // given
            when(commentRepository.findByIdAndIsDeletedFalse(parentComment.getId())).thenReturn(
                Optional.of(parentComment));

            // when
            commentService.deleteCommentByAdmin(parentComment.getId());

            // then
            assertThat(parentComment.isDeleted()).isTrue();
            verify(commentRepository).findByIdAndIsDeletedFalse(parentComment.getId());
        }

        @Test
        @DisplayName("작성자가 아닌 경우 댓글 삭제 시 예외 발생")
        void deleteComment_whenCalledByNotOwner_shouldThrowException() {
            // given
            when(commentRepository.findByIdAndIsDeletedFalse(parentComment.getId())).thenReturn(
                Optional.of(parentComment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(parentComment.getId(),
                testMember.getId())).isInstanceOf(CustomException.class)
                .hasMessage(CommentErrorCode.COMMENT_FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회 테스트")
    class GetCommentListTests {

        private Pageable pageable;

        @BeforeEach
        void setup() {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("일반 사용자로 조회 시 삭제된 댓글은 내용이 변경되어 보임")
        void getPagedParentComments_whenNotAdmin_shouldShowDeletedMessage() {
            // given
            ReflectionTestUtils.setField(parentComment, "isDeleted", true);
            Page<Comment> commentsPage = new PageImpl<>(List.of(parentComment), pageable, 1);
            when(dreamService.getDreamOrThrow(anyLong())).thenReturn(testDream);
            when(commentRepository.findAllByDreamIdAndParentIsNull(anyLong(),
                any(Pageable.class))).thenReturn(commentsPage);
            when(commentRepository.countChildrenGroupedByParentId(anyList())).thenReturn(
                Collections.emptyList());

            // when
            Page<CommentResponse> responsePage = commentService.getPagedParentCommentsByDream(
                testDream.getId(), false, pageable);

            // then
            assertThat(responsePage.getContent().get(0).content()).isEqualTo("삭제된 댓글입니다.");
        }

        @Test
        @DisplayName("관리자로 조회 시 삭제된 댓글도 내용이 그대로 보임")
        void getPagedParentComments_whenAdmin_shouldShowOriginalContent() {
            // given
            ReflectionTestUtils.setField(parentComment, "isDeleted", true);
            Page<Comment> commentsPage = new PageImpl<>(List.of(parentComment), pageable, 1);
            when(dreamService.getDreamOrThrow(anyLong())).thenReturn(testDream);
            when(commentRepository.findAllByDreamIdAndParentIsNull(anyLong(),
                any(Pageable.class))).thenReturn(commentsPage);
            when(commentRepository.countChildrenGroupedByParentId(anyList())).thenReturn(
                Collections.emptyList());

            // when
            Page<CommentResponse> responsePage = commentService.getPagedParentCommentsByDream(
                testDream.getId(), true, pageable);

            // then
            assertThat(responsePage.getContent().get(0).content()).isEqualTo("부모 댓글");
        }

        @Test
        @DisplayName("대댓글 목록 조회 성공 시 Page<CommentResponse> 반환")
        void getPagedChildrenComments_whenWithValidParent_shouldReturnPagedResponse() {
            // given
            Page<Comment> childrenPage = new PageImpl<>(List.of(childComment), pageable, 1);
            when(commentRepository.findAllByParentId(parentComment.getId(), pageable)).thenReturn(
                childrenPage);

            // when
            Page<CommentResponse> responsePage = commentService.getPagedChildrenCommentsByParent(
                parentComment.getId(), false, pageable);

            // then
            assertThat(responsePage.getTotalElements()).isEqualTo(1);
            assertThat(responsePage.getContent().get(0).content()).isEqualTo("대댓글");
        }
    }
}