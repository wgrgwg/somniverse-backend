package dev.wgrgwg.somniverse.dream.service;

import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.request.DreamUpdateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.exception.DreamErrorCode;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DreamService {

    private final DreamRepository dreamRepository;
    private final MemberService memberService;

    @Transactional
    public DreamResponse createDream(DreamCreateRequest request, Long memberId) {
        Member currentMember = memberService.findById(memberId);

        Dream dream = Dream.builder().title(request.title())
            .content(request.content()).dreamDate(request.dreamDate())
            .isPublic(request.isPublic()).member(currentMember).build();

        Dream savedDream = dreamRepository.save(dream);

        return DreamResponse.fromEntity(savedDream);
    }

    @Transactional(readOnly = true)
    public DreamResponse getMyDream(Long dreamId, Long memberId) {
        Dream dream = getDreamOrThrow(dreamId);
        validateOwner(dream, memberId);

        return DreamResponse.fromEntity(dream);
    }

    @Transactional(readOnly = true)
    public DreamResponse getDreamWithAccessControl(Long dreamId, Long requesterId,
        boolean isAdmin) {
        Dream dream = getDreamOrThrow(dreamId);

        boolean isOwner = dream.getMember().getId().equals(requesterId);
        if (!dream.isPublic() && !isOwner && !isAdmin) {
            throw new CustomException(DreamErrorCode.DREAM_FORBIDDEN);
        }

        return DreamResponse.fromEntity(dream);
    }

    @Transactional(readOnly = true)
    public DreamResponse getDreamAsAdmin(Long dreamId, boolean includeDeleted) {
        Dream dream = dreamRepository.findById(dreamId)
            .orElseThrow(() -> new CustomException(DreamErrorCode.DREAM_NOT_FOUND));

        if (!includeDeleted && dream.isDeleted()) {
            throw new CustomException(DreamErrorCode.DREAM_NOT_FOUND);
        }

        return DreamResponse.fromEntity(dream);
    }

    @Transactional(readOnly = true)
    public Page<DreamSimpleResponse> getMyDreams(Long memberId, Pageable pageable) {
        return dreamRepository.findAllByMemberIdAndIsDeletedFalse(memberId, pageable)
            .map(DreamSimpleResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DreamSimpleResponse> getPublicDreams(Pageable pageable) {
        return dreamRepository.findAllByIsPublicTrueAndIsDeletedFalse(pageable)
            .map(DreamSimpleResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DreamSimpleResponse> getPublicDreamsByMember(Long memberId, Pageable pageable) {
        return dreamRepository.findAllByMemberIdAndIsDeletedFalseAndIsPublicTrue(memberId, pageable)
            .map(DreamSimpleResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DreamSimpleResponse> getAllDreamsForAdmin(Pageable pageable,
        boolean includeDeleted) {

        if (includeDeleted) {
            return dreamRepository.findAll(pageable).map(DreamSimpleResponse::fromEntity);
        }

        return dreamRepository.findAllByIsDeletedFalse(pageable)
            .map(DreamSimpleResponse::fromEntity);
    }

    @Transactional
    DreamResponse updateDream(Long dreamId, Long memberId, DreamUpdateRequest request) {
        Dream dream = getDreamOrThrow(dreamId);
        validateOwner(dream, memberId);

        dream.update(request.title(), request.content(), request.dreamDate(), request.isPublic());

        return DreamResponse.fromEntity(dream);
    }

    @Transactional
    public void deleteDream(Long dreamId, Long memberId) {
        Dream dream = getDreamOrThrow(dreamId);
        validateOwner(dream, memberId);

        dream.softDelete();
    }

    private Dream getDreamOrThrow(Long dreamId) {
        return dreamRepository.findByIdAndIsDeletedFalse(dreamId)
            .orElseThrow(() -> new CustomException(DreamErrorCode.DREAM_NOT_FOUND));
    }

    private void validateOwner(Dream dream, Long memberId) {
        if (!Objects.equals(dream.getMember().getId(), memberId)) {
            throw new CustomException(DreamErrorCode.DREAM_FORBIDDEN);
        }
    }
}
