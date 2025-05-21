package dev.wgrgwg.somniverse.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberSignupRequestDto(
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Size(max = 30, message = "이메일은 최대 30자까지 입력 가능합니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>\\[\\]~`_+=\\\\/';-]).*$",
        message = "비밀번호는 영문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다.")
    String password,

    @NotBlank(message = "사용자명은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "사용자명은 2자 이상 20자 이하로 입력해주세요.")
    String username
) {

}
