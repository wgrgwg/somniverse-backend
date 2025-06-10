package dev.wgrgwg.somniverse.security.jwt.repository;

import dev.wgrgwg.somniverse.security.jwt.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByValue(String value);

    List<RefreshToken> findAllByMemberId(Long memberId);
}
