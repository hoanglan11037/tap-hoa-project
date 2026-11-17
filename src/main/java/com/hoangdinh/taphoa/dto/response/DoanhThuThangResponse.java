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
public class DoanhThuThangResponse {
    private int thang;
    private BigDecimal doanhThu;
    private BigDecimal loiNhuan;
    private long soHoaDon;
}
