package dev.wgrgwg.somniverse.member.controller;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.member.dto.request.MemberRoleUpdateRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberAdminResponse;
import dev.wgrgwg.somniverse.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN')")
@Slf4j
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<MemberAdminResponse>>> getAllMember(Pageable pageable,
        @RequestParam(required = false) String keyword) {
        Page<MemberAdminResponse> response = memberService.getAllMembersForAdmin(pageable, keyword);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponseDto<MemberAdminResponse>> getMember(
        @PathVariable Long memberId) {
        MemberAdminResponse response = memberService.getMemberForAdmin(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }
    
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<ApiResponseDto<MemberAdminResponse>> updateMemberRole(
        @PathVariable Long memberId, @Valid @RequestBody MemberRoleUpdateRequest request
    ) {
        MemberAdminResponse response = memberService.updateMemberRoleByAdmin(memberId, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }
}
