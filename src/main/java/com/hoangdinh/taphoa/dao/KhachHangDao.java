package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.request.KhachHangRequest;
import com.hoangdinh.taphoa.dto.response.KhachHangResponse;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Truy xuất bảng tblKhachHang kèm thống kê mua hàng (số lần mua, tổng chi tiêu, lần mua cuối). */
public class KhachHangDao {

    private static final String SELECT = """
            SELECT k.fldMa_kh, k.fldMa_the, k.fldHo_ten, k.fldSdt, k.fldDia_chi, k.fldDiem_tich_luy, k.fldNgay_tao,
                   COALESCE(x.so_lan, 0) AS so_lan, COALESCE(x.tong, 0) AS tong_chi, x.lan_cuoi
            FROM tblKhachHang k
            OUTER APPLY (SELECT COUNT(*) AS so_lan, SUM(h.fldTong_tien - h.fldGiam_gia) AS tong, MAX(h.fldNgay_lap) AS lan_cuoi
                         FROM tblHoaDon h WHERE h.fldMa_kh = k.fldMa_kh AND h.fldTrang_thai = 'HOAN_THANH') x
            """;

    public PageResponse<KhachHangResponse> search(String keyword, int page, int size) {
        String where = " WHERE 1 = 1";
        List<Object> params = new ArrayList<>();
        if (keyword != null) {
            where += " AND (k.fldHo_ten LIKE ? OR k.fldSdt LIKE ? OR k.fldMa_the LIKE ?)";
            String like = Jdbc.like(keyword);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        long total = Jdbc.queryLong("SELECT COUNT(*) FROM tblKhachHang k" + where, params.toArray());
        params.add(page * size);
        params.add(size);
        List<KhachHangResponse> items = Jdbc.query(SELECT + where
                + " ORDER BY k.fldMa_kh DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", KhachHangDao::map, params.toArray());
        return new PageResponse<>(items, page, size, total, (int) ((total + size - 1) / size));
    }

    public Optional<KhachHangResponse> findById(int maKh) {
        return Jdbc.queryOne(SELECT + " WHERE k.fldMa_kh = ?", KhachHangDao::map, maKh);
    }

    /** Tìm khách tại quầy theo số điện thoại hoặc mã thẻ. */
    public Optional<KhachHangResponse> findBySdtOrMaThe(String key) {
        return Jdbc.queryOne(SELECT + " WHERE k.fldSdt = ? OR k.fldMa_the = ?", KhachHangDao::map, key, key.toUpperCase());
    }

    public long demMoiTu(LocalDateTime from) {
        return Jdbc.queryLong("SELECT COUNT(*) FROM tblKhachHang WHERE fldNgay_tao >= ?", from);
    }

    public boolean existsBySdt(String sdt, Integer exceptMaKh) {
        return Jdbc.exists("SELECT 1 FROM tblKhachHang WHERE fldSdt = ? AND fldMa_kh <> ?",
                sdt, exceptMaKh != null ? exceptMaKh : -1);
    }

    public int insert(KhachHangRequest req) {
        return Jdbc.insert("INSERT INTO tblKhachHang (fldHo_ten, fldSdt, fldDia_chi) VALUES (?, ?, ?)",
                req.getHoTen().trim(), req.getSdt().trim(), Jdbc.text(req.getDiaChi()));
    }

    public void update(int maKh, KhachHangRequest req) {
        Jdbc.update("UPDATE tblKhachHang SET fldHo_ten = ?, fldSdt = ?, fldDia_chi = ? WHERE fldMa_kh = ?",
                req.getHoTen().trim(), req.getSdt().trim(), Jdbc.text(req.getDiaChi()), maKh);
    }

    /** Cộng (số dương) hoặc trừ (số âm) điểm tích lũy; không để điểm âm. */
    public void congDiem(int maKh, int diem) {
        Jdbc.update("""
                UPDATE tblKhachHang
                SET fldDiem_tich_luy = CASE WHEN fldDiem_tich_luy + ? < 0 THEN 0 ELSE fldDiem_tich_luy + ? END
                WHERE fldMa_kh = ?
                """, diem, diem, maKh);
    }

    /** Xóa khách hàng; hóa đơn cũ vẫn giữ, chỉ bỏ liên kết (ON DELETE SET NULL). */
    public void delete(int maKh) {
        Jdbc.update("DELETE FROM tblKhachHang WHERE fldMa_kh = ?", maKh);
    }

    private static KhachHangResponse map(ResultSet rs) throws SQLException {
        return KhachHangResponse.builder()
                .maKh(rs.getInt("fldMa_kh"))
                .maThe(rs.getString("fldMa_the"))
                .hoTen(rs.getString("fldHo_ten"))
                .sdt(rs.getString("fldSdt"))
                .diaChi(rs.getString("fldDia_chi"))
                .diemTichLuy(rs.getInt("fldDiem_tich_luy"))
                .ngayTao(Jdbc.getDateTime(rs, "fldNgay_tao"))
                .soLanMua(rs.getLong("so_lan"))
                .tongChiTieu(rs.getBigDecimal("tong_chi"))
                .lanMuaCuoi(Jdbc.getDateTime(rs, "lan_cuoi"))
                .build();
    }
}
