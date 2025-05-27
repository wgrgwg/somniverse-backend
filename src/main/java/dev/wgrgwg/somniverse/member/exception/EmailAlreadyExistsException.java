package dev.wgrgwg.somniverse.member.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super(MemberErrorMessage.EMAIL_ALREADY_EXISTS);
    }
}
