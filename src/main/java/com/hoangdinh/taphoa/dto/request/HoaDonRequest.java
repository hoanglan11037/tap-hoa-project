package com.hoangdinh.taphoa.dto.request;

import com.hoangdinh.taphoa.enums.PhuongThucThanhToan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thanh toán giỏ hàng tại quầy. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoaDonRequest {
    /** Khách hàng thân thiết (bỏ trống: khách lẻ). */
    private Integer maKh;

    @Valid
    @NotNull(message = "Giỏ hàng đang trống")
    private List<DongHangRequest> items;

    @PositiveOrZero(message = "Giảm giá không được âm")
    private BigDecimal giamGia;

    /** Tiền khách đưa (chỉ cần khi trả tiền mặt). */
    @PositiveOrZero(message = "Tiền khách đưa không được âm")
    private BigDecimal khachTra;

    @NotNull(message = "Chọn phương thức thanh toán")
    private PhuongThucThanhToan phuongThuc;

    @Size(max = 200, message = "Ghi chú tối đa 200 ký tự")
    private String ghiChu;
}
