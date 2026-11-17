package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.request.SanPhamRequest;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Truy xuất bảng tblSanPham và view vwSanPham
 * (view kèm tên danh mục và tình trạng tồn kho: còn hàng / sắp hết / hết hàng / ngừng bán).
 */
public class SanPhamDao {

    private static final String SELECT = """
            SELECT fldMa_sp, fldMa_hang, fldMa_vach, fldTen_sp, fldMa_dm, fldTen_dm, fldDon_vi, fldGia_ban, fldGia_nhap,
                   fldTon_kho, fldTon_toi_thieu, fldDang_ban, fldTinh_trang, fldNgay_tao
            FROM vwSanPham
            """;

    /** Tìm theo tên / mã hàng / mã vạch, lọc theo danh mục và tình trạng kho; page đánh số từ 0. */
    public PageResponse<SanPhamResponse> search(String keyword, Integer maDm, TinhTrangKho tinhTrang, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (keyword != null) {
            where.append(" AND (fldTen_sp LIKE ? OR fldMa_hang LIKE ? OR fldMa_vach LIKE ?)");
            String like = Jdbc.like(keyword);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (maDm != null) {
            where.append(" AND fldMa_dm = ?");
            params.add(maDm);
        }
        if (tinhTrang != null) {
            where.append(" AND fldTinh_trang = ?");
            params.add(tinhTrang);
        }
        long total = Jdbc.queryLong("SELECT COUNT(*) FROM vwSanPham" + where, params.toArray());
        params.add(page * size);
        params.add(size);
        List<SanPhamResponse> items = Jdbc.query(SELECT + where
                + " ORDER BY fldTen_sp OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", SanPhamDao::map, params.toArray());
        return new PageResponse<>(items, page, size, total, (int) ((total + size - 1) / size));
    }

    /** Sản phẩm đang bán (cho màn bán hàng / nhập hàng), lọc theo danh mục và từ khóa. */
    public List<SanPhamResponse> findDangBan(String keyword, Integer maDm, int limit) {
        StringBuilder where = new StringBuilder(" WHERE fldDang_ban = 1");
        List<Object> params = new ArrayList<>();
        params.add(limit);
        if (keyword != null) {
            where.append(" AND (fldTen_sp LIKE ? OR fldMa_hang LIKE ? OR fldMa_vach LIKE ?)");
            String like = Jdbc.like(keyword);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (maDm != null) {
            where.append(" AND fldMa_dm = ?");
            params.add(maDm);
        }
        return Jdbc.query(SELECT.replaceFirst("SELECT", "SELECT TOP (?)") + where + " ORDER BY fldTen_sp",
                SanPhamDao::map, params.toArray());
    }

    public Map<TinhTrangKho, Long> demTheoTinhTrang() {
        Map<TinhTrangKho, Long> result = new EnumMap<>(TinhTrangKho.class);
        for (TinhTrangKho t : TinhTrangKho.values()) {
            result.put(t, 0L);
        }
        Jdbc.query("SELECT fldTinh_trang, COUNT(*) AS so_luong FROM vwSanPham GROUP BY fldTinh_trang",
                rs -> result.put(Jdbc.getEnum(rs, "fldTinh_trang", TinhTrangKho.class), rs.getLong("so_luong")));
        return result;
    }

    /** Sản phẩm đang bán mà hết hoặc sắp hết, ít hàng nhất xếp trước. */
    public List<SanPhamResponse> findCanNhap(int limit) {
        return Jdbc.query(SELECT.replaceFirst("SELECT", "SELECT TOP (?)")
                + " WHERE fldTinh_trang IN ('HET_HANG', 'SAP_HET') ORDER BY fldTon_kho, fldTen_sp",
                SanPhamDao::map, limit);
    }

    public Optional<SanPhamResponse> findById(int maSp) {
        return Jdbc.queryOne(SELECT + " WHERE fldMa_sp = ?", SanPhamDao::map, maSp);
    }

    /** Tìm chính xác theo mã vạch hoặc mã hàng (quét mã ở quầy). */
    public Optional<SanPhamResponse> findByMa(String ma) {
        return Jdbc.queryOne(SELECT + " WHERE fldMa_vach = ? OR fldMa_hang = ?", SanPhamDao::map, ma, ma.toUpperCase());
    }

    public boolean existsByMaVach(String maVach, Integer exceptMaSp) {
        return Jdbc.exists("SELECT 1 FROM tblSanPham WHERE fldMa_vach = ? AND fldMa_sp <> ?",
                maVach, exceptMaSp != null ? exceptMaSp : -1);
    }

    /** Sản phẩm đã xuất hiện trên hóa đơn hoặc phiếu nhập chưa. */
    public boolean daCoGiaoDich(int maSp) {
        return Jdbc.exists("""
                SELECT 1 WHERE EXISTS (SELECT 1 FROM tblChiTietHoaDon WHERE fldMa_sp = ?)
                            OR EXISTS (SELECT 1 FROM tblChiTietPhieuNhap WHERE fldMa_sp = ?)
                """, maSp, maSp);
    }

    public int insert(SanPhamRequest req) {
        return Jdbc.insert("""
                INSERT INTO tblSanPham (fldMa_vach, fldTen_sp, fldMa_dm, fldDon_vi, fldGia_ban, fldGia_nhap, fldTon_kho,
                                        fldTon_toi_thieu, fldTrang_thai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Jdbc.text(req.getMaVach()), req.getTenSp().trim(), req.getMaDm(), req.getDonVi().trim(),
                req.getGiaBan(), req.getGiaNhap() != null ? req.getGiaNhap() : BigDecimal.ZERO,
                req.getTonKho() != null ? req.getTonKho() : 0, req.getTonToiThieu(), req.isDangBan());
    }

    /** Sửa thông tin; tồn kho không sửa ở đây (thay đổi qua bán hàng / nhập hàng). */
    public void update(int maSp, SanPhamRequest req) {
        Jdbc.update("""
                UPDATE tblSanPham
                SET fldMa_vach = ?, fldTen_sp = ?, fldMa_dm = ?, fldDon_vi = ?, fldGia_ban = ?, fldGia_nhap = ?,
                    fldTon_toi_thieu = ?, fldTrang_thai = ?
                WHERE fldMa_sp = ?
                """, Jdbc.text(req.getMaVach()), req.getTenSp().trim(), req.getMaDm(), req.getDonVi().trim(),
                req.getGiaBan(), req.getGiaNhap() != null ? req.getGiaNhap() : BigDecimal.ZERO,
                req.getTonToiThieu(), req.isDangBan(), maSp);
    }

    /**
     * Trừ tồn kho khi bán. Điều kiện tồn kho >= số lượng nằm ngay trong câu UPDATE nên hai quầy bán cùng lúc
     * không thể làm kho âm; trả về false nếu không đủ hàng.
     */
    public boolean truTonKho(int maSp, int soLuong) {
        return Jdbc.update("UPDATE tblSanPham SET fldTon_kho = fldTon_kho - ? WHERE fldMa_sp = ? AND fldTon_kho >= ?",
                soLuong, maSp, soLuong) == 1;
    }

    public void congTonKho(int maSp, int soLuong) {
        Jdbc.update("UPDATE tblSanPham SET fldTon_kho = fldTon_kho + ? WHERE fldMa_sp = ?", soLuong, maSp);
    }

    /** Nhập hàng: cộng tồn kho và cập nhật giá nhập mới nhất. */
    public void nhapKho(int maSp, int soLuong, BigDecimal giaNhap) {
        Jdbc.update("UPDATE tblSanPham SET fldTon_kho = fldTon_kho + ?, fldGia_nhap = ? WHERE fldMa_sp = ?",
                soLuong, giaNhap, maSp);
    }

    public void delete(int maSp) {
        Jdbc.update("DELETE FROM tblSanPham WHERE fldMa_sp = ?", maSp);
    }

    static SanPhamResponse map(ResultSet rs) throws SQLException {
        return SanPhamResponse.builder()
                .maSp(rs.getInt("fldMa_sp"))
                .maHang(rs.getString("fldMa_hang"))
                .maVach(rs.getString("fldMa_vach"))
                .tenSp(rs.getString("fldTen_sp"))
                .maDm(rs.getInt("fldMa_dm"))
                .tenDm(rs.getString("fldTen_dm"))
                .donVi(rs.getString("fldDon_vi"))
                .giaBan(rs.getBigDecimal("fldGia_ban"))
                .giaNhap(rs.getBigDecimal("fldGia_nhap"))
                .tonKho(rs.getInt("fldTon_kho"))
                .tonToiThieu(rs.getInt("fldTon_toi_thieu"))
                .dangBan(rs.getBoolean("fldDang_ban"))
                .tinhTrang(Jdbc.getEnum(rs, "fldTinh_trang", TinhTrangKho.class))
                .ngayTao(Jdbc.getDateTime(rs, "fldNgay_tao"))
                .build();
    }
}
