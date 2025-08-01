package dev.wgrgwg.somniverse.comment.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentErrorCode implements ErrorCode {
    PARENT_NOT_IN_SAME_DREAM("COMMENT_001", "부모 댓글과 같은 꿈 일기에 속해있지 않습니다.", HttpStatus.BAD_REQUEST),
    REPLY_TO_REPLY_NOT_ALLOWED("COMMENT_002", "대댓글에 대댓글을 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),
    PARENT_COMMENT_DELETED("COMMENT_003", "삭제된 댓글에 대댓글을 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),

    COMMENT_NOT_FOUND("COMMENT_003", "해당 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    COMMENT_FORBIDDEN("COMMENT_004", "해당 댓글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommentErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
