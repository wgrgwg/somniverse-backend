package dev.wgrgwg.somniverse.security.oauth.service;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Provider;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.security.oauth.exception.OAuthErrorCode;
import dev.wgrgwg.somniverse.security.oauth.exception.OAuthException;
import dev.wgrgwg.somniverse.security.oauth.user.CustomOAuth2User;
import dev.wgrgwg.somniverse.security.oauth.user.OAuth2UserInfo;
import dev.wgrgwg.somniverse.security.oauth.user.OAuth2UserInfoFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String TEMPLATE = "%s_%s";
    private static final int SUFFIX_LENGTH = 8;
    private static final int MAX_RETRIES = 5;

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();

        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oauth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId,
            oAuth2User.getAttributes());

        Member member = processOAuth2User(oauth2UserInfo, registrationId);

        return new CustomOAuth2User(member, oAuth2User.getAttributes());
    }

    private Member processOAuth2User(OAuth2UserInfo oauth2UserInfo, String registrationId) {
        Provider provider = Provider.fromString(registrationId);

        Optional<Member> existingMemberByProvider = memberRepository.findByProviderAndProviderId(
            provider, oauth2UserInfo.getProviderId());

        if (existingMemberByProvider.isPresent()) {
            return existingMemberByProvider.get();
        }

        String username = generateUniqueUsername(oauth2UserInfo.getName());

        Member newMember = Member.builder()
            .email(oauth2UserInfo.getEmail())
            .username(username)
            .role(Role.USER)
            .provider(provider)
            .providerId(oauth2UserInfo.getProviderId())
            .build();

        Member savedMember = memberRepository.save(newMember);

        log.info("새로운 OAuth 사용자 생성: memberId={}, email={}, provider={}",
            savedMember.getId(), savedMember.getEmail(), provider.name());

        return savedMember;
    }

    private String generateUniqueUsername(String baseName) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String suffix = UUID.randomUUID().toString().substring(0, SUFFIX_LENGTH);
            String candidate = String.format(TEMPLATE, baseName, suffix);

            if (!memberRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new OAuthException(OAuthErrorCode.INVALID_USERNAME);
    }
}
