package dev.wgrgwg.somniverse.member.repository;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Provider;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmailAndProvider(String email, Provider provider);

    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);

    Page<Member> findByEmailContainingOrUsernameContaining(String email, String username,
        Pageable pageable);
}
