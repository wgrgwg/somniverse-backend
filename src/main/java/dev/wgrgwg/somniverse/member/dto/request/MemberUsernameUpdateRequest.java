package dev.wgrgwg.somniverse.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUsernameUpdateRequest(
    @NotBlank(message = "사용자명은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "사용자명은 2자 이상 20자 이하로 입력해주세요.")
    String username
) {

}
