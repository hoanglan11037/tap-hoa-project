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
public class DanhMucRequest {
    @NotBlank(message = "Nhập tên danh mục")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String tenDm;

    @Size(max = 300, message = "Mô tả tối đa 300 ký tự")
    private String moTa;
}
