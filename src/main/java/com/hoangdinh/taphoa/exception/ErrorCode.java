package com.hoangdinh.taphoa.exception;

import lombok.Getter;

/** Mã lỗi nghiệp vụ hiển thị cho người dùng. Dạng TH-xxyy: xx là nhóm lỗi, yy là số thứ tự. */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR("TH-4000", "Dữ liệu không hợp lệ"),

    UNAUTHORIZED("TH-4100", "Phiên đăng nhập đã kết thúc, vui lòng đăng nhập lại"),
    LOGIN_FAILED("TH-4101", "Sai tên đăng nhập hoặc mật khẩu"),

    FORBIDDEN("TH-4300", "Chỉ chủ cửa hàng mới dùng được chức năng này"),
    ACCOUNT_LOCKED("TH-4301", "Tài khoản đã bị khóa, liên hệ chủ cửa hàng"),

    SAN_PHAM_NOT_FOUND("TH-4401", "Không tìm thấy sản phẩm"),
    DANH_MUC_NOT_FOUND("TH-4402", "Không tìm thấy danh mục"),
    NCC_NOT_FOUND("TH-4403", "Không tìm thấy nhà cung cấp"),
    KHACH_HANG_NOT_FOUND("TH-4404", "Không tìm thấy khách hàng"),
    HOA_DON_NOT_FOUND("TH-4405", "Không tìm thấy hóa đơn"),
    PHIEU_NHAP_NOT_FOUND("TH-4406", "Không tìm thấy phiếu nhập"),
    TAI_KHOAN_NOT_FOUND("TH-4407", "Không tìm thấy tài khoản"),
    MA_VACH_KHONG_TON_TAI("TH-4408", "Không có sản phẩm nào với mã vạch / mã hàng này"),

    SDT_DA_TON_TAI("TH-4901", "Số điện thoại đã được dùng"),
    MA_VACH_DA_TON_TAI("TH-4902", "Mã vạch đã gán cho sản phẩm khác"),
    TEN_DANH_MUC_DA_TON_TAI("TH-4903", "Tên danh mục đã tồn tại"),
    TEN_DANG_NHAP_DA_TON_TAI("TH-4904", "Tên đăng nhập đã tồn tại"),
    SAN_PHAM_DA_GIAO_DICH("TH-4905", "Sản phẩm đã có giao dịch mua bán, chỉ có thể chuyển sang ngừng bán"),
    DANH_MUC_CON_SAN_PHAM("TH-4906", "Danh mục còn sản phẩm, hãy chuyển sản phẩm sang danh mục khác trước"),
    NCC_DA_GIAO_DICH("TH-4907", "Nhà cung cấp đã có phiếu nhập, chỉ có thể ngừng hợp tác"),
    DU_LIEU_DANG_DUOC_DUNG("TH-4908", "Dữ liệu đang được sử dụng ở nơi khác, không thể thực hiện"),
    DU_LIEU_TRUNG("TH-4909", "Dữ liệu bị trùng với bản ghi đã có"),

    GIO_HANG_TRONG("TH-4221", "Giỏ hàng đang trống"),
    SAN_PHAM_NGUNG_BAN("TH-4222", "Sản phẩm đã ngừng bán"),
    KHONG_DU_TON_KHO("TH-4223", "Không đủ hàng trong kho"),
    GIAM_GIA_VUOT_TONG("TH-4224", "Số tiền giảm giá lớn hơn tổng tiền hàng"),
    KHACH_TRA_THIEU("TH-4225", "Khách đưa chưa đủ tiền"),
    HOA_DON_DA_HUY("TH-4226", "Hóa đơn đã bị hủy trước đó"),
    NCC_NGUNG_HOP_TAC("TH-4227", "Nhà cung cấp đã ngừng hợp tác"),
    PHIEU_NHAP_TRONG("TH-4228", "Phiếu nhập chưa có dòng hàng nào"),
    SAN_PHAM_LAP_LAI("TH-4229", "Một sản phẩm chỉ được xuất hiện một lần trên phiếu"),
    MAT_KHAU_CU_SAI("TH-4230", "Mật khẩu hiện tại không đúng"),
    MAT_KHAU_BAT_BUOC("TH-4231", "Nhập mật khẩu cho tài khoản mới"),
    KHONG_TU_KHOA_MINH("TH-4232", "Không thể tự khóa hoặc hạ quyền tài khoản đang đăng nhập"),

    INTERNAL_ERROR("TH-5000", "Lỗi hệ thống, vui lòng thử lại sau"),
    DB_KET_NOI("TH-5001", "Không kết nối được cơ sở dữ liệu");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
