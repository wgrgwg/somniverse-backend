package dev.wgrgwg.somniverse.comment.repository;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    Page<Comment> findAllByParentId(Long parentId, Pageable pageable);

    @Query("""
        SELECT c
        FROM Comment c
        JOIN FETCH c.member
        WHERE c.dream.id = :dreamId
        AND c.parent IS NULL
        """)
    Page<Comment> findAllByDreamIdAndParentIsNull(Long dreamId, Pageable pageable);

    @Query("SELECT c.parent.id, COUNT(c) " +
        "FROM Comment c " +
        "WHERE c.parent.id IN :parentIds AND c.isDeleted = false " +
        "GROUP BY c.parent.id")
    List<Object[]> countChildrenGroupedByParentId(@Param("parentIds") List<Long> parentIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.isDeleted = true, c.deletedAt = :deletedAt WHERE c.dream.id = :dreamId")
    void softDeleteByDream(@Param("dreamId") Long dreamId,
        @Param("deletedAt") LocalDateTime deletedAt);
}
