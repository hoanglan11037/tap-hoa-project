package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thêm / sửa sản phẩm. Tồn kho chỉ nhập khi thêm mới; về sau thay đổi qua bán hàng và nhập hàng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamRequest {
    @Pattern(regexp = "\\d{6,20}", message = "Mã vạch gồm 6–20 chữ số")
    private String maVach;

    @NotBlank(message = "Nhập tên sản phẩm")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String tenSp;

    @NotNull(message = "Chọn danh mục")
    private Integer maDm;

    @NotBlank(message = "Nhập đơn vị tính")
    @Size(max = 20, message = "Đơn vị tính tối đa 20 ký tự")
    private String donVi;

    @NotNull(message = "Nhập giá bán")
    @PositiveOrZero(message = "Giá bán không được âm")
    private BigDecimal giaBan;

    @PositiveOrZero(message = "Giá nhập không được âm")
    private BigDecimal giaNhap;

    @Min(value = 0, message = "Tồn kho ban đầu không được âm")
    private Integer tonKho;

    @NotNull(message = "Nhập mức tồn tối thiểu")
    @Min(value = 0, message = "Mức tồn tối thiểu không được âm")
    private Integer tonToiThieu;

    @Builder.Default
    private boolean dangBan = true;
}
