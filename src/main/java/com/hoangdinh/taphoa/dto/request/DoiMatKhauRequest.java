package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoiMatKhauRequest {
    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String matKhauCu;

    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 6, max = 50, message = "Mật khẩu mới từ 6 đến 50 ký tự")
    private String matKhauMoi;
}
