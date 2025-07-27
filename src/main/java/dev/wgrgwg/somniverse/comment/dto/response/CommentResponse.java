package dev.wgrgwg.somniverse.comment.dto.response;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CommentResponse(
    Long id,
    String content,
    String author,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isDeleted,
    Long parentId,
    List<CommentResponse> children
) {

    public static CommentResponse fromEntity(Comment comment) {
        Long parentId = null;
        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
        }

        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getMember().getUsername(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            comment.isDeleted(),
            parentId,
            new ArrayList<>()
        );
    }
}
