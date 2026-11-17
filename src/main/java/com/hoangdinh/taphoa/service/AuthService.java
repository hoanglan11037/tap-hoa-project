package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.dao.TaiKhoanDao;
import com.hoangdinh.taphoa.dto.request.DoiMatKhauRequest;
import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.PasswordHasher;
import com.hoangdinh.taphoa.security.Session;

/** Đăng nhập, đăng xuất, đổi mật khẩu. */
public class AuthService {

    private final TaiKhoanDao taiKhoanDao;

    public AuthService(TaiKhoanDao taiKhoanDao) {
        this.taiKhoanDao = taiKhoanDao;
    }

    /** Kiểm tra tên đăng nhập / mật khẩu; thành công thì ghi nhận vào Session. */
    public TaiKhoanResponse login(String tenDangNhap, String matKhau) {
        TaiKhoanResponse tk = taiKhoanDao.findByTenDangNhap(tenDangNhap.trim())
                .filter(t -> PasswordHasher.matches(matKhau, taiKhoanDao.findMatKhau(t.getMaTk()).orElse(null)))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!tk.isTrangThai()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        Session.login(tk);
        return tk;
    }

    public void logout() {
        Session.logout();
    }

    public void doiMatKhau(String matKhauCu, String matKhauMoi) {
        Validation.check(new DoiMatKhauRequest(matKhauCu, matKhauMoi));
        int maTk = Session.current().getMaTk();
        String hienTai = taiKhoanDao.findMatKhau(maTk).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!PasswordHasher.matches(matKhauCu, hienTai)) {
            throw new BusinessException(ErrorCode.MAT_KHAU_CU_SAI);
        }
        taiKhoanDao.updateMatKhau(maTk, PasswordHasher.hash(matKhauMoi));
    }
}
