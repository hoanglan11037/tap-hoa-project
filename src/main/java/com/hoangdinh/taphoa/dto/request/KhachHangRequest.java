package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangRequest {
    @NotBlank(message = "Nhập họ tên khách hàng")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Nhập số điện thoại")
    @Pattern(regexp = "0\\d{9,10}", message = "Số điện thoại gồm 10–11 chữ số, bắt đầu bằng 0")
    private String sdt;

    @Size(max = 200, message = "Địa chỉ tối đa 200 ký tự")
    private String diaChi;
}
