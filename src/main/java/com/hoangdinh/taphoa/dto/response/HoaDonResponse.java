package com.hoangdinh.taphoa.dto.response;

import com.hoangdinh.taphoa.enums.PhuongThucThanhToan;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoaDonResponse {
    private Integer maHd;
    private String soHd;
    private Integer maKh;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String thuNgan;
    private LocalDateTime ngayLap;
    private BigDecimal tongTien;
    private BigDecimal giamGia;
    /** Khách phải trả = tổng tiền hàng - giảm giá. */
    private BigDecimal phaiTra;
    private BigDecimal khachTra;
    private BigDecimal tienThua;
    private PhuongThucThanhToan phuongThuc;
    private TrangThaiHoaDon trangThai;
    private String ghiChu;
    private int soMatHang;
    /** Chỉ có khi lấy chi tiết một hóa đơn. */
    private List<ChiTietHoaDonResponse> chiTiet;
}
