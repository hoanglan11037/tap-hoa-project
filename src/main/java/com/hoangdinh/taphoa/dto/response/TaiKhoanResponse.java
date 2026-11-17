package com.hoangdinh.taphoa.dto.response;

import com.hoangdinh.taphoa.enums.VaiTro;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoanResponse {
    private Integer maTk;
    private String tenDangNhap;
    private String hoTen;
    private VaiTro vaiTro;
    private boolean trangThai;
    private LocalDateTime ngayTao;
}
