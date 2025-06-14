package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.RefreshToken;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
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

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken refreshTokenFromDB = refreshTokenRepository.findByValue(refreshToken)
            .orElseThrow(() -> new CustomException(MemberErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Member member = refreshTokenFromDB.getMember();
        if (member == null) {
            throw new CustomException(MemberErrorCode.MEMBER_FOR_TOKEN_NOT_FOUND);
        }

        TokenResponse newTokens = jwtProvider.generateToken(member);

        refreshTokenFromDB.updateValue(newTokens.refreshToken());

        return new TokenResponse(newTokens.accessToken(), newTokens.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByValue(refreshToken).ifPresent(refreshTokenRepository::delete);
    }
}
