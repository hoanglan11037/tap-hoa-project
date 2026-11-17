/* =========================================================
   CSDL: taphoa_db — Phần mềm quản lý cửa hàng tạp hóa Hoàng Đinh
   Hệ quản trị: Microsoft SQL Server (LocalDB 2019 trở lên)
   Chạy: sqlcmd -S "(localdb)\MSSQLLocalDB" -E -i taphoa_db.sql
   ========================================================= */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;   -- bắt buộc cho cột tính toán PERSISTED và chỉ mục có điều kiện
SET NOCOUNT ON;
GO
USE master;
GO
IF DB_ID(N'taphoa_db') IS NOT NULL
BEGIN
    ALTER DATABASE taphoa_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE taphoa_db;
END
GO
CREATE DATABASE taphoa_db COLLATE Vietnamese_CI_AS;
GO
/* Tài khoản SQL cho phần mềm Java kết nối (LocalDB không hỗ trợ Windows Authentication qua JDBC) */
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'taphoa_app')
    CREATE LOGIN taphoa_app WITH PASSWORD = N'TapHoa@123456', CHECK_POLICY = OFF;
GO
USE taphoa_db;
GO
CREATE USER taphoa_app FOR LOGIN taphoa_app;
ALTER ROLE db_owner ADD MEMBER taphoa_app;
GO

/* ---------- 1. Tài khoản đăng nhập phần mềm ---------- */
CREATE TABLE tblTaiKhoan (
    fldMa_tk          INT IDENTITY(1,1) CONSTRAINT PK_TaiKhoan PRIMARY KEY,
    fldTen_dang_nhap  VARCHAR(50)   NOT NULL CONSTRAINT UQ_TaiKhoan_TenDN UNIQUE,
    fldMat_khau       VARCHAR(64)   NOT NULL,              -- SHA-256 (hex)
    fldHo_ten         NVARCHAR(100) NOT NULL,
    fldVai_tro        VARCHAR(10)   NOT NULL CONSTRAINT CK_TaiKhoan_VaiTro CHECK (fldVai_tro IN ('ADMIN', 'THU_NGAN')),
    fldTrang_thai     BIT           NOT NULL CONSTRAINT DF_TaiKhoan_TrangThai DEFAULT 1,
    fldNgay_tao       DATETIME2(0)  NOT NULL CONSTRAINT DF_TaiKhoan_NgayTao DEFAULT SYSDATETIME()
);

/* ---------- 2. Danh mục (nhóm hàng) ---------- */
CREATE TABLE tblDanhMuc (
    fldMa_dm   INT IDENTITY(1,1) CONSTRAINT PK_DanhMuc PRIMARY KEY,
    fldTen_dm  NVARCHAR(100) NOT NULL CONSTRAINT UQ_DanhMuc_Ten UNIQUE,
    fldMo_ta   NVARCHAR(300) NULL
);

/* ---------- 3. Nhà cung cấp ---------- */
CREATE TABLE tblNhaCungCap (
    fldMa_ncc         INT IDENTITY(1,1) CONSTRAINT PK_NhaCungCap PRIMARY KEY,
    fldTen_ncc        NVARCHAR(150) NOT NULL,
    fldNguoi_lien_he  NVARCHAR(100) NULL,
    fldSdt            VARCHAR(15)   NOT NULL CONSTRAINT UQ_NhaCungCap_Sdt UNIQUE,
    fldDia_chi        NVARCHAR(200) NULL,
    fldTrang_thai     BIT           NOT NULL CONSTRAINT DF_NhaCungCap_TrangThai DEFAULT 1
);

