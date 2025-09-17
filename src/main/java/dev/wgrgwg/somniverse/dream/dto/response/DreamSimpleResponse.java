package dev.wgrgwg.somniverse.dream.dto.response;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DreamSimpleResponse(
    Long id,
    String title,
    LocalDate dreamDate,
    LocalDateTime createdAt,
    String authorUsername
) {

    public static DreamSimpleResponse fromEntity(Dream dream) {
        return new DreamSimpleResponse(
            dream.getId(),
            dream.getTitle(),
            dream.getDreamDate(),
            dream.getCreatedAt(),
            dream.getMember().getUsername()
        );
    }
}
