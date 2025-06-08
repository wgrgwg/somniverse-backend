package dev.wgrgwg.somniverse.security.userdetails;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import dev.wgrgwg.somniverse.security.exception.SecurityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email).orElseThrow(
            () -> new UsernameNotFoundException(SecurityErrorCode.EMAIL_NOT_FOUND.getMessage()));

        return new CustomUserDetails(member);
    }
}
