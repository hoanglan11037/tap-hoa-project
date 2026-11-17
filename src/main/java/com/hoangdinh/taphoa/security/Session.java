package com.hoangdinh.taphoa.security;

import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.enums.VaiTro;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;

/** Tài khoản đang đăng nhập trên máy này; tầng service dùng để biết ai thực hiện thao tác và kiểm tra quyền. */
public final class Session {

    private static volatile TaiKhoanResponse taiKhoan;

    private Session() {
    }

    public static void login(TaiKhoanResponse tk) {
        taiKhoan = tk;
    }

    public static void logout() {
        taiKhoan = null;
    }

    /** Tài khoản đang đăng nhập, null nếu chưa đăng nhập. */
    public static TaiKhoanResponse taiKhoan() {
        return taiKhoan;
    }

    public static boolean isAdmin() {
        TaiKhoanResponse tk = taiKhoan;
        return tk != null && tk.getVaiTro() == VaiTro.ADMIN;
    }

    /** Tài khoản đang đăng nhập; chưa đăng nhập thì báo lỗi để giao diện quay về màn đăng nhập. */
    public static TaiKhoanResponse current() {
        TaiKhoanResponse tk = taiKhoan;
        if (tk == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return tk;
    }

    /** Mọi chức năng nghiệp vụ đều yêu cầu đã đăng nhập. */
    public static void requireLogin() {
        current();
    }

    /** Chức năng chỉ dành cho chủ cửa hàng (danh mục, nhà cung cấp, thống kê, tài khoản, hủy hóa đơn). */
    public static void requireAdmin() {
        if (current().getVaiTro() != VaiTro.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
