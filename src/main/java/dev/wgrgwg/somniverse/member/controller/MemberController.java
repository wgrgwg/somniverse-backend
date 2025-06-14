package dev.wgrgwg.somniverse.member.controller;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.member.dto.request.SignupRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.message.MemberSuccessMessage;
import dev.wgrgwg.somniverse.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members")
    public ResponseEntity<ApiResponseDto<MemberResponse>> signup(@Valid @RequestBody
    SignupRequest signupRequest) {
        MemberResponse memberResponse = memberService.signup(signupRequest);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponseDto.success(MemberSuccessMessage.SIGNUP_SUCCESS.getMessage(),
                memberResponse));
    }
}
