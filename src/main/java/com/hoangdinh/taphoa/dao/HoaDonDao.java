package com.hoangdinh.taphoa.dao;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dto.response.ChiTietHoaDonResponse;
import com.hoangdinh.taphoa.dto.response.DoanhThuNgayResponse;
import com.hoangdinh.taphoa.dto.response.DoanhThuThangResponse;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.TopSanPhamResponse;
import com.hoangdinh.taphoa.enums.PhuongThucThanhToan;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Truy xuất hóa đơn bán hàng (tblHoaDon, tblChiTietHoaDon) và các số liệu doanh thu. */
public class HoaDonDao {

    private static final String SELECT = """
            SELECT h.fldMa_hd, h.fldSo_hd, h.fldMa_kh, k.fldHo_ten AS ten_kh, k.fldSdt AS sdt_kh, t.fldHo_ten AS thu_ngan,
                   h.fldNgay_lap, h.fldTong_tien, h.fldGiam_gia, h.fldKhach_tra, h.fldPhuong_thuc, h.fldTrang_thai, h.fldGhi_chu,
                   (SELECT COUNT(*) FROM tblChiTietHoaDon c WHERE c.fldMa_hd = h.fldMa_hd) AS so_mat_hang
            FROM tblHoaDon h
            LEFT JOIN tblKhachHang k ON k.fldMa_kh = h.fldMa_kh
            JOIN tblTaiKhoan t ON t.fldMa_tk = h.fldMa_tk
            """;

    /** Doanh thu tính trên hóa đơn hoàn thành: tổng tiền hàng - giảm giá. */
    private static final String HOAN_THANH = "fldTrang_thai = 'HOAN_THANH'";

