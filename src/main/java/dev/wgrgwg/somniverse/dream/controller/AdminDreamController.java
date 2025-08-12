package dev.wgrgwg.somniverse.dream.controller;

import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dreams")
@RequiredArgsConstructor
@Slf4j
public class AdminDreamController {

    private final DreamService dreamService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<DreamSimpleResponse>>> getDreamsForAdmin(
        Pageable pageable, @RequestParam(defaultValue = "false") Boolean includeDeleted) {

        Page<DreamSimpleResponse> response = dreamService.getAllDreamsForAdmin(pageable,
            includeDeleted);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @GetMapping("/{dreamId}")
    public ResponseEntity<ApiResponseDto<DreamResponse>> getDreamAsAdmin(@PathVariable Long dreamId,
        @RequestParam(defaultValue = "false") Boolean includeDeleted) {
        DreamResponse response = dreamService.getDreamAsAdmin(dreamId, includeDeleted);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @DeleteMapping("/{dreamId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteDreamByAdmin(@PathVariable Long dreamId) {
        dreamService.deleteDreamByAdmin(dreamId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
