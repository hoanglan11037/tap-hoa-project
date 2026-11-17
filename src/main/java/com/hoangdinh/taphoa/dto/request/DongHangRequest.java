package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một dòng trong giỏ hàng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DongHangRequest {
    @NotNull(message = "Thiếu sản phẩm")
    private Integer maSp;

    @NotNull(message = "Thiếu số lượng")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
}
