package dev.wgrgwg.somniverse.comment.repository;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    Page<Comment> findAllByDreamIdAndParentIsNullAndIsDeletedFalse(Long dreamId, Pageable pageable);

    Page<Comment> findAllByParentIdAndIsDeletedFalse(Long parentId, Pageable pageable);

    Page<Comment> findAllByMemberIdAndIsDeletedFalse(Long memberId, Pageable pageable);
}
