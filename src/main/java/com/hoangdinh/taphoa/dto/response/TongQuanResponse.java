package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TongQuanResponse {
    private BigDecimal doanhThuHomNay;
    private BigDecimal doanhThuHomQua;
    private long hoaDonHomNay;
    private BigDecimal loiNhuanHomNay;
    private long sapHetHang;
    private long hetHang;
    private long khachHangMoiThangNay;
    private List<DoanhThuNgayResponse> doanhThu7Ngay;
    private List<SanPhamResponse> canNhapThem;
    private List<HoaDonResponse> hoaDonGanDay;
    private List<TopSanPhamResponse> banChayHomNay;
}
