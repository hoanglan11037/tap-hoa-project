package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.config.Database;
import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dao.HoaDonDao;
import com.hoangdinh.taphoa.dao.KhachHangDao;
import com.hoangdinh.taphoa.dao.SanPhamDao;
import com.hoangdinh.taphoa.dto.request.DongHangRequest;
import com.hoangdinh.taphoa.dto.request.HoaDonRequest;
import com.hoangdinh.taphoa.dto.response.ChiTietHoaDonResponse;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.enums.PhuongThucThanhToan;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bán hàng tại quầy: lập hóa đơn, tra cứu và hủy hóa đơn. */
public class BanHangService {

    /** Mỗi 10.000 đ khách thân thiết được 1 điểm. */
    public static final BigDecimal TIEN_MOI_DIEM = BigDecimal.valueOf(10_000);
    private static final int SO_HOA_DON_TOI_DA = 500;

    private final HoaDonDao hoaDonDao;
    private final SanPhamDao sanPhamDao;
    private final KhachHangDao khachHangDao;

    public BanHangService(HoaDonDao hoaDonDao, SanPhamDao sanPhamDao, KhachHangDao khachHangDao) {
        this.hoaDonDao = hoaDonDao;
        this.sanPhamDao = sanPhamDao;
        this.khachHangDao = khachHangDao;
    }

    /**
     * Thanh toán giỏ hàng. Trong <b>một giao dịch</b>: kiểm tra từng mặt hàng (đang bán, đủ tồn kho), lập hóa đơn,
     * ghi chi tiết với giá bán và giá vốn hiện tại, trừ tồn kho, cộng điểm cho khách. Lỗi ở bước nào thì không ghi gì.
     */
    public HoaDonResponse thanhToan(HoaDonRequest req) {
        int maTk = Session.current().getMaTk();
        Validation.check(req);
        if (req.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.GIO_HANG_TRONG);
        }
        Set<Integer> daCo = new HashSet<>();
        for (DongHangRequest d : req.getItems()) {
            if (!daCo.add(d.getMaSp())) {
                throw new BusinessException(ErrorCode.SAN_PHAM_LAP_LAI);
            }
        }

        return Database.inTransaction(() -> {
            if (req.getMaKh() != null) {
                khachHangDao.findById(req.getMaKh()).orElseThrow(() -> new BusinessException(ErrorCode.KHACH_HANG_NOT_FOUND));
            }
            BigDecimal tong = BigDecimal.ZERO;
            List<SanPhamResponse> sanPham = req.getItems().stream().map(d -> kiemTraMatHang(d.getMaSp(), d.getSoLuong())).toList();
            for (int i = 0; i < sanPham.size(); i++) {
                tong = tong.add(sanPham.get(i).getGiaBan().multiply(BigDecimal.valueOf(req.getItems().get(i).getSoLuong())));
            }

            BigDecimal giam = req.getGiamGia() != null ? req.getGiamGia() : BigDecimal.ZERO;
            if (giam.compareTo(tong) > 0) {
                throw new BusinessException(ErrorCode.GIAM_GIA_VUOT_TONG);
            }
            BigDecimal phaiTra = tong.subtract(giam);
            BigDecimal khachTra;
            if (req.getPhuongThuc() == PhuongThucThanhToan.TIEN_MAT) {
                khachTra = req.getKhachTra() != null ? req.getKhachTra() : phaiTra;
                if (khachTra.compareTo(phaiTra) < 0) {
                    throw new BusinessException(ErrorCode.KHACH_TRA_THIEU);
                }
            } else {
                khachTra = phaiTra;   // chuyển khoản / quẹt thẻ: trả đúng số tiền
            }

            int maHd = hoaDonDao.insert(req.getMaKh(), maTk, tong, giam, khachTra, req.getPhuongThuc(), Jdbc.text(req.getGhiChu()));
            for (int i = 0; i < sanPham.size(); i++) {
                SanPhamResponse sp = sanPham.get(i);
                int soLuong = req.getItems().get(i).getSoLuong();
                hoaDonDao.insertChiTiet(maHd, sp.getMaSp(), soLuong, sp.getGiaBan(), sp.getGiaNhap());
                if (!sanPhamDao.truTonKho(sp.getMaSp(), soLuong)) {
                    throw khongDuHang(sanPhamDao.findById(sp.getMaSp()).orElse(sp));
                }
            }
            if (req.getMaKh() != null) {
                khachHangDao.congDiem(req.getMaKh(), diem(phaiTra));
            }
            return chiTiet(maHd);
        });
    }

    public List<HoaDonResponse> search(String keyword, LocalDate tuNgay, LocalDate denNgay) {
        Session.requireLogin();
        LocalDate den = denNgay != null ? denNgay : LocalDate.now();
        LocalDate tu = tuNgay != null ? tuNgay : den.minusDays(30);
        return hoaDonDao.search(Jdbc.text(keyword), tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), SO_HOA_DON_TOI_DA);
    }

    /** Hóa đơn kèm các dòng hàng. */
    public HoaDonResponse chiTiet(int maHd) {
        Session.requireLogin();
        HoaDonResponse hd = hoaDonDao.findById(maHd).orElseThrow(() -> new BusinessException(ErrorCode.HOA_DON_NOT_FOUND));
        hd.setChiTiet(hoaDonDao.findChiTiet(maHd));
        return hd;
    }

    /** Hủy hóa đơn (chỉ chủ cửa hàng): hoàn hàng về kho, trừ lại điểm đã cộng cho khách. */
    public HoaDonResponse huy(int maHd, String lyDo) {
        Session.requireAdmin();
        return Database.inTransaction(() -> {
            HoaDonResponse hd = chiTiet(maHd);
            if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
                throw new BusinessException(ErrorCode.HOA_DON_DA_HUY);
            }
            for (ChiTietHoaDonResponse c : hd.getChiTiet()) {
                sanPhamDao.congTonKho(c.getMaSp(), c.getSoLuong());
            }
            if (hd.getMaKh() != null) {
                khachHangDao.congDiem(hd.getMaKh(), -diem(hd.getPhaiTra()));
            }
            String ghiChu = Jdbc.text(lyDo);
            hoaDonDao.huy(maHd, ghiChu != null ? "Hủy: " + ghiChu : "Đã hủy");
            return chiTiet(maHd);
        });
    }

    public static int diem(BigDecimal phaiTra) {
        return phaiTra.divideToIntegralValue(TIEN_MOI_DIEM).intValue();
    }

    private SanPhamResponse kiemTraMatHang(int maSp, int soLuong) {
        SanPhamResponse sp = sanPhamDao.findById(maSp).orElseThrow(() -> new BusinessException(ErrorCode.SAN_PHAM_NOT_FOUND));
        if (!sp.isDangBan()) {
            throw new BusinessException(ErrorCode.SAN_PHAM_NGUNG_BAN, "\"" + sp.getTenSp() + "\" đã ngừng bán");
        }
        if (sp.getTonKho() < soLuong) {
            throw khongDuHang(sp);
        }
        return sp;
    }

    private static BusinessException khongDuHang(SanPhamResponse sp) {
        return new BusinessException(ErrorCode.KHONG_DU_TON_KHO,
                "\"" + sp.getTenSp() + "\" chỉ còn " + sp.getTonKho() + " " + sp.getDonVi().toLowerCase() + " trong kho");
    }
}
