package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.dao.TaiKhoanDao;
import com.hoangdinh.taphoa.dto.request.TaiKhoanRequest;
import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.enums.VaiTro;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.PasswordHasher;
import com.hoangdinh.taphoa.security.Session;
import java.util.List;

/** Quản lý tài khoản nhân viên (chỉ chủ cửa hàng). */
public class TaiKhoanService {

    private final TaiKhoanDao taiKhoanDao;

    public TaiKhoanService(TaiKhoanDao taiKhoanDao) {
        this.taiKhoanDao = taiKhoanDao;
    }

    public List<TaiKhoanResponse> findAll() {
        Session.requireAdmin();
        return taiKhoanDao.findAll();
    }

    public TaiKhoanResponse create(TaiKhoanRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        if (req.getMatKhau() == null || req.getMatKhau().isBlank()) {
            throw new BusinessException(ErrorCode.MAT_KHAU_BAT_BUOC);
        }
        if (taiKhoanDao.existsByTenDangNhap(req.getTenDangNhap().trim(), null)) {
            throw new BusinessException(ErrorCode.TEN_DANG_NHAP_DA_TON_TAI);
        }
        int maTk = taiKhoanDao.insert(req, PasswordHasher.hash(req.getMatKhau()));
        return get(maTk);
    }

    /** Bỏ trống mật khẩu để giữ nguyên; không cho tự khóa hoặc tự hạ quyền tài khoản đang đăng nhập. */
    public TaiKhoanResponse update(int maTk, TaiKhoanRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        get(maTk);
        if (taiKhoanDao.existsByTenDangNhap(req.getTenDangNhap().trim(), maTk)) {
            throw new BusinessException(ErrorCode.TEN_DANG_NHAP_DA_TON_TAI);
        }
        boolean laChinhMinh = maTk == Session.current().getMaTk();
        if (laChinhMinh && (!req.isTrangThai() || req.getVaiTro() != VaiTro.ADMIN)) {
            throw new BusinessException(ErrorCode.KHONG_TU_KHOA_MINH);
        }
        taiKhoanDao.update(maTk, req);
        if (req.getMatKhau() != null && !req.getMatKhau().isBlank()) {
            taiKhoanDao.updateMatKhau(maTk, PasswordHasher.hash(req.getMatKhau()));
        }
        TaiKhoanResponse saved = get(maTk);
        if (laChinhMinh) {
            Session.login(saved);   // cập nhật họ tên hiển thị
        }
        return saved;
    }

    private TaiKhoanResponse get(int maTk) {
        return taiKhoanDao.findById(maTk).orElseThrow(() -> new BusinessException(ErrorCode.TAI_KHOAN_NOT_FOUND));
    }
}
