package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.dao.HoaDonDao;
import com.hoangdinh.taphoa.dao.KhachHangDao;
import com.hoangdinh.taphoa.dao.SanPhamDao;
import com.hoangdinh.taphoa.dto.response.DoanhThuNgayResponse;
import com.hoangdinh.taphoa.dto.response.DoanhThuThangResponse;
import com.hoangdinh.taphoa.dto.response.TongQuanResponse;
import com.hoangdinh.taphoa.dto.response.TopSanPhamResponse;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.security.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Số liệu màn hình Tổng quan và báo cáo doanh thu – lợi nhuận. */
public class ThongKeService {

    private static final int SO_NGAY_BIEU_DO = 7;
    private static final int SO_DONG_TONG_QUAN = 6;
    private static final int SO_TOP_SAN_PHAM = 10;

    private final HoaDonDao hoaDonDao;
    private final SanPhamDao sanPhamDao;
    private final KhachHangDao khachHangDao;

    public ThongKeService(HoaDonDao hoaDonDao, SanPhamDao sanPhamDao, KhachHangDao khachHangDao) {
        this.hoaDonDao = hoaDonDao;
        this.sanPhamDao = sanPhamDao;
        this.khachHangDao = khachHangDao;
    }

    public TongQuanResponse tongQuan() {
        Session.requireLogin();
        LocalDate today = LocalDate.now();
        LocalDateTime dauNgay = today.atStartOfDay();
        LocalDateTime cuoiNgay = today.plusDays(1).atStartOfDay();
        Map<TinhTrangKho, Long> kho = sanPhamDao.demTheoTinhTrang();

        return TongQuanResponse.builder()
                .doanhThuHomNay(hoaDonDao.doanhThu(dauNgay, cuoiNgay))
                .doanhThuHomQua(hoaDonDao.doanhThu(dauNgay.minusDays(1), dauNgay))
                .hoaDonHomNay(hoaDonDao.demHoaDon(dauNgay, cuoiNgay))
                .loiNhuanHomNay(hoaDonDao.loiNhuan(dauNgay, cuoiNgay))
                .sapHetHang(kho.get(TinhTrangKho.SAP_HET))
                .hetHang(kho.get(TinhTrangKho.HET_HANG))
                .khachHangMoiThangNay(khachHangDao.demMoiTu(today.withDayOfMonth(1).atStartOfDay()))
                .doanhThu7Ngay(doanhThuTheoNgay(today.minusDays(SO_NGAY_BIEU_DO - 1), today))
                .canNhapThem(sanPhamDao.findCanNhap(SO_DONG_TONG_QUAN))
                .hoaDonGanDay(hoaDonDao.findGanDay(SO_DONG_TONG_QUAN))
                .banChayHomNay(hoaDonDao.topSanPham(dauNgay, cuoiNgay, 5))
                .build();
    }

    /** Doanh thu từng ngày từ tuNgay đến denNgay (ngày không bán = 0). */
    private List<DoanhThuNgayResponse> doanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        Map<LocalDate, DoanhThuNgayResponse> co = hoaDonDao
                .doanhThuTheoNgay(tuNgay.atStartOfDay(), denNgay.plusDays(1).atStartOfDay()).stream()
                .collect(Collectors.toMap(DoanhThuNgayResponse::getNgay, Function.identity()));
        List<DoanhThuNgayResponse> result = new ArrayList<>();
        for (LocalDate d = tuNgay; !d.isAfter(denNgay); d = d.plusDays(1)) {
            result.add(co.getOrDefault(d, new DoanhThuNgayResponse(d, BigDecimal.ZERO, 0)));
        }
        return result;
    }

    /** Doanh thu, lợi nhuận 12 tháng của năm (tháng không bán = 0). Chỉ chủ cửa hàng. */
    public List<DoanhThuThangResponse> doanhThuNam(int nam) {
        Session.requireAdmin();
        List<DoanhThuThangResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(new DoanhThuThangResponse(m, BigDecimal.ZERO, BigDecimal.ZERO, 0));
        }
        for (DoanhThuThangResponse r : hoaDonDao.doanhThuTheoThang(nam)) {
            result.set(r.getThang() - 1, r);
        }
        return result;
    }

    /** Top sản phẩm bán chạy trong năm. Chỉ chủ cửa hàng. */
    public List<TopSanPhamResponse> topSanPham(int nam) {
        Session.requireAdmin();
        LocalDateTime from = LocalDate.of(nam, 1, 1).atStartOfDay();
        return hoaDonDao.topSanPham(from, from.plusYears(1), SO_TOP_SAN_PHAM);
    }
}
