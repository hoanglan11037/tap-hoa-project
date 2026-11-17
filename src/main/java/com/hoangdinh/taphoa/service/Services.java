package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.dao.DanhMucDao;
import com.hoangdinh.taphoa.dao.HoaDonDao;
import com.hoangdinh.taphoa.dao.KhachHangDao;
import com.hoangdinh.taphoa.dao.NhaCungCapDao;
import com.hoangdinh.taphoa.dao.PhieuNhapDao;
import com.hoangdinh.taphoa.dao.SanPhamDao;
import com.hoangdinh.taphoa.dao.TaiKhoanDao;

/**
 * Nơi khởi tạo và nối các lớp với nhau: mỗi service nhận các DAO nó cần qua constructor.
 * Controller (giao diện) gọi service qua đây, ví dụ {@code Services.SAN_PHAM.search(...)}.
 */
public final class Services {

    private static final TaiKhoanDao TAI_KHOAN_DAO = new TaiKhoanDao();
    private static final DanhMucDao DANH_MUC_DAO = new DanhMucDao();
    private static final NhaCungCapDao NHA_CUNG_CAP_DAO = new NhaCungCapDao();
    private static final SanPhamDao SAN_PHAM_DAO = new SanPhamDao();
    private static final KhachHangDao KHACH_HANG_DAO = new KhachHangDao();
    private static final HoaDonDao HOA_DON_DAO = new HoaDonDao();
    private static final PhieuNhapDao PHIEU_NHAP_DAO = new PhieuNhapDao();

    public static final AuthService AUTH = new AuthService(TAI_KHOAN_DAO);
    public static final TaiKhoanService TAI_KHOAN = new TaiKhoanService(TAI_KHOAN_DAO);
    public static final DanhMucService DANH_MUC = new DanhMucService(DANH_MUC_DAO);
    public static final NhaCungCapService NHA_CUNG_CAP = new NhaCungCapService(NHA_CUNG_CAP_DAO);
    public static final SanPhamService SAN_PHAM = new SanPhamService(SAN_PHAM_DAO, DANH_MUC_DAO);
    public static final KhachHangService KHACH_HANG = new KhachHangService(KHACH_HANG_DAO, HOA_DON_DAO);
    public static final BanHangService BAN_HANG = new BanHangService(HOA_DON_DAO, SAN_PHAM_DAO, KHACH_HANG_DAO);
    public static final NhapHangService NHAP_HANG = new NhapHangService(PHIEU_NHAP_DAO, NHA_CUNG_CAP_DAO, SAN_PHAM_DAO);
    public static final ThongKeService THONG_KE = new ThongKeService(HOA_DON_DAO, SAN_PHAM_DAO, KHACH_HANG_DAO);

    private Services() {
    }
}
