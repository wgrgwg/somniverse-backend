package dev.wgrgwg.somniverse.dream.dto.response;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record DreamResponse(
    Long id,
    String title,
    String content,
    LocalDate dreamDate,
    boolean isPublic,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    MemberResponse author,
    boolean isDeleted
) {

    public static DreamResponse fromEntity(Dream dream) {
        return new DreamResponse(
            dream.getId(),
            dream.getTitle(),
            dream.getContent(),
            dream.getDreamDate(),
            dream.isPublic(),
            dream.getCreatedAt(),
            dream.getUpdatedAt(),
            MemberResponse.fromEntity(dream.getMember()),
            dream.isDeleted()
        );
    }
}
