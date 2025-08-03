package dev.wgrgwg.somniverse.comment.service;

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
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final DreamService dreamService;
    private final MemberService memberService;

    @Transactional
    public CommentResponse createComment(Long dreamId, CommentCreateRequest request,
        Long memberId) {

        Dream dream = dreamService.getDreamOrThrow(dreamId);
        Member member = memberService.getMemberOrThrow(memberId);

        Comment parent = resolveParentComment(request.parentId(), dreamId);

        Comment comment = Comment.builder()
            .content(request.content())
            .dream(dream)
            .member(member)
            .parent(parent)
            .build();

        if (parent != null) {
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.fromEntity(savedComment, 0L);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getPagedParentCommentsByDream(Long dreamId, boolean isAdmin,
        Pageable pageable) {
        dreamService.getDreamOrThrow(dreamId);

        Page<Comment> commentsPage = commentRepository.findAllByDreamIdAndParentIsNull(dreamId,
            pageable);
        List<Long> parentIds = commentsPage.getContent().stream()
            .map(Comment::getId)
            .toList();

        Map<Long, Long> childCountMap = getChildCountMap(parentIds);

        return commentsPage.map(comment -> convertToDtoWithAccessControl(comment, isAdmin,
            childCountMap.getOrDefault(comment.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getPagedChildrenCommentsByParent(Long parentId, boolean isAdmin,
        Pageable pageable) {
        getCommentOrThrow(parentId);

        Page<Comment> commentsPage = commentRepository.findAllByParentId(parentId, pageable);

        return commentsPage.map(comment -> convertToDtoWithAccessControl(comment, isAdmin, 0L));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request,
        Long memberId) {
        Comment comment = getCommentOrThrow(commentId);

        validateUpdatable(comment, memberId);

        comment.updateContent(request.content());

        return CommentResponse.fromEntityWithoutChildCount(comment);
    }

    private Comment resolveParentComment(Long parentId, Long dreamId) {
        if (parentId == null) {
            return null;
        }

        Comment parent = getCommentOrThrow(parentId);
        validateParent(parent, dreamId);
        return parent;
    }

    private Map<Long, Long> getChildCountMap(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> result = commentRepository.countChildrenGroupedByParentId(parentIds);
        return result.stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));
    }

    private CommentResponse convertToDtoWithAccessControl(Comment comment, boolean isAdmin,
        Long totalChildCount) {
        if (!isAdmin && comment.isDeleted()) {
            return CommentResponse.fromDeletedEntity(comment, totalChildCount);
        }

        return CommentResponse.fromEntity(comment, totalChildCount);
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findByIdAndIsDeletedFalse(commentId)
            .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateParent(Comment parent, Long dreamId) {
        if (parent.isDeleted()) {
            throw new CustomException(CommentErrorCode.PARENT_COMMENT_DELETED);
        }

        if (!parent.getDream().getId().equals(dreamId)) {
            throw new CustomException(CommentErrorCode.PARENT_NOT_IN_SAME_DREAM);
        }

        if (parent.getParent() != null) {
            throw new CustomException(CommentErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);
        }
    }

    private void validateUpdatable(Comment comment, Long memberId){
        if(comment.isDeleted()){
            throw new CustomException(CommentErrorCode.DELETED_COMMENT_CANNOT_BE_UPDATED);
        }

        validateOwner(comment, memberId);
    }

    private void validateOwner(Comment comment, Long memberId) {
        if (!Objects.equals(comment.getMember().getId(), memberId)) {
            throw new CustomException(CommentErrorCode.COMMENT_FORBIDDEN);
        }
    }
}