/* ---------- 4. Sản phẩm ---------- */
CREATE TABLE tblSanPham (
    fldMa_sp          INT IDENTITY(1,1) CONSTRAINT PK_SanPham PRIMARY KEY,
    fldMa_hang        AS ('SP' + RIGHT('0000' + CAST(fldMa_sp AS VARCHAR(10)), 4)) PERSISTED,
    fldMa_vach        VARCHAR(20)   NULL,
    fldTen_sp         NVARCHAR(150) NOT NULL,
    fldMa_dm          INT           NOT NULL CONSTRAINT FK_SanPham_DanhMuc REFERENCES tblDanhMuc (fldMa_dm),
    fldDon_vi         NVARCHAR(20)  NOT NULL,
    fldGia_ban        DECIMAL(12,0) NOT NULL CONSTRAINT CK_SanPham_GiaBan CHECK (fldGia_ban >= 0),
    fldGia_nhap       DECIMAL(12,0) NOT NULL CONSTRAINT DF_SanPham_GiaNhap DEFAULT 0 CONSTRAINT CK_SanPham_GiaNhap CHECK (fldGia_nhap >= 0),
    fldTon_kho        INT           NOT NULL CONSTRAINT DF_SanPham_TonKho DEFAULT 0 CONSTRAINT CK_SanPham_TonKho CHECK (fldTon_kho >= 0),
    fldTon_toi_thieu  INT           NOT NULL CONSTRAINT DF_SanPham_TonToiThieu DEFAULT 5 CONSTRAINT CK_SanPham_TonToiThieu CHECK (fldTon_toi_thieu >= 0),
    fldTrang_thai     BIT           NOT NULL CONSTRAINT DF_SanPham_TrangThai DEFAULT 1,   -- 1: đang bán, 0: ngừng bán
    fldNgay_tao       DATETIME2(0)  NOT NULL CONSTRAINT DF_SanPham_NgayTao DEFAULT SYSDATETIME()
);
-- Mã vạch không bắt buộc nhưng nếu có thì không được trùng
CREATE UNIQUE INDEX UX_SanPham_MaVach ON tblSanPham (fldMa_vach) WHERE fldMa_vach IS NOT NULL;
CREATE INDEX IX_SanPham_Ten ON tblSanPham (fldTen_sp);

/* ---------- 5. Khách hàng thân thiết ---------- */
CREATE TABLE tblKhachHang (
    fldMa_kh          INT IDENTITY(1,1) CONSTRAINT PK_KhachHang PRIMARY KEY,
    fldMa_the         AS ('KH' + RIGHT('0000' + CAST(fldMa_kh AS VARCHAR(10)), 4)) PERSISTED,
    fldHo_ten         NVARCHAR(100) NOT NULL,
    fldSdt            VARCHAR(15)   NOT NULL CONSTRAINT UQ_KhachHang_Sdt UNIQUE,
    fldDia_chi        NVARCHAR(200) NULL,
    fldDiem_tich_luy  INT           NOT NULL CONSTRAINT DF_KhachHang_Diem DEFAULT 0 CONSTRAINT CK_KhachHang_Diem CHECK (fldDiem_tich_luy >= 0),
    fldNgay_tao       DATETIME2(0)  NOT NULL CONSTRAINT DF_KhachHang_NgayTao DEFAULT SYSDATETIME()
);

/* ---------- 6. Hóa đơn bán hàng ---------- */
CREATE TABLE tblHoaDon (
    fldMa_hd         INT IDENTITY(1,1) CONSTRAINT PK_HoaDon PRIMARY KEY,
    fldSo_hd         AS ('HD' + RIGHT('000000' + CAST(fldMa_hd AS VARCHAR(10)), 6)) PERSISTED,
    fldMa_kh         INT           NULL     CONSTRAINT FK_HoaDon_KhachHang REFERENCES tblKhachHang (fldMa_kh) ON DELETE SET NULL,
    fldMa_tk         INT           NOT NULL CONSTRAINT FK_HoaDon_TaiKhoan REFERENCES tblTaiKhoan (fldMa_tk),
    fldNgay_lap      DATETIME2(0)  NOT NULL CONSTRAINT DF_HoaDon_NgayLap DEFAULT SYSDATETIME(),
    fldTong_tien     DECIMAL(12,0) NOT NULL CONSTRAINT CK_HoaDon_TongTien CHECK (fldTong_tien >= 0),   -- tổng tiền hàng
    fldGiam_gia      DECIMAL(12,0) NOT NULL CONSTRAINT DF_HoaDon_GiamGia DEFAULT 0,
    fldKhach_tra     DECIMAL(12,0) NOT NULL,
    fldPhuong_thuc   VARCHAR(15)   NOT NULL CONSTRAINT CK_HoaDon_PhuongThuc CHECK (fldPhuong_thuc IN ('TIEN_MAT', 'CHUYEN_KHOAN', 'THE')),
    fldTrang_thai    VARCHAR(12)   NOT NULL CONSTRAINT DF_HoaDon_TrangThai DEFAULT 'HOAN_THANH'
                     CONSTRAINT CK_HoaDon_TrangThai CHECK (fldTrang_thai IN ('HOAN_THANH', 'DA_HUY')),
    fldGhi_chu       NVARCHAR(200) NULL,
    CONSTRAINT CK_HoaDon_GiamGia CHECK (fldGiam_gia BETWEEN 0 AND fldTong_tien),
    CONSTRAINT CK_HoaDon_KhachTra CHECK (fldKhach_tra >= fldTong_tien - fldGiam_gia)
);
CREATE INDEX IX_HoaDon_NgayLap ON tblHoaDon (fldNgay_lap);

