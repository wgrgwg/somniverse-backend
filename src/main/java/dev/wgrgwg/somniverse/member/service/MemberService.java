package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.RefreshToken;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.request.SignupRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
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
    public MemberResponse signup(SignupRequest signupRequest) {
        if (memberRepository.existsByEmail(signupRequest.email())) {
            throw new CustomException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByUsername(signupRequest.username())) {
            throw new CustomException(MemberErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(signupRequest.password());

        Member newMember = Member.builder()
            .username(signupRequest.username())
            .email(signupRequest.email())
            .password(encodedPassword)
            .role(Role.USER)
            .build();

        Member savedMember = memberRepository.save(newMember);

        return MemberResponse.fromEntity(savedMember);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            loginRequest.email(), loginRequest.password());

        Authentication authentication = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Member member = userDetails.getMember();

        TokenResponse tokenResponse = jwtProvider.generateToken(member);

        RefreshToken refreshToken = RefreshToken.builder()
            .member(member)
            .value(tokenResponse.refreshToken())
            .build();

        refreshTokenRepository.save(refreshToken);

        return tokenResponse;
    }
}
