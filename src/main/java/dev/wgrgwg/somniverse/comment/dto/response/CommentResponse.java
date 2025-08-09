package dev.wgrgwg.somniverse.comment.dto.response;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import dev.wgrgwg.somniverse.comment.message.CommentMessage;
import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String content,
    String author,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isDeleted,
    Long parentId,
    Long totalChildrenCount
) {

    public static CommentResponse fromEntity(Comment comment, Long totalChildrenCount) {
        return from(comment, comment.getContent(), totalChildrenCount);
    }

    public static CommentResponse fromDeletedEntity(Comment comment, Long totalChildrenCount) {
        return from(comment, CommentMessage.DELETED_COMMENT_CONTENT.getMessage(),
            totalChildrenCount);
    }

    public static CommentResponse fromEntityWithoutChildCount(Comment comment) {
        return from(comment, comment.getContent(), null);
    }

    public static CommentResponse from(Comment comment, String content, Long totalChildrenCount) {
        Long parentId = null;
        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
        }

        return new CommentResponse(
            comment.getId(),
            content,
            comment.getMember().getUsername(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            comment.isDeleted(),
            parentId,
            totalChildrenCount
        );
    }
}
