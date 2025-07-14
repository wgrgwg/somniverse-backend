package dev.wgrgwg.somniverse.dream.repository;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DreamRepository extends JpaRepository<Dream, Long> {

    Page<Dream> findAllByMemberIdAndIsDeletedFalse(Long memberId, Pageable pageable);

    Page<Dream> findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(Long memberId, Pageable pageable);

    Page<Dream> findAllByIsPublicTrueAndIsDeletedFalse(Pageable pageable);

    Optional<Dream> findByIdAndIsDeletedFalse(Long id);
}
