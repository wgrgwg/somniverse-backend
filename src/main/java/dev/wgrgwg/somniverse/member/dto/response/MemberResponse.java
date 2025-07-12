package dev.wgrgwg.somniverse.member.dto.response;

import dev.wgrgwg.somniverse.member.domain.Member;
import java.time.LocalDateTime;

public record MemberResponse(
    Long id,
    String email,
    String username,
    String role,
    LocalDateTime createdAt
) {

    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getUsername(),
            member.getRole().toString(),
            member.getCreatedAt()
        );
    }
}
