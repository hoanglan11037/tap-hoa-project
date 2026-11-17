package com.hoangdinh.taphoa.enums;

/** Tình trạng tồn kho của sản phẩm (tính trong view vwSanPham). */
public enum TinhTrangKho {
    CON_HANG("Còn hàng"),
    SAP_HET("Sắp hết"),
    HET_HANG("Hết hàng"),
    NGUNG_BAN("Ngừng bán");

    private final String label;

    TinhTrangKho(String label) {
        this.label = label;
    }

    /** Nhãn tiếng Việt để hiển thị trên giao diện. */
    public String getLabel() {
        return label;
    }
}
