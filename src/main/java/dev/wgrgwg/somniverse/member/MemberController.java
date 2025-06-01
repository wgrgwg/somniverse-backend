package dev.wgrgwg.somniverse.member;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberSignupRequestDto;
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

    @PostMapping("/signup")
    public ResponseEntity<ApiResponseDto<MemberResponseDto>> signup(@Valid @RequestBody
    MemberSignupRequestDto memberSignupRequestDto) {
        MemberResponseDto memberResponseDto = memberService.signup(memberSignupRequestDto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponseDto.success("회원가입이 완료되었습니다.", memberResponseDto));
    }
}
