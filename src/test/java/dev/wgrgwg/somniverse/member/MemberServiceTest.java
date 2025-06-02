package dev.wgrgwg.somniverse.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.MemberResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberSignupRequestDto;
import dev.wgrgwg.somniverse.member.exception.EmailAlreadyExistsException;
import dev.wgrgwg.somniverse.member.exception.UsernameAlreadyExistsException;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.member.service.MemberService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 시 중복된 이메일이면 예외 발생")
    void signup_whenEmailAlreadyExists_shouldThrowException() {
        // given
        MemberSignupRequestDto dto = new MemberSignupRequestDto("user1", "user@email.com",
            "password");
        when(memberRepository.existsByEmail(dto.email())).thenReturn(true);

        // when
        // then
        Assertions.assertThatThrownBy(() -> memberService.signup(dto)).isInstanceOf(
            EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("회원가입 시 중복된 사용자명이면 예외 발생")
    void signup_whenUsernameAlreadyExists_shouldThrowException() {
        // given
        MemberSignupRequestDto dto = new MemberSignupRequestDto("user1", "user@email.com",
            "password");
        when(memberRepository.existsByUsername(dto.username())).thenReturn(true);

        // when
        // then
        Assertions.assertThatThrownBy(() -> memberService.signup(dto)).isInstanceOf(
            UsernameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("회원가입 성공 시 MemberResponseDto 반환")
    void signup_shouldReturnMemberResponseDto_whenSignupSuccess(){
        // given
        MemberSignupRequestDto dto = new MemberSignupRequestDto("user1", "user@email.com",
            "password");
        when(memberRepository.existsByEmail(dto.email())).thenReturn(false);
        when(memberRepository.existsByUsername(dto.username())).thenReturn(false);

        Member savedMember = Member.builder()
            .id(1L)
            .username(dto.username())
            .email(dto.email())
            .password("encoded-password")
            .role(Role.USER)
            .build();

        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

        // when
        MemberResponseDto responseDto = memberService.signup(dto);

        // then
        Assertions.assertThat(responseDto.email()).isEqualTo(dto.email());
        Assertions.assertThat(responseDto.username()).isEqualTo(dto.username());
        Assertions.assertThat(responseDto.role()).isEqualTo(Role.USER.toString());
    }
}