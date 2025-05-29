package dev.wgrgwg.somniverse.member;

import dev.wgrgwg.somniverse.member.dto.MemberResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberSignupRequestDto;
import dev.wgrgwg.somniverse.member.exception.EmailAlreadyExistsException;
import dev.wgrgwg.somniverse.member.exception.UsernameAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public MemberResponseDto signup(MemberSignupRequestDto memberSignupRequestDto) {
        if (memberRepository.existsByEmail(memberSignupRequestDto.email())) {
            throw new EmailAlreadyExistsException();
        }

        if (memberRepository.existsByUsername(memberSignupRequestDto.username())) {
            throw new UsernameAlreadyExistsException();
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
}
