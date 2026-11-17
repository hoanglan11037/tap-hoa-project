# Hoàng Đinh — Phần mềm quản lý cửa hàng tạp hóa

Đồ án lập trình Java: bán hàng tại quầy (quét mã vạch, giỏ hàng, tiền thừa, in phiếu), hóa đơn, sản phẩm – tồn kho,
danh mục, nhập hàng từ nhà cung cấp, khách hàng thân thiết tích điểm, báo cáo doanh thu – lãi gộp và tài khoản nhân viên.

Một ứng dụng desktop **JavaFX 21** (giao diện AtlantaFX Cupertino, biểu tượng Material Design) kết nối trực tiếp
**SQL Server** bằng **JDBC**.

## Chạy chương trình

0. **Tạo file cấu hình** (file cấu hình không đưa lên git):
   `copy src\main\resources\app.properties.example src\main\resources\app.properties`
1. **Tạo CSDL:** mở `database/taphoa_db.sql` trong SQL Server Management Studio và Execute
   (hoặc `sqlcmd -S "(localdb)\MSSQLLocalDB" -E -f 65001 -i database/taphoa_db.sql`).
   Script tạo sẵn 20 sản phẩm, 5 khách hàng, 3 nhà cung cấp và khoảng 6 tháng hóa đơn mẫu.
2. **Chạy:** `mvn javafx:run` tại thư mục dự án,
   hoặc build `mvn package` rồi `java -jar target/taphoa-1.0.0-all.jar`.

Tài khoản mẫu: `admin` / `admin123` (chủ cửa hàng), `thungan1` / `123456` (thu ngân).

**Kết nối CSDL** cấu hình trong `src/main/resources/app.properties`. Mặc định dùng SQL Server LocalDB `MSSQLLocalDB`
(đăng nhập SQL `taphoa_app` / `TapHoa@123456`, được tạo bởi script CSDL). Muốn trỏ sang SQL Server khác mà không build lại,
đặt một file `app.properties` cạnh file jar, ví dụ:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=taphoa_db;encrypt=false
db.user=taphoa_app
db.password=TapHoa@123456
```

## Chức năng

| Màn hình | Nội dung | Ai dùng |
|---|---|---|
| Tổng quan | Doanh thu, số hóa đơn, lãi gộp hôm nay (so với hôm qua), biểu đồ 7 ngày, hàng sắp hết, bán chạy hôm nay | Tất cả |
| Bán hàng (F2) | Quét mã vạch / gõ tên, lọc theo danh mục, giỏ hàng tăng giảm số lượng, tìm khách theo SĐT, giảm giá, tiền mặt – chuyển khoản – thẻ, tính tiền thừa, thanh toán (F9), in phiếu | Tất cả |
| Hóa đơn | Lọc theo ngày, xem phiếu tính tiền, in lại, xuất CSV; **hủy hóa đơn** (hoàn kho, trừ điểm) | Tất cả; hủy: chủ |
| Sản phẩm | Tìm, lọc theo danh mục và tình trạng kho, thanh tồn kho, xuất CSV; thêm / sửa / xóa | Xem: tất cả; sửa: chủ |
| Danh mục | Thẻ nhóm hàng kèm số sản phẩm và giá trị tồn | Chủ |
| Nhập hàng | Lập phiếu nhiều dòng, cộng tồn kho, cập nhật giá nhập; xem phiếu cũ | Tất cả |
| Nhà cung cấp | Thêm / sửa / ngừng hợp tác | Chủ |
| Khách hàng | Thẻ thành viên, điểm tích lũy (1 điểm / 10.000 đ), lịch sử mua, bán hàng cho khách | Tất cả; xóa: chủ |
| Báo cáo | Doanh thu và lãi gộp 12 tháng, top 10 sản phẩm bán chạy | Chủ |
| Nhân viên | Tạo, khóa tài khoản, đặt lại mật khẩu | Chủ |

## Kiến trúc

```
com.hoangdinh.taphoa
├── App, Launcher                     khởi động JavaFX
├── controller/                       màn hình (FXML controller): chỉ đọc form, gọi service ở luồng nền
├── service/                          nghiệp vụ: kiểm tra dữ liệu, quyền, giao dịch
│   ├── Services                      khởi tạo và nối service với DAO
│   └── Validation                    kiểm tra ràng buộc @NotBlank, @Pattern... trên lớp request
├── dao/                              truy vấn SQL bằng JDBC (PreparedStatement), mỗi nhóm bảng một lớp
├── dto/request, dto/response         dữ liệu trao đổi giữa giao diện và service
├── enums/                            vai trò, tình trạng kho, phương thức thanh toán, trạng thái hóa đơn
├── exception/                        ErrorCode (mã TH-xxxx + thông báo tiếng Việt), BusinessException
├── security/                         Session (tài khoản đang đăng nhập), băm mật khẩu SHA-256
├── config/                           Database (pool HikariCP + giao dịch), Jdbc (hàm tiện ích), LocalDB
└── util/                             định dạng, FormDialog (hộp thoại thêm/sửa), Receipt (phiếu in), chạy nền
```

Luồng một thao tác: **Controller → Service → DAO → SQL Server**. Ví dụ thanh toán ở quầy:
`BanHangController` gửi `HoaDonRequest` → `BanHangService.thanhToan` kiểm tra từng mặt hàng còn bán và đủ tồn kho,
giảm giá không vượt tổng tiền, khách đưa đủ tiền → trong **một giao dịch** gọi `HoaDonDao.insert`, ghi chi tiết với
giá bán và giá vốn hiện tại, `SanPhamDao.truTonKho` (câu UPDATE có điều kiện `tồn kho >= số lượng` nên hai quầy bán
cùng lúc không làm kho âm), cộng điểm cho khách. Lỗi ở bước nào thì không ghi gì cả.

CSDL (`database/taphoa_db.sql`): `tblTaiKhoan`, `tblDanhMuc`, `tblNhaCungCap`, `tblSanPham`, `tblKhachHang`,
`tblHoaDon` + `tblChiTietHoaDon`, `tblPhieuNhap` + `tblChiTietPhieuNhap`, view `vwSanPham` tính tình trạng tồn kho.
Mã hàng `SP0001`, mã thẻ `KH0001`, số hóa đơn `HD000001`, số phiếu `PN00001` là cột tính toán do CSDL sinh.

Sơ đồ CSDL: `database/taphoa_db.erd.json` (mở bằng ERD Editor) và ảnh `database/taphoa_db-2026-11-17T20_55_00.png`.

Báo cáo đồ án: `docs/BaoCao_QuanLyTapHoa.docx`. Ảnh giao diện: `docs/screenshots` (tạo lại bằng lớp `dev/Snapshot`
trong `src/test`).
