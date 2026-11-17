package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoanhThuNgayResponse {
    private LocalDate ngay;
    private BigDecimal doanhThu;
    private long soHoaDon;
}
