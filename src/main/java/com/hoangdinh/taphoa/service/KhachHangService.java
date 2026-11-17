package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dao.HoaDonDao;
import com.hoangdinh.taphoa.dao.KhachHangDao;
import com.hoangdinh.taphoa.dto.request.KhachHangRequest;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.KhachHangResponse;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.List;

/** Khách hàng thân thiết: tích điểm khi mua, tra cứu lịch sử mua hàng. */
public class KhachHangService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int SO_HOA_DON_LICH_SU = 20;

    private final KhachHangDao khachHangDao;
    private final HoaDonDao hoaDonDao;

    public KhachHangService(KhachHangDao khachHangDao, HoaDonDao hoaDonDao) {
        this.khachHangDao = khachHangDao;
        this.hoaDonDao = hoaDonDao;
    }

    public PageResponse<KhachHangResponse> search(String keyword, int page, int size) {
        Session.requireLogin();
        return khachHangDao.search(Jdbc.text(keyword), Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    public KhachHangResponse get(int maKh) {
        Session.requireLogin();
        return khachHangDao.findById(maKh).orElseThrow(() -> new BusinessException(ErrorCode.KHACH_HANG_NOT_FOUND));
    }

    /** Tìm nhanh tại quầy bằng số điện thoại hoặc mã thẻ KHxxxx. */
    public KhachHangResponse timTaiQuay(String sdtHoacMaThe) {
        Session.requireLogin();
        String key = sdtHoacMaThe == null ? "" : sdtHoacMaThe.trim();
        return khachHangDao.findBySdtOrMaThe(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.KHACH_HANG_NOT_FOUND,
                        "Chưa có khách hàng với số điện thoại / mã thẻ \"" + key + "\""));
    }

    public List<HoaDonResponse> lichSuMua(int maKh) {
        Session.requireLogin();
        return hoaDonDao.findByKhachHang(maKh, SO_HOA_DON_LICH_SU);
    }

    public KhachHangResponse create(KhachHangRequest req) {
        Session.requireLogin();
        Validation.check(req);
        if (khachHangDao.existsBySdt(req.getSdt().trim(), null)) {
            throw new BusinessException(ErrorCode.SDT_DA_TON_TAI);
        }
        return get(khachHangDao.insert(req));
    }

    public KhachHangResponse update(int maKh, KhachHangRequest req) {
        Validation.check(req);
        get(maKh);
        if (khachHangDao.existsBySdt(req.getSdt().trim(), maKh)) {
            throw new BusinessException(ErrorCode.SDT_DA_TON_TAI);
        }
        khachHangDao.update(maKh, req);
        return get(maKh);
    }

    /** Xóa khách hàng: hóa đơn cũ vẫn giữ nhưng chuyển thành khách lẻ. Chỉ chủ cửa hàng. */
    public void delete(int maKh) {
        Session.requireAdmin();
        get(maKh);
        khachHangDao.delete(maKh);
    }
}
