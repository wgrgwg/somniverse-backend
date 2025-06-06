package dev.wgrgwg.somniverse.member.message;

import lombok.Getter;

@Getter
public enum MemberSuccessMessage {
    SIGNUP_SUCCESS("회원가입이 완료되었습니다."),
    LOGIN_SUCCESS("로그인에 성공했습니다.");

    private final String message;

    MemberSuccessMessage(String message) {
        this.message = message;
    }
}
