package com.hoangdinh.taphoa.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NhaCungCapResponse {
    private Integer maNcc;
    private String tenNcc;
    private String nguoiLienHe;
    private String sdt;
    private String diaChi;
    private boolean trangThai;
    private long soPhieuNhap;
    private LocalDateTime lanNhapCuoi;
}
