package dev.wgrgwg.somniverse.comment.message;

import lombok.Getter;

@Getter
public enum CommentMessage {
    DELETED_COMMENT_CONTENT("삭제된 댓글입니다.");

    private final String message;

    CommentMessage(String message) {
        this.message = message;
    }
}
