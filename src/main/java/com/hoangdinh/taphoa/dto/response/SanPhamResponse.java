package com.hoangdinh.taphoa.dto.response;

import com.hoangdinh.taphoa.enums.TinhTrangKho;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamResponse {
    private Integer maSp;
    /** Mã hàng do CSDL sinh: SP0001... */
    private String maHang;
    private String maVach;
    private String tenSp;
    private Integer maDm;
    private String tenDm;
    private String donVi;
    private BigDecimal giaBan;
    private BigDecimal giaNhap;
    private int tonKho;
    private int tonToiThieu;
    private boolean dangBan;
    private TinhTrangKho tinhTrang;
    private LocalDateTime ngayTao;
}