CREATE TABLE tblChiTietHoaDon (
    fldMa_hd     INT           NOT NULL CONSTRAINT FK_CTHD_HoaDon REFERENCES tblHoaDon (fldMa_hd) ON DELETE CASCADE,
    fldMa_sp     INT           NOT NULL CONSTRAINT FK_CTHD_SanPham REFERENCES tblSanPham (fldMa_sp),
    fldSo_luong  INT           NOT NULL CONSTRAINT CK_CTHD_SoLuong CHECK (fldSo_luong > 0),
    fldDon_gia   DECIMAL(12,0) NOT NULL CONSTRAINT CK_CTHD_DonGia CHECK (fldDon_gia >= 0),
    fldGia_von   DECIMAL(12,0) NOT NULL CONSTRAINT CK_CTHD_GiaVon CHECK (fldGia_von >= 0),   -- giá nhập lúc bán, để tính lợi nhuận
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (fldMa_hd, fldMa_sp)
);

/* ---------- 7. Phiếu nhập hàng ---------- */
CREATE TABLE tblPhieuNhap (
    fldMa_pn     INT IDENTITY(1,1) CONSTRAINT PK_PhieuNhap PRIMARY KEY,
    fldSo_pn     AS ('PN' + RIGHT('00000' + CAST(fldMa_pn AS VARCHAR(10)), 5)) PERSISTED,
    fldMa_ncc    INT           NOT NULL CONSTRAINT FK_PhieuNhap_NCC REFERENCES tblNhaCungCap (fldMa_ncc),
    fldMa_tk     INT           NOT NULL CONSTRAINT FK_PhieuNhap_TaiKhoan REFERENCES tblTaiKhoan (fldMa_tk),
    fldNgay_nhap DATETIME2(0)  NOT NULL CONSTRAINT DF_PhieuNhap_Ngay DEFAULT SYSDATETIME(),
    fldGhi_chu   NVARCHAR(200) NULL
);
CREATE INDEX IX_PhieuNhap_Ngay ON tblPhieuNhap (fldNgay_nhap);

CREATE TABLE tblChiTietPhieuNhap (
    fldMa_pn     INT           NOT NULL CONSTRAINT FK_CTPN_PhieuNhap REFERENCES tblPhieuNhap (fldMa_pn) ON DELETE CASCADE,
    fldMa_sp     INT           NOT NULL CONSTRAINT FK_CTPN_SanPham REFERENCES tblSanPham (fldMa_sp),
    fldSo_luong  INT           NOT NULL CONSTRAINT CK_CTPN_SoLuong CHECK (fldSo_luong > 0),
    fldDon_gia   DECIMAL(12,0) NOT NULL CONSTRAINT CK_CTPN_DonGia CHECK (fldDon_gia >= 0),
    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (fldMa_pn, fldMa_sp)
);
GO

/* ---------- View: sản phẩm kèm danh mục và tình trạng tồn kho ---------- */
CREATE VIEW vwSanPham AS
SELECT sp.fldMa_sp, sp.fldMa_hang, sp.fldMa_vach, sp.fldTen_sp, sp.fldMa_dm, dm.fldTen_dm, sp.fldDon_vi,
       sp.fldGia_ban, sp.fldGia_nhap, sp.fldTon_kho, sp.fldTon_toi_thieu, sp.fldTrang_thai AS fldDang_ban,
       sp.fldNgay_tao,
       CASE
           WHEN sp.fldTrang_thai = 0 THEN 'NGUNG_BAN'
           WHEN sp.fldTon_kho = 0 THEN 'HET_HANG'
           WHEN sp.fldTon_kho <= sp.fldTon_toi_thieu THEN 'SAP_HET'
           ELSE 'CON_HANG'
       END AS fldTinh_trang
FROM tblSanPham sp
JOIN tblDanhMuc dm ON dm.fldMa_dm = sp.fldMa_dm;
GO

