package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietPhieuNhapResponse {
    private Integer maSp;
    private String maHang;
    private String tenSp;
    private String donVi;
    private int soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
}
