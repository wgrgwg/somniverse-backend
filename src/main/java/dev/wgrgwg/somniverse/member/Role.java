package dev.wgrgwg.somniverse.member;

public enum Role {
    ADMIN("책임자"),
    MANAGER("관리자"),
    USER("사용자");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
