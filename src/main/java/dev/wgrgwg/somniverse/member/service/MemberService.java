package dev.wgrgwg.somniverse.member.service;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Provider;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.MemberRoleUpdateRequest;
import dev.wgrgwg.somniverse.member.dto.request.MemberUsernameUpdateRequest;
import dev.wgrgwg.somniverse.member.dto.request.SignupRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberAdminResponse;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signup(SignupRequest signupRequest) {
        if (memberRepository.existsByEmailAndProvider(signupRequest.email(), Provider.LOCAL)) {
            throw new CustomException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByUsername(signupRequest.username())) {
            throw new CustomException(MemberErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(signupRequest.password());

        Member newMember = Member.builder().username(signupRequest.username())
            .email(signupRequest.email()).password(encodedPassword).role(Role.USER)
            .provider(Provider.LOCAL).build();

        Member savedMember = memberRepository.save(newMember);

        return MemberResponse.fromEntity(savedMember);
    }

    @Transactional(readOnly = true)
    public Page<MemberAdminResponse> getAllMembersForAdmin(Pageable pageable, String keyword) {
        if (keyword == null) {
            return memberRepository.findAll(pageable).map(MemberAdminResponse::fromEntity);
        }

        return memberRepository.findByEmailContainingOrUsernameContaining(keyword, keyword,
            pageable).map(MemberAdminResponse::fromEntity);
    }

    @Transactional
    public MemberAdminResponse updateMemberRoleByAdmin(Long memberId,
        MemberRoleUpdateRequest request) {
        Member member = getMemberOrThrow(memberId);

        member.updateRole(request.role());

        return MemberAdminResponse.fromEntity(member);
    }

    @Transactional
    public MemberResponse updateMemberUsername(Long memberId, MemberUsernameUpdateRequest request) {
        Member member = getMemberOrThrow(memberId);

        if (memberRepository.existsByUsername(request.username())) {
            throw new CustomException(MemberErrorCode.USERNAME_ALREADY_EXISTS);
        }

        member.updateUsername(request.username());

        return MemberResponse.fromEntity(member);
    }

    @Transactional(readOnly = true)
    public MemberAdminResponse getMemberForAdmin(Long memberId) {
        Member member = getMemberOrThrow(memberId);

        return MemberAdminResponse.fromEntity(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.fromEntity(getMemberOrThrow(memberId));
    }

    public Member getMemberOrThrow(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
