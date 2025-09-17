package dev.wgrgwg.somniverse.comment.controller;

import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCommentByAdmin(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
