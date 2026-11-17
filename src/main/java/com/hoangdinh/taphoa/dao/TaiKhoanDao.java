package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.request.TaiKhoanRequest;
import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.enums.VaiTro;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Truy xuất bảng tblTaiKhoan. */
public class TaiKhoanDao {

    private static final String SELECT = """
            SELECT fldMa_tk, fldTen_dang_nhap, fldHo_ten, fldVai_tro, fldTrang_thai, fldNgay_tao
            FROM tblTaiKhoan
            """;

    public List<TaiKhoanResponse> findAll() {
        return Jdbc.query(SELECT + " ORDER BY fldMa_tk", TaiKhoanDao::map);
    }

    public Optional<TaiKhoanResponse> findById(int maTk) {
        return Jdbc.queryOne(SELECT + " WHERE fldMa_tk = ?", TaiKhoanDao::map, maTk);
    }

    public Optional<TaiKhoanResponse> findByTenDangNhap(String tenDangNhap) {
        return Jdbc.queryOne(SELECT + " WHERE fldTen_dang_nhap = ?", TaiKhoanDao::map, tenDangNhap);
    }

    /** Mật khẩu đã băm (SHA-256 hex) của tài khoản. */
    public Optional<String> findMatKhau(int maTk) {
        return Jdbc.queryOne("SELECT fldMat_khau FROM tblTaiKhoan WHERE fldMa_tk = ?", rs -> rs.getString(1), maTk);
    }

    /** Tên đăng nhập đã có ai dùng chưa (bỏ qua tài khoản exceptMaTk khi đang sửa). */
    public boolean existsByTenDangNhap(String tenDangNhap, Integer exceptMaTk) {
        return Jdbc.exists("SELECT 1 FROM tblTaiKhoan WHERE fldTen_dang_nhap = ? AND fldMa_tk <> ?",
                tenDangNhap, exceptMaTk != null ? exceptMaTk : -1);
    }

    public int insert(TaiKhoanRequest req, String matKhauDaBam) {
        return Jdbc.insert("""
                INSERT INTO tblTaiKhoan (fldTen_dang_nhap, fldMat_khau, fldHo_ten, fldVai_tro, fldTrang_thai)
                VALUES (?, ?, ?, ?, ?)
                """, req.getTenDangNhap().trim(), matKhauDaBam, req.getHoTen().trim(), req.getVaiTro(), req.isTrangThai());
    }

    public void update(int maTk, TaiKhoanRequest req) {
        Jdbc.update("""
                UPDATE tblTaiKhoan
                SET fldTen_dang_nhap = ?, fldHo_ten = ?, fldVai_tro = ?, fldTrang_thai = ?
                WHERE fldMa_tk = ?
                """, req.getTenDangNhap().trim(), req.getHoTen().trim(), req.getVaiTro(), req.isTrangThai(), maTk);
    }

    public void updateMatKhau(int maTk, String matKhauDaBam) {
        Jdbc.update("UPDATE tblTaiKhoan SET fldMat_khau = ? WHERE fldMa_tk = ?", matKhauDaBam, maTk);
    }

    private static TaiKhoanResponse map(ResultSet rs) throws SQLException {
        return TaiKhoanResponse.builder()
                .maTk(rs.getInt("fldMa_tk"))
                .tenDangNhap(rs.getString("fldTen_dang_nhap"))
                .hoTen(rs.getString("fldHo_ten"))
                .vaiTro(Jdbc.getEnum(rs, "fldVai_tro", VaiTro.class))
                .trangThai(rs.getBoolean("fldTrang_thai"))
                .ngayTao(Jdbc.getDateTime(rs, "fldNgay_tao"))
                .build();
    }
}
