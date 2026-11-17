package com.hoangdinh.taphoa.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhieuNhapRequest {
    @NotNull(message = "Chọn nhà cung cấp")
    private Integer maNcc;

    @Valid
    @NotNull(message = "Phiếu nhập chưa có dòng hàng nào")
    private List<DongNhapRequest> items;

    @Size(max = 200, message = "Ghi chú tối đa 200 ký tự")
    private String ghiChu;
}
