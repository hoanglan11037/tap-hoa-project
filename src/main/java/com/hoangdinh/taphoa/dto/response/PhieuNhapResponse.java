package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhieuNhapResponse {
    private Integer maPn;
    private String soPn;
    private Integer maNcc;
    private String tenNcc;
    private String nguoiNhap;
    private LocalDateTime ngayNhap;
    private String ghiChu;
    private int soMatHang;
    private BigDecimal tongTien;
    /** Chỉ có khi lấy chi tiết một phiếu. */
    private List<ChiTietPhieuNhapResponse> chiTiet;
}
