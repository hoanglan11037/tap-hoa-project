package com.hoangdinh.taphoa.enums;

/** Hóa đơn hủy được giữ lại để đối chiếu, hàng được hoàn về kho. */
public enum TrangThaiHoaDon {
    HOAN_THANH("Hoàn thành"),
    DA_HUY("Đã hủy");

    private final String label;

    TrangThaiHoaDon(String label) {
        this.label = label;
    }

    /** Nhãn tiếng Việt để hiển thị trên giao diện. */
    public String getLabel() {
        return label;
    }
}
