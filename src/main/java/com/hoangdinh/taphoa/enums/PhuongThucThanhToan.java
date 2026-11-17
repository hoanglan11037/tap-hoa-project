package com.hoangdinh.taphoa.enums;

/** Hình thức khách trả tiền. */
public enum PhuongThucThanhToan {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản"),
    THE("Quẹt thẻ");

    private final String label;

    PhuongThucThanhToan(String label) {
        this.label = label;
    }

    /** Nhãn tiếng Việt để hiển thị trên giao diện. */
    public String getLabel() {
        return label;
    }
}
