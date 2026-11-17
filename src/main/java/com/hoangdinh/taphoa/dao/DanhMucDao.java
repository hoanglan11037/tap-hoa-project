package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.request.DanhMucRequest;
import com.hoangdinh.taphoa.dto.response.DanhMucResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Truy xuất bảng tblDanhMuc, kèm số sản phẩm và giá trị hàng tồn của từng danh mục. */
public class DanhMucDao {

    private static final String SELECT = """
            SELECT d.fldMa_dm, d.fldTen_dm, d.fldMo_ta,
                   (SELECT COUNT(*) FROM tblSanPham s WHERE s.fldMa_dm = d.fldMa_dm) AS so_san_pham,
                   (SELECT COALESCE(SUM(s.fldTon_kho * s.fldGia_nhap), 0) FROM tblSanPham s WHERE s.fldMa_dm = d.fldMa_dm) AS gia_tri_ton
            FROM tblDanhMuc d
            """;

    public List<DanhMucResponse> findAll() {
        return Jdbc.query(SELECT + " ORDER BY d.fldTen_dm", DanhMucDao::map);
    }

    public Optional<DanhMucResponse> findById(int maDm) {
        return Jdbc.queryOne(SELECT + " WHERE d.fldMa_dm = ?", DanhMucDao::map, maDm);
    }

    public boolean existsByTen(String ten, Integer exceptMaDm) {
        return Jdbc.exists("SELECT 1 FROM tblDanhMuc WHERE fldTen_dm = ? AND fldMa_dm <> ?",
                ten, exceptMaDm != null ? exceptMaDm : -1);
    }

    public int insert(DanhMucRequest req) {
        return Jdbc.insert("INSERT INTO tblDanhMuc (fldTen_dm, fldMo_ta) VALUES (?, ?)",
                req.getTenDm().trim(), Jdbc.text(req.getMoTa()));
    }

    public void update(int maDm, DanhMucRequest req) {
        Jdbc.update("UPDATE tblDanhMuc SET fldTen_dm = ?, fldMo_ta = ? WHERE fldMa_dm = ?",
                req.getTenDm().trim(), Jdbc.text(req.getMoTa()), maDm);
    }

    public void delete(int maDm) {
        Jdbc.update("DELETE FROM tblDanhMuc WHERE fldMa_dm = ?", maDm);
    }

    private static DanhMucResponse map(ResultSet rs) throws SQLException {
        return DanhMucResponse.builder()
                .maDm(rs.getInt("fldMa_dm"))
                .tenDm(rs.getString("fldTen_dm"))
                .moTa(rs.getString("fldMo_ta"))
                .soSanPham(rs.getLong("so_san_pham"))
                .giaTriTon(rs.getBigDecimal("gia_tri_ton"))
                .build();
    }
}
