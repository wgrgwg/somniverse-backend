package dev.wgrgwg.somniverse.member.dto.response;

import dev.wgrgwg.somniverse.member.domain.Member;
import java.time.LocalDateTime;

public record MemberAdminResponse(
    Long id,
    String email,
    String username,
    String role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isDeleted,
    LocalDateTime deletedAt
) {

    public static MemberAdminResponse fromEntity(Member member) {
        return new MemberAdminResponse(
            member.getId(),
            member.getEmail(),
            member.getUsername(),
            member.getRole().toString(),
            member.getCreatedAt(),
            member.getUpdatedAt(),
            member.isDeleted(),
            member.getDeletedAt()
        );
    }
}
