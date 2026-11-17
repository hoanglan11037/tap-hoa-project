package com.hoangdinh.taphoa.enums;

/** Vai trò tài khoản: chủ cửa hàng dùng mọi chức năng, thu ngân dùng bán hàng – kho – khách hàng. */
public enum VaiTro {
    ADMIN("Chủ cửa hàng"),
    THU_NGAN("Thu ngân");

    private final String label;

    VaiTro(String label) {
        this.label = label;
    }

    /** Nhãn tiếng Việt để hiển thị trên giao diện. */
    public String getLabel() {
        return label;
    }
}
