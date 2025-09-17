package dev.wgrgwg.somniverse.member.dto.request;

import dev.wgrgwg.somniverse.member.domain.Role;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
    @NotNull(message = "변경할 역할은 필수 입력 항목입니다.")
    Role role
) {

}
