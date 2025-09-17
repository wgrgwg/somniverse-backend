package dev.wgrgwg.somniverse.dream.domain;

import dev.wgrgwg.somniverse.comment.domain.Comment;
import dev.wgrgwg.somniverse.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Dream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;
    private String content;

    private LocalDate dreamDate;

    @OneToMany(mappedBy = "dream")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    private boolean isPublic;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean isDeleted;
    private LocalDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        this.isDeleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String content, LocalDate dreamDate, boolean isPublic) {
        this.title = title;
        this.content = content;
        this.dreamDate = dreamDate;
        this.isPublic = isPublic;
    }

    public void softDelete() {
        isDeleted = true;
        for (Comment comment : comments) {
            comment.softDelete();
        }
        deletedAt = LocalDateTime.now();
    }
}
