package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.request.NhaCungCapRequest;
import com.hoangdinh.taphoa.dto.response.NhaCungCapResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Truy xuất bảng tblNhaCungCap, kèm số phiếu nhập và lần nhập gần nhất. */
public class NhaCungCapDao {

    private static final String SELECT = """
            SELECT n.fldMa_ncc, n.fldTen_ncc, n.fldNguoi_lien_he, n.fldSdt, n.fldDia_chi, n.fldTrang_thai,
                   (SELECT COUNT(*) FROM tblPhieuNhap p WHERE p.fldMa_ncc = n.fldMa_ncc) AS so_phieu,
                   (SELECT MAX(p.fldNgay_nhap) FROM tblPhieuNhap p WHERE p.fldMa_ncc = n.fldMa_ncc) AS lan_cuoi
            FROM tblNhaCungCap n
            """;

    /** keyword: tên / người liên hệ / số điện thoại; chiDangHopTac: chỉ lấy nhà cung cấp đang hợp tác. */
    public List<NhaCungCapResponse> search(String keyword, boolean chiDangHopTac) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        Object[] params = {};
        if (keyword != null) {
            sql.append(" AND (n.fldTen_ncc LIKE ? OR n.fldNguoi_lien_he LIKE ? OR n.fldSdt LIKE ?)");
            String like = Jdbc.like(keyword);
            params = new Object[] {like, like, like};
        }
        if (chiDangHopTac) {
            sql.append(" AND n.fldTrang_thai = 1");
        }
        return Jdbc.query(sql + " ORDER BY n.fldTrang_thai DESC, n.fldTen_ncc", NhaCungCapDao::map, params);
    }

    public Optional<NhaCungCapResponse> findById(int maNcc) {
        return Jdbc.queryOne(SELECT + " WHERE n.fldMa_ncc = ?", NhaCungCapDao::map, maNcc);
    }

    public boolean existsBySdt(String sdt, Integer exceptMaNcc) {
        return Jdbc.exists("SELECT 1 FROM tblNhaCungCap WHERE fldSdt = ? AND fldMa_ncc <> ?",
                sdt, exceptMaNcc != null ? exceptMaNcc : -1);
    }

    public int insert(NhaCungCapRequest req) {
        return Jdbc.insert("""
                INSERT INTO tblNhaCungCap (fldTen_ncc, fldNguoi_lien_he, fldSdt, fldDia_chi, fldTrang_thai)
                VALUES (?, ?, ?, ?, ?)
                """, req.getTenNcc().trim(), Jdbc.text(req.getNguoiLienHe()), req.getSdt().trim(),
                Jdbc.text(req.getDiaChi()), req.isTrangThai());
    }

    public void update(int maNcc, NhaCungCapRequest req) {
        Jdbc.update("""
                UPDATE tblNhaCungCap
                SET fldTen_ncc = ?, fldNguoi_lien_he = ?, fldSdt = ?, fldDia_chi = ?, fldTrang_thai = ?
                WHERE fldMa_ncc = ?
                """, req.getTenNcc().trim(), Jdbc.text(req.getNguoiLienHe()), req.getSdt().trim(),
                Jdbc.text(req.getDiaChi()), req.isTrangThai(), maNcc);
    }

    public void delete(int maNcc) {
        Jdbc.update("DELETE FROM tblNhaCungCap WHERE fldMa_ncc = ?", maNcc);
    }

    private static NhaCungCapResponse map(ResultSet rs) throws SQLException {
        return NhaCungCapResponse.builder()
                .maNcc(rs.getInt("fldMa_ncc"))
                .tenNcc(rs.getString("fldTen_ncc"))
                .nguoiLienHe(rs.getString("fldNguoi_lien_he"))
                .sdt(rs.getString("fldSdt"))
                .diaChi(rs.getString("fldDia_chi"))
                .trangThai(rs.getBoolean("fldTrang_thai"))
                .soPhieuNhap(rs.getLong("so_phieu"))
                .lanNhapCuoi(Jdbc.getDateTime(rs, "lan_cuoi"))
                .build();
    }
}
