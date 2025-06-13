package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.RefreshToken;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
}
