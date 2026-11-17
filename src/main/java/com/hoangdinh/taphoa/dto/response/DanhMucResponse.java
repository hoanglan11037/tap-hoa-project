package com.hoangdinh.taphoa.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanhMucResponse {
    private Integer maDm;
    private String tenDm;
    private String moTa;
    private long soSanPham;
    /** Tổng giá trị hàng tồn của danh mục (theo giá nhập). */
    private BigDecimal giaTriTon;
}
