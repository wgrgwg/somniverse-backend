package dev.wgrgwg.somniverse.member.dto;

import dev.wgrgwg.somniverse.member.Member;
import java.time.LocalDateTime;

public record MemberResponseDto(
    Long id,
    String email,
    String username,
    String role,
    LocalDateTime createdAt
) {

    public static MemberResponseDto fromEntity(Member member) {
        return new MemberResponseDto(
            member.getId(),
            member.getEmail(),
            member.getUsername(),
            member.getRole().toString(),
            member.getCreatedAt()
        );
    }
}
