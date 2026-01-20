package dev.wgrgwg.somniverse.dream.domain;

public enum DreamAnalysisStatus {
    NONE("구 꿈일기"),
    PENDING("분석 대기"),
    COMPLETED("분석 완료"),
    FAILED("분석 실패");

    private final String label;

    DreamAnalysisStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