    /** Hóa đơn trong khoảng [from, to), tìm theo số hóa đơn / tên / SĐT khách; mới nhất trước. */
    public List<HoaDonResponse> search(String keyword, LocalDateTime from, LocalDateTime to, int limit) {
        StringBuilder sql = new StringBuilder(SELECT.replaceFirst("SELECT", "SELECT TOP (?)"))
                .append(" WHERE h.fldNgay_lap >= ? AND h.fldNgay_lap < ?");
        List<Object> params = new ArrayList<>(List.of(limit, from, to));
        if (keyword != null) {
            sql.append(" AND (h.fldSo_hd LIKE ? OR k.fldHo_ten LIKE ? OR k.fldSdt LIKE ?)");
            String like = Jdbc.like(keyword);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return Jdbc.query(sql + " ORDER BY h.fldNgay_lap DESC, h.fldMa_hd DESC", HoaDonDao::map, params.toArray());
    }

    public List<HoaDonResponse> findGanDay(int limit) {
        return Jdbc.query(SELECT.replaceFirst("SELECT", "SELECT TOP (?)") + " ORDER BY h.fldNgay_lap DESC, h.fldMa_hd DESC",
                HoaDonDao::map, limit);
    }

    public List<HoaDonResponse> findByKhachHang(int maKh, int limit) {
        return Jdbc.query(SELECT.replaceFirst("SELECT", "SELECT TOP (?)")
                + " WHERE h.fldMa_kh = ? ORDER BY h.fldNgay_lap DESC", HoaDonDao::map, limit, maKh);
    }

    public Optional<HoaDonResponse> findById(int maHd) {
        return Jdbc.queryOne(SELECT + " WHERE h.fldMa_hd = ?", HoaDonDao::map, maHd);
    }

    public List<ChiTietHoaDonResponse> findChiTiet(int maHd) {
        return Jdbc.query("""
                SELECT c.fldMa_sp, s.fldMa_hang, s.fldTen_sp, s.fldDon_vi, c.fldSo_luong, c.fldDon_gia
                FROM tblChiTietHoaDon c JOIN tblSanPham s ON s.fldMa_sp = c.fldMa_sp
                WHERE c.fldMa_hd = ? ORDER BY s.fldTen_sp
                """, rs -> ChiTietHoaDonResponse.builder()
                .maSp(rs.getInt("fldMa_sp"))
                .maHang(rs.getString("fldMa_hang"))
                .tenSp(rs.getString("fldTen_sp"))
                .donVi(rs.getString("fldDon_vi"))
                .soLuong(rs.getInt("fldSo_luong"))
                .donGia(rs.getBigDecimal("fldDon_gia"))
                .thanhTien(rs.getBigDecimal("fldDon_gia").multiply(BigDecimal.valueOf(rs.getInt("fldSo_luong"))))
                .build(), maHd);
    }

    public int insert(Integer maKh, int maTk, BigDecimal tongTien, BigDecimal giamGia, BigDecimal khachTra,
                      PhuongThucThanhToan phuongThuc, String ghiChu) {
        return Jdbc.insert("""
                INSERT INTO tblHoaDon (fldMa_kh, fldMa_tk, fldTong_tien, fldGiam_gia, fldKhach_tra, fldPhuong_thuc,
                                       fldTrang_thai, fldGhi_chu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, maKh, maTk, tongTien, giamGia, khachTra, phuongThuc, TrangThaiHoaDon.HOAN_THANH, ghiChu);
    }

    public void insertChiTiet(int maHd, int maSp, int soLuong, BigDecimal donGia, BigDecimal giaVon) {
        Jdbc.update("""
                INSERT INTO tblChiTietHoaDon (fldMa_hd, fldMa_sp, fldSo_luong, fldDon_gia, fldGia_von)
                VALUES (?, ?, ?, ?, ?)
                """, maHd, maSp, soLuong, donGia, giaVon);
    }

    public void huy(int maHd, String lyDo) {
        Jdbc.update("UPDATE tblHoaDon SET fldTrang_thai = ?, fldGhi_chu = ? WHERE fldMa_hd = ?",
                TrangThaiHoaDon.DA_HUY, lyDo, maHd);
    }

    // ---------- Số liệu doanh thu ----------

    public BigDecimal doanhThu(LocalDateTime from, LocalDateTime to) {
        return Jdbc.queryMoney("SELECT SUM(fldTong_tien - fldGiam_gia) FROM tblHoaDon WHERE " + HOAN_THANH
                + " AND fldNgay_lap >= ? AND fldNgay_lap < ?", from, to);
    }

    public long demHoaDon(LocalDateTime from, LocalDateTime to) {
        return Jdbc.queryLong("SELECT COUNT(*) FROM tblHoaDon WHERE " + HOAN_THANH
                + " AND fldNgay_lap >= ? AND fldNgay_lap < ?", from, to);
    }

    /** Lợi nhuận gộp = doanh thu - giá vốn hàng bán. */
    public BigDecimal loiNhuan(LocalDateTime from, LocalDateTime to) {
        return Jdbc.queryMoney("""
                SELECT SUM(h.fldTong_tien - h.fldGiam_gia)
                       - (SELECT COALESCE(SUM(c.fldSo_luong * c.fldGia_von), 0) FROM tblChiTietHoaDon c
                          JOIN tblHoaDon h2 ON h2.fldMa_hd = c.fldMa_hd
                          WHERE h2.fldTrang_thai = 'HOAN_THANH' AND h2.fldNgay_lap >= ? AND h2.fldNgay_lap < ?)
                FROM tblHoaDon h WHERE h.fldTrang_thai = 'HOAN_THANH' AND h.fldNgay_lap >= ? AND h.fldNgay_lap < ?
                """, from, to, from, to);
    }

    /** Doanh thu từng ngày trong [from, to); ngày không bán được không có trong kết quả. */
    public List<DoanhThuNgayResponse> doanhThuTheoNgay(LocalDateTime from, LocalDateTime to) {
        return Jdbc.query("""
                SELECT CAST(fldNgay_lap AS DATE) AS ngay, SUM(fldTong_tien - fldGiam_gia) AS doanh_thu, COUNT(*) AS so_hd
                FROM tblHoaDon WHERE fldTrang_thai = 'HOAN_THANH' AND fldNgay_lap >= ? AND fldNgay_lap < ?
                GROUP BY CAST(fldNgay_lap AS DATE)
                """, rs -> new DoanhThuNgayResponse(Jdbc.getDate(rs, "ngay"), rs.getBigDecimal("doanh_thu"),
                rs.getLong("so_hd")), from, to);
    }

    /** Doanh thu, lợi nhuận, số hóa đơn từng tháng của năm; tháng không bán được không có trong kết quả. */
    public List<DoanhThuThangResponse> doanhThuTheoThang(int nam) {
        return Jdbc.query("""
                SELECT MONTH(h.fldNgay_lap) AS thang,
                       SUM(h.fldTong_tien - h.fldGiam_gia) AS doanh_thu,
                       SUM(h.fldTong_tien - h.fldGiam_gia - v.gia_von) AS loi_nhuan,
                       COUNT(*) AS so_hd
                FROM tblHoaDon h
                CROSS APPLY (SELECT SUM(c.fldSo_luong * c.fldGia_von) AS gia_von
                             FROM tblChiTietHoaDon c WHERE c.fldMa_hd = h.fldMa_hd) v
                WHERE h.fldTrang_thai = 'HOAN_THANH' AND YEAR(h.fldNgay_lap) = ?
                GROUP BY MONTH(h.fldNgay_lap)
                """, rs -> new DoanhThuThangResponse(rs.getInt("thang"), rs.getBigDecimal("doanh_thu"),
                rs.getBigDecimal("loi_nhuan"), rs.getLong("so_hd")), nam);
    }

    /** Sản phẩm bán chạy nhất (theo số lượng) trong [from, to). */
    public List<TopSanPhamResponse> topSanPham(LocalDateTime from, LocalDateTime to, int limit) {
        return Jdbc.query("""
                SELECT TOP (?) s.fldMa_hang, s.fldTen_sp, s.fldDon_vi,
                       SUM(c.fldSo_luong) AS so_luong, SUM(c.fldSo_luong * c.fldDon_gia) AS doanh_thu
                FROM tblChiTietHoaDon c
                JOIN tblHoaDon h ON h.fldMa_hd = c.fldMa_hd
                JOIN tblSanPham s ON s.fldMa_sp = c.fldMa_sp
                WHERE h.fldTrang_thai = 'HOAN_THANH' AND h.fldNgay_lap >= ? AND h.fldNgay_lap < ?
                GROUP BY s.fldMa_hang, s.fldTen_sp, s.fldDon_vi
                ORDER BY so_luong DESC, doanh_thu DESC
                """, rs -> new TopSanPhamResponse(rs.getString("fldMa_hang"), rs.getString("fldTen_sp"),
                rs.getString("fldDon_vi"), rs.getLong("so_luong"), rs.getBigDecimal("doanh_thu")), limit, from, to);
    }

    private static HoaDonResponse map(ResultSet rs) throws SQLException {
        BigDecimal tong = rs.getBigDecimal("fldTong_tien");
        BigDecimal giam = rs.getBigDecimal("fldGiam_gia");
        BigDecimal khachTra = rs.getBigDecimal("fldKhach_tra");
        BigDecimal phaiTra = tong.subtract(giam);
        return HoaDonResponse.builder()
                .maHd(rs.getInt("fldMa_hd"))
                .soHd(rs.getString("fldSo_hd"))
                .maKh(Jdbc.getInteger(rs, "fldMa_kh"))
                .tenKhachHang(rs.getString("ten_kh"))
                .sdtKhachHang(rs.getString("sdt_kh"))
                .thuNgan(rs.getString("thu_ngan"))
                .ngayLap(Jdbc.getDateTime(rs, "fldNgay_lap"))
                .tongTien(tong)
                .giamGia(giam)
                .phaiTra(phaiTra)
                .khachTra(khachTra)
                .tienThua(khachTra.subtract(phaiTra))
                .phuongThuc(Jdbc.getEnum(rs, "fldPhuong_thuc", PhuongThucThanhToan.class))
                .trangThai(Jdbc.getEnum(rs, "fldTrang_thai", TrangThaiHoaDon.class))
                .ghiChu(rs.getString("fldGhi_chu"))
                .soMatHang(rs.getInt("so_mat_hang"))
                .build();
    }
}