/* =========================== DỮ LIỆU MẪU =========================== */
INSERT INTO tblTaiKhoan (fldTen_dang_nhap, fldMat_khau, fldHo_ten, fldVai_tro) VALUES
('admin',    LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'admin123'), 2)), N'Chủ cửa hàng',     'ADMIN'),
('thungan1', LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', '123456'), 2)),   N'Phạm Thị Ngọc Lan', 'THU_NGAN');

INSERT INTO tblDanhMuc (fldTen_dm, fldMo_ta) VALUES
(N'Đồ uống',        N'Nước suối, nước ngọt, trà, cà phê đóng chai'),
(N'Mì & đồ khô',    N'Mì gói, miến, phở khô, gạo'),
(N'Gia vị',         N'Nước mắm, dầu ăn, bột ngọt, đường, muối'),
(N'Bánh kẹo',       N'Bánh quy, snack, kẹo'),
(N'Sữa',            N'Sữa tươi, sữa chua, sữa đặc'),
(N'Hóa phẩm',       N'Bột giặt, nước rửa chén, khăn giấy');

INSERT INTO tblNhaCungCap (fldTen_ncc, fldNguoi_lien_he, fldSdt, fldDia_chi) VALUES
(N'Đại lý nước giải khát Minh Tâm', N'Anh Tâm',  '0903111222', N'Chợ Bình Tây, Quận 6, TP.HCM'),
(N'Tổng kho thực phẩm Hưng Thịnh',  N'Chị Hoa',  '0918333444', N'Quốc lộ 1A, Bình Tân, TP.HCM'),
(N'Nhà phân phối hóa phẩm Phú Gia', N'Anh Khoa', '0937555666', N'KCN Tân Bình, TP.HCM');

INSERT INTO tblSanPham (fldMa_vach, fldTen_sp, fldMa_dm, fldDon_vi, fldGia_ban, fldGia_nhap, fldTon_kho, fldTon_toi_thieu) VALUES
('8930000000011', N'Nước suối 500ml',                 1, N'Chai',  5000,   3200, 96, 24),
('8930000000028', N'Nước ngọt có ga lon 330ml',       1, N'Lon',  10000,   7500, 60, 24),
('8930000000035', N'Trà xanh chai 455ml',             1, N'Chai', 10000,   7200,  8, 12),
('8930000000042', N'Cà phê sữa lon 180ml',            1, N'Lon',  12000,   8800, 30, 12),
('8930000000059', N'Mì tôm chua cay gói 75g',         2, N'Gói',   4500,   3400,150, 30),
('8930000000066', N'Phở bò ăn liền gói 65g',          2, N'Gói',   8000,   6000, 40, 20),
('8930000000073', N'Gạo thơm túi 5kg',                2, N'Túi', 115000,  98000, 12,  5),
('8930000000080', N'Nước mắm cá cơm 500ml',           3, N'Chai', 42000,  33000, 18,  6),
('8930000000097', N'Dầu ăn đậu nành 1 lít',           3, N'Chai', 52000,  44000,  4,  6),
('8930000000103', N'Bột ngọt gói 400g',               3, N'Gói',  28000,  22500, 20,  6),
('8930000000110', N'Đường cát trắng 1kg',             3, N'Gói',  26000,  21000,  0,  5),
('8930000000127', N'Bánh quy bơ hộp 300g',            4, N'Hộp',  45000,  34000, 14,  5),
('8930000000134', N'Snack khoai tây vị tự nhiên 52g', 4, N'Gói',  12000,   8500, 36, 12),
('8930000000141', N'Kẹo dẻo trái cây gói 100g',       4, N'Gói',  15000,  10500,  5,  8),
('8930000000158', N'Sữa tươi tiệt trùng 180ml',       5, N'Hộp',   8000,   6300, 72, 24),
('8930000000165', N'Sữa chua ăn có đường',            5, N'Hộp',   7000,   5200, 24, 12),
('8930000000172', N'Sữa đặc có đường lon 380g',       5, N'Lon',  25000,  20000, 16,  6),
('8930000000189', N'Bột giặt túi 800g',               6, N'Túi',  38000,  30500, 10,  4),
('8930000000196', N'Nước rửa chén chai 750ml',        6, N'Chai', 29000,  22800,  3,  4),
('8930000000202', N'Khăn giấy rút 180 tờ',            6, N'Gói',  18000,  13000, 22,  6);

INSERT INTO tblKhachHang (fldHo_ten, fldSdt, fldDia_chi) VALUES
(N'Trần Văn Hùng',     '0912345678', N'Hẻm 12, Lê Văn Sỹ'),
(N'Nguyễn Thị Mai',    '0987112204', N'Chung cư Hòa Bình, lầu 3'),
(N'Lê Quốc Bảo',       '0903556781', N'Số 45 đường số 7'),
(N'Võ Thị Thanh Tâm',  '0868220915', NULL),
(N'Đặng Minh Khôi',    '0934701662', N'Số 8 Nguyễn Trãi');

/* Phiếu nhập mẫu */
INSERT INTO tblPhieuNhap (fldMa_ncc, fldMa_tk, fldNgay_nhap, fldGhi_chu) VALUES
(1, 1, DATEADD(DAY, -20, SYSDATETIME()), N'Nhập đồ uống đầu tháng'),
(2, 1, DATEADD(DAY, -12, SYSDATETIME()), NULL),
(3, 2, DATEADD(DAY, -3,  SYSDATETIME()), N'Hàng hóa phẩm');
INSERT INTO tblChiTietPhieuNhap (fldMa_pn, fldMa_sp, fldSo_luong, fldDon_gia) VALUES
(1, 1, 120, 3200), (1, 2, 96, 7500), (1, 3, 48, 7200), (1, 4, 48, 8800),
(2, 5, 200, 3400), (2, 6, 60, 6000), (2, 7, 20, 98000), (2, 8, 24, 33000), (2, 10, 24, 22500),
(3, 18, 20, 30500), (3, 19, 24, 22800), (3, 20, 40, 13000);

/* Hóa đơn mẫu: 180 ngày gần nhất, mỗi ngày vài hóa đơn ngẫu nhiên (để màn Tổng quan, Thống kê có số liệu) */
DECLARE @d INT = 180, @i INT, @n INT, @hd INT, @ngay DATETIME2(0), @r INT, @tong DECIMAL(12,0), @pt VARCHAR(15), @kh INT;
WHILE @d >= 0
BEGIN
    SET @n = 3 + ABS(CHECKSUM(NEWID())) % 6;
    SET @i = 0;
    WHILE @i < @n
    BEGIN
        SET @ngay = DATEADD(MINUTE, 390 + ABS(CHECKSUM(NEWID())) % 900,
                            CAST(CAST(DATEADD(DAY, -@d, SYSDATETIME()) AS DATE) AS DATETIME2(0)));
        IF @ngay <= SYSDATETIME()
        BEGIN
            SET @r = ABS(CHECKSUM(NEWID()));
            SET @pt = CASE @r % 5 WHEN 0 THEN 'CHUYEN_KHOAN' WHEN 1 THEN 'THE' ELSE 'TIEN_MAT' END;
            SET @kh = CASE WHEN @r % 3 = 0 THEN 1 + @r % 5 ELSE NULL END;
            INSERT INTO tblHoaDon (fldMa_kh, fldMa_tk, fldNgay_lap, fldTong_tien, fldGiam_gia, fldKhach_tra, fldPhuong_thuc)
            VALUES (@kh, 1 + @r % 2, @ngay, 0, 0, 0, @pt);
            SET @hd = SCOPE_IDENTITY();
            INSERT INTO tblChiTietHoaDon (fldMa_hd, fldMa_sp, fldSo_luong, fldDon_gia, fldGia_von)
            SELECT TOP (1 + ABS(CHECKSUM(NEWID())) % 5) @hd, fldMa_sp, 1 + ABS(CHECKSUM(NEWID())) % 4, fldGia_ban, fldGia_nhap
            FROM tblSanPham ORDER BY NEWID();
            SELECT @tong = SUM(fldSo_luong * fldDon_gia) FROM tblChiTietHoaDon WHERE fldMa_hd = @hd;
            UPDATE tblHoaDon
            SET fldTong_tien = @tong,
                fldKhach_tra = CASE WHEN @pt = 'TIEN_MAT' THEN CEILING(@tong / 10000) * 10000 ELSE @tong END
            WHERE fldMa_hd = @hd;
        END
        SET @i += 1;
    END
    SET @d -= 1;
END;

/* Một hóa đơn đã hủy để minh họa */
UPDATE tblHoaDon SET fldTrang_thai = 'DA_HUY', fldGhi_chu = N'Khách đổi ý, hủy hóa đơn'
WHERE fldMa_hd = (SELECT MIN(fldMa_hd) FROM tblHoaDon WHERE fldNgay_lap >= DATEADD(DAY, -2, SYSDATETIME()));

/* Điểm tích lũy: 1 điểm cho mỗi 10.000 đ đã mua */
UPDATE kh SET fldDiem_tich_luy = ISNULL(x.diem, 0)
FROM tblKhachHang kh
OUTER APPLY (SELECT CAST(SUM(h.fldTong_tien - h.fldGiam_gia) / 10000 AS INT) AS diem
             FROM tblHoaDon h WHERE h.fldMa_kh = kh.fldMa_kh AND h.fldTrang_thai = 'HOAN_THANH') x;
GO
