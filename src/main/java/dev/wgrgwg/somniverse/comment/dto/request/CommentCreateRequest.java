package dev.wgrgwg.somniverse.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    String content,

    Long parentId
) {

}
