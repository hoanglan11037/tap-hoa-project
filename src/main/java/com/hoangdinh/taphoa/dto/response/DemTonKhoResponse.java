package com.hoangdinh.taphoa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Số sản phẩm theo tình trạng tồn kho cho các nút lọc. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemTonKhoResponse {
    private long tatCa;
    private long conHang;
    private long sapHet;
    private long hetHang;
    private long ngungBan;
}
