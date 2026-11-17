package com.hoangdinh.taphoa.dto.request;

import com.hoangdinh.taphoa.enums.VaiTro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tạo / cập nhật tài khoản nhân viên. Khi cập nhật, bỏ trống mật khẩu để giữ nguyên. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoanRequest {
    @NotBlank(message = "Nhập tên đăng nhập")
    @Pattern(regexp = "[a-zA-Z0-9._]{3,50}", message = "Tên đăng nhập 3–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới")
    private String tenDangNhap;

    @Size(min = 6, max = 50, message = "Mật khẩu từ 6 đến 50 ký tự")
    private String matKhau;

    @NotBlank(message = "Nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @NotNull(message = "Chọn vai trò")
    private VaiTro vaiTro;

    @Builder.Default
    private boolean trangThai = true;
}
