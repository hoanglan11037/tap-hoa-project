package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dao.DanhMucDao;
import com.hoangdinh.taphoa.dao.SanPhamDao;
import com.hoangdinh.taphoa.dto.request.SanPhamRequest;
import com.hoangdinh.taphoa.dto.response.DemTonKhoResponse;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.List;
import java.util.Map;

/** Quản lý hàng hóa. Thu ngân xem và tìm sản phẩm; thêm / sửa / xóa chỉ chủ cửa hàng. */
public class SanPhamService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int SO_SAN_PHAM_QUAY = 60;

    private final SanPhamDao sanPhamDao;
    private final DanhMucDao danhMucDao;

    public SanPhamService(SanPhamDao sanPhamDao, DanhMucDao danhMucDao) {
        this.sanPhamDao = sanPhamDao;
        this.danhMucDao = danhMucDao;
    }

    public PageResponse<SanPhamResponse> search(String keyword, Integer maDm, TinhTrangKho tinhTrang, int page, int size) {
        Session.requireLogin();
        return sanPhamDao.search(Jdbc.text(keyword), maDm, tinhTrang, Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    /** Sản phẩm hiển thị ở quầy bán hàng / chọn khi nhập hàng (chỉ hàng đang bán). */
    public List<SanPhamResponse> dangBan(String keyword, Integer maDm) {
        Session.requireLogin();
        return sanPhamDao.findDangBan(Jdbc.text(keyword), maDm, SO_SAN_PHAM_QUAY);
    }

    /** Quét mã vạch hoặc gõ mã hàng (SP0001) ở quầy. */
    public SanPhamResponse findByMa(String ma) {
        Session.requireLogin();
        String key = ma == null ? "" : ma.trim();
        return sanPhamDao.findByMa(key).orElseThrow(() -> new BusinessException(ErrorCode.MA_VACH_KHONG_TON_TAI));
    }

    public DemTonKhoResponse demTonKho() {
        Session.requireLogin();
        Map<TinhTrangKho, Long> dem = sanPhamDao.demTheoTinhTrang();
        return DemTonKhoResponse.builder()
                .tatCa(dem.values().stream().mapToLong(Long::longValue).sum())
                .conHang(dem.get(TinhTrangKho.CON_HANG))
                .sapHet(dem.get(TinhTrangKho.SAP_HET))
                .hetHang(dem.get(TinhTrangKho.HET_HANG))
                .ngungBan(dem.get(TinhTrangKho.NGUNG_BAN))
                .build();
    }

    public SanPhamResponse get(int maSp) {
        Session.requireLogin();
        return sanPhamDao.findById(maSp).orElseThrow(() -> new BusinessException(ErrorCode.SAN_PHAM_NOT_FOUND));
    }

    public SanPhamResponse create(SanPhamRequest req) {
        Session.requireAdmin();
        kiemTra(req, null);
        return get(sanPhamDao.insert(req));
    }

    public SanPhamResponse update(int maSp, SanPhamRequest req) {
        Session.requireAdmin();
        get(maSp);
        kiemTra(req, maSp);
        sanPhamDao.update(maSp, req);
        return get(maSp);
    }

    /** Sản phẩm đã từng mua / bán thì không xóa (giữ lịch sử hóa đơn), chỉ chuyển sang ngừng bán. */
    public void delete(int maSp) {
        Session.requireAdmin();
        get(maSp);
        if (sanPhamDao.daCoGiaoDich(maSp)) {
            throw new BusinessException(ErrorCode.SAN_PHAM_DA_GIAO_DICH);
        }
        sanPhamDao.delete(maSp);
    }

    private void kiemTra(SanPhamRequest req, Integer maSp) {
        Validation.check(req);
        danhMucDao.findById(req.getMaDm()).orElseThrow(() -> new BusinessException(ErrorCode.DANH_MUC_NOT_FOUND));
        String maVach = Jdbc.text(req.getMaVach());
        if (maVach != null && sanPhamDao.existsByMaVach(maVach, maSp)) {
            throw new BusinessException(ErrorCode.MA_VACH_DA_TON_TAI);
        }
    }
}
