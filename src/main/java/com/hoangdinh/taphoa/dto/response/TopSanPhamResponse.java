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
public class TopSanPhamResponse {
    private String maHang;
    private String tenSp;
    private String donVi;
    private long soLuong;
    private BigDecimal doanhThu;
}
