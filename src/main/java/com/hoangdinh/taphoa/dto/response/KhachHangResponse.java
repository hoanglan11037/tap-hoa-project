package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangResponse {
    private Integer maKh;
    private String maThe;
    private String hoTen;
    private String sdt;
    private String diaChi;
    private int diemTichLuy;
    private LocalDateTime ngayTao;
    private long soLanMua;
    private BigDecimal tongChiTieu;
    private LocalDateTime lanMuaCuoi;
}
