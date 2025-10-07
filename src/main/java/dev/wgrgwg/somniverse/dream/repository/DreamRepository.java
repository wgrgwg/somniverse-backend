package dev.wgrgwg.somniverse.dream.repository;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DreamRepository extends JpaRepository<Dream, Long> {

    @Query("""
        SELECT d
        FROM Dream d
        JOIN FETCH d.member
        """)
    Page<Dream> findAllForAdmin(Pageable pageable);

    Page<Dream> findAllByMemberIdAndIsDeletedFalse(Long memberId, Pageable pageable);

    Page<Dream> findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(Long memberId, Pageable pageable);

    @Query("""
        SELECT d
        FROM Dream d
        JOIN FETCH d.member
        WHERE d.isPublic=true
        AND d.isDeleted=false
        """)
    Page<Dream> findAllByIsPublicTrueAndIsDeletedFalse(Pageable pageable);

    @Query("""
        SELECT d
        FROM Dream d
        JOIN FETCH d.member
        WHERE d.isDeleted=false
        """)
    Page<Dream> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Dream> findByIdAndIsDeletedFalse(Long id);
}
