package dev.wgrgwg.somniverse.comment.controller;

import dev.wgrgwg.somniverse.comment.dto.request.CommentCreateRequest;
import dev.wgrgwg.somniverse.comment.dto.request.CommentUpdateRequest;
import dev.wgrgwg.somniverse.comment.dto.response.CommentResponse;
import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/dreams/{dreamId}/comments")
    public ResponseEntity<ApiResponseDto<CommentResponse>> createComment(@PathVariable Long dreamId,
        @Valid @RequestBody CommentCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        CommentResponse response = commentService.createComment(dreamId, request, memberId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping("/dreams/{dreamId}/comments")
    public ResponseEntity<ApiResponseDto<Page<CommentResponse>>> getParentsComments(
        @PathVariable Long dreamId, @AuthenticationPrincipal CustomUserDetails userDetails,
        Pageable pageable) {
        boolean isAdmin = userDetails.isAdmin();

        Page<CommentResponse> response = commentService.getPagedParentCommentsByDream(dreamId,
            isAdmin, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @GetMapping("/comments/{commentId}/children")
    public ResponseEntity<ApiResponseDto<Page<CommentResponse>>> getChildrenComments(
        @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails,
        Pageable pageable) {
        boolean isAdmin = userDetails.isAdmin();

        Page<CommentResponse> response = commentService.getPagedChildrenCommentsByParent(commentId,
            isAdmin, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponseDto<CommentResponse>> updateComment(
        @PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        CommentResponse response = commentService.updateComment(commentId, request, memberId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.success(response));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteComment(@PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();

        commentService.deleteComment(commentId, memberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/comments/admin/{commentId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCommentByAdmin(@PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.deleteCommentByAdmin(commentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
