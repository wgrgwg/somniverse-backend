package dev.wgrgwg.somniverse.dream.controller;

import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.request.DreamUpdateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
@Slf4j
public class DreamController {

    private final DreamService dreamService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<DreamResponse>> createDream(
        @RequestBody DreamCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        DreamResponse response = dreamService.createDream(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(response));
    }

    @GetMapping("/{dreamId}")
    public ResponseEntity<ApiResponseDto<DreamResponse>> getDream(@PathVariable Long dreamId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        DreamResponse response = dreamService.getDreamWithAccessControl(dreamId, memberId,
            userDetails.isAdmin());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @PutMapping("/{dreamId}")
    public ResponseEntity<ApiResponseDto<DreamResponse>> updateDream(@PathVariable Long dreamId,
        @RequestBody DreamUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        DreamResponse response = dreamService.updateDream(dreamId, memberId, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @DeleteMapping("/{dreamId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteDream(@PathVariable Long dreamId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        dreamService.deleteDream(dreamId, memberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<Page<DreamSimpleResponse>>> getMyDreams(
        @AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        Long memberId = userDetails.getMember().getId();

        Page<DreamSimpleResponse> response = dreamService.getMyDreams(memberId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping("/my/{dreamId}")
    public ResponseEntity<ApiResponseDto<DreamResponse>> getMyDream(@PathVariable Long dreamId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        DreamResponse response = dreamService.getMyDream(dreamId, memberId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<DreamSimpleResponse>>> getDreams(Pageable pageable) {
        Page<DreamSimpleResponse> response = dreamService.getPublicDreams(pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponseDto<Page<DreamSimpleResponse>>> getDreamsByMember(
        @PathVariable Long memberId, Pageable pageable) {
        Page<DreamSimpleResponse> response = dreamService.getPublicDreamsByMember(memberId,
            pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }
}