package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.AccessTokenBlackListRepository;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import java.util.Optional;
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
    private final MemberService memberService;
    private final AccessTokenBlackListRepository accessTokenBlackListRepository;

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Member member = userDetails.getMember();

        TokenResponse tokenResponse = jwtProvider.generateToken(member);

        refreshTokenRepository.save(tokenResponse.refreshToken(), member.getId().toString());

        return tokenResponse;
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        String memberId = refreshTokenRepository.findMemberIdByToken(refreshToken)
            .orElseThrow(() -> new CustomException(MemberErrorCode.TOKEN_NOT_FOUND));

        Member member = memberService.findById(Long.parseLong(memberId));

        TokenResponse newTokens = jwtProvider.generateToken(member);

        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.save(newTokens.refreshToken(), memberId);

        return new TokenResponse(newTokens.accessToken(), newTokens.refreshToken());
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        Optional<String> memberIdOpt = refreshTokenRepository.findMemberIdByToken(refreshToken);

        if (memberIdOpt.isEmpty()) {
            throw new CustomException(MemberErrorCode.TOKEN_NOT_FOUND);
        }

        refreshTokenRepository.delete(refreshToken);

        long remainingExpirationMillis = jwtProvider.getRemainingExpirationMillis(accessToken);
        accessTokenBlackListRepository.save(accessToken, remainingExpirationMillis);
    }
}
