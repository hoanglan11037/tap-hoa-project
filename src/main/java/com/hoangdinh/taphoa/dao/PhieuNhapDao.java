package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.response.ChiTietPhieuNhapResponse;
import com.hoangdinh.taphoa.dto.response.PhieuNhapResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Truy xuất phiếu nhập hàng (tblPhieuNhap, tblChiTietPhieuNhap). */
public class PhieuNhapDao {

    private static final String SELECT = """
            SELECT p.fldMa_pn, p.fldSo_pn, p.fldMa_ncc, n.fldTen_ncc, t.fldHo_ten AS nguoi_nhap, p.fldNgay_nhap, p.fldGhi_chu,
                   x.so_mat_hang, x.tong_tien
            FROM tblPhieuNhap p
            JOIN tblNhaCungCap n ON n.fldMa_ncc = p.fldMa_ncc
            JOIN tblTaiKhoan t ON t.fldMa_tk = p.fldMa_tk
            CROSS APPLY (SELECT COUNT(*) AS so_mat_hang, COALESCE(SUM(c.fldSo_luong * c.fldDon_gia), 0) AS tong_tien
                         FROM tblChiTietPhieuNhap c WHERE c.fldMa_pn = p.fldMa_pn) x
            """;

    public List<PhieuNhapResponse> findMoiNhat(int limit) {
        return Jdbc.query(SELECT.replaceFirst("SELECT", "SELECT TOP (?)") + " ORDER BY p.fldNgay_nhap DESC, p.fldMa_pn DESC",
                PhieuNhapDao::map, limit);
    }

    public Optional<PhieuNhapResponse> findById(int maPn) {
        return Jdbc.queryOne(SELECT + " WHERE p.fldMa_pn = ?", PhieuNhapDao::map, maPn);
    }

    public List<ChiTietPhieuNhapResponse> findChiTiet(int maPn) {
        return Jdbc.query("""
                SELECT c.fldMa_sp, s.fldMa_hang, s.fldTen_sp, s.fldDon_vi, c.fldSo_luong, c.fldDon_gia
                FROM tblChiTietPhieuNhap c JOIN tblSanPham s ON s.fldMa_sp = c.fldMa_sp
                WHERE c.fldMa_pn = ? ORDER BY s.fldTen_sp
                """, rs -> ChiTietPhieuNhapResponse.builder()
                .maSp(rs.getInt("fldMa_sp"))
                .maHang(rs.getString("fldMa_hang"))
                .tenSp(rs.getString("fldTen_sp"))
                .donVi(rs.getString("fldDon_vi"))
                .soLuong(rs.getInt("fldSo_luong"))
                .donGia(rs.getBigDecimal("fldDon_gia"))
                .thanhTien(rs.getBigDecimal("fldDon_gia").multiply(BigDecimal.valueOf(rs.getInt("fldSo_luong"))))
                .build(), maPn);
    }

    public int insert(int maNcc, int maTk, String ghiChu) {
        return Jdbc.insert("INSERT INTO tblPhieuNhap (fldMa_ncc, fldMa_tk, fldGhi_chu) VALUES (?, ?, ?)",
                maNcc, maTk, ghiChu);
    }

    public void insertChiTiet(int maPn, int maSp, int soLuong, BigDecimal donGia) {
        Jdbc.update("INSERT INTO tblChiTietPhieuNhap (fldMa_pn, fldMa_sp, fldSo_luong, fldDon_gia) VALUES (?, ?, ?, ?)",
                maPn, maSp, soLuong, donGia);
    }

    private static PhieuNhapResponse map(ResultSet rs) throws SQLException {
        return PhieuNhapResponse.builder()
                .maPn(rs.getInt("fldMa_pn"))
                .soPn(rs.getString("fldSo_pn"))
                .maNcc(rs.getInt("fldMa_ncc"))
                .tenNcc(rs.getString("fldTen_ncc"))
                .nguoiNhap(rs.getString("nguoi_nhap"))
                .ngayNhap(Jdbc.getDateTime(rs, "fldNgay_nhap"))
                .ghiChu(rs.getString("fldGhi_chu"))
                .soMatHang(rs.getInt("so_mat_hang"))
                .tongTien(rs.getBigDecimal("tong_tien"))
                .build();
    }
}
