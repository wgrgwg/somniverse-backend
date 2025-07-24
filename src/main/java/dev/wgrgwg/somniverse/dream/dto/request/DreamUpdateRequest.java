package dev.wgrgwg.somniverse.dream.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DreamUpdateRequest(
    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다.")
    String title,

    @NotBlank(message = "본문은 필수 입력 항목입니다.")
    String content,

    @NotNull(message = "꿈을 꾼 날짜는 필수 입력 항목입니다.")
    @FutureOrPresent(message = "꿈을 꾼 날짜는 현재 또는 미래일 수 없습니다.")
    LocalDate dreamDate,

    @NotNull(message = "공개 여부는 필수 입력 항목입니다.")
    boolean isPublic
) {

}
