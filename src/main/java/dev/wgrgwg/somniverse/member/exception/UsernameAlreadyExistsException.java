package dev.wgrgwg.somniverse.member.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super(MemberErrorMessage.USERNAME_ALREADY_EXISTS);
    }
}
