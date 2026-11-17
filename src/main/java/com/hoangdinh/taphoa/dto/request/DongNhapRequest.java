package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một dòng hàng trên phiếu nhập. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DongNhapRequest {
    @NotNull(message = "Thiếu sản phẩm")
    private Integer maSp;

    @NotNull(message = "Nhập số lượng")
    @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
    private Integer soLuong;

    @NotNull(message = "Nhập đơn giá nhập")
    @PositiveOrZero(message = "Đơn giá nhập không được âm")
    private BigDecimal donGia;
}
