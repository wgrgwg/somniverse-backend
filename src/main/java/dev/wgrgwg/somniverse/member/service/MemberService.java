package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.MemberLoginRequestDto;
import dev.wgrgwg.somniverse.member.dto.MemberResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberSignupRequestDto;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.security.jwt.domain.RefreshToken;
import dev.wgrgwg.somniverse.security.jwt.dto.TokenDto;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.jwt.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public MemberResponseDto signup(MemberSignupRequestDto memberSignupRequestDto) {
        if (memberRepository.existsByEmail(memberSignupRequestDto.email())) {
            throw new CustomException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByUsername(memberSignupRequestDto.username())) {
            throw new CustomException(MemberErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(memberSignupRequestDto.password());

        Member newMember = Member.builder()
            .username(memberSignupRequestDto.username())
            .email(memberSignupRequestDto.email())
            .password(encodedPassword)
            .role(Role.USER)
            .build();

        Member savedMember = memberRepository.save(newMember);

        return MemberResponseDto.fromEntity(savedMember);
    }

    public TokenDto login(MemberLoginRequestDto memberLoginRequestDto) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            memberLoginRequestDto.email(), memberLoginRequestDto.password());

        Authentication authentication = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Member member = userDetails.getMember();

        TokenDto tokenDto = jwtProvider.generateToken(member);

        RefreshToken refreshToken = RefreshToken.builder()
            .member(member)
            .value(tokenDto.refreshToken())
            .build();

        refreshTokenRepository.save(refreshToken);

        return tokenDto;
    }
}
