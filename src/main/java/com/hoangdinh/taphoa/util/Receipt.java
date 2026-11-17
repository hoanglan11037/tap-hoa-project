package com.hoangdinh.taphoa.util;

import com.hoangdinh.taphoa.dto.response.ChiTietHoaDonResponse;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import com.hoangdinh.taphoa.service.BanHangService;
import java.math.BigDecimal;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Vẽ hóa đơn dạng "phiếu tính tiền" khổ hẹp: dùng cho xem lại, hộp thoại sau thanh toán và in. */
public final class Receipt {

    public static final String TEN_CUA_HANG = "TẠP HÓA HOÀNG ĐINH";
    public static final String DIA_CHI = "123 Đường Số 9, P. Bình Hưng Hòa, TP.HCM";

    private Receipt() {
    }

    public static VBox of(HoaDonResponse hd) {
        VBox r = new VBox(3);
        r.getStyleClass().add("receipt");
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            r.getStyleClass().add("receipt-void");
        }
        r.getChildren().addAll(center(TEN_CUA_HANG, "rc-shop"), center(DIA_CHI, "rc-small"), dashed(),
                center("HÓA ĐƠN BÁN HÀNG", "rc-heading"),
                pair("Số", hd.getSoHd()), pair("Ngày", Fmt.dateTime(hd.getNgayLap())),
                pair("Thu ngân", hd.getThuNgan()),
                pair("Khách", hd.getTenKhachHang() != null ? hd.getTenKhachHang() : "Khách lẻ"), dashed());
        if (hd.getChiTiet() != null) {
            for (ChiTietHoaDonResponse c : hd.getChiTiet()) {
                Label ten = new Label(c.getTenSp());
                ten.getStyleClass().add("rc-item");
                ten.setWrapText(true);
                r.getChildren().addAll(ten,
                        pair("  " + c.getSoLuong() + " " + c.getDonVi().toLowerCase() + " x " + Fmt.moneyPlain(c.getDonGia()),
                                Fmt.moneyPlain(c.getThanhTien())));
            }
        }
        r.getChildren().addAll(dashed(), pair("Tiền hàng", Fmt.money(hd.getTongTien())));
        if (hd.getGiamGia().signum() > 0) {
            r.getChildren().add(pair("Giảm giá", "-" + Fmt.money(hd.getGiamGia())));
        }
        HBox total = pair("TỔNG CỘNG", Fmt.money(hd.getPhaiTra()));
        total.getStyleClass().add("rc-total");
        r.getChildren().addAll(total, pair(hd.getPhuongThuc().getLabel(), Fmt.money(hd.getKhachTra())));
        if (hd.getTienThua() != null && hd.getTienThua().signum() > 0) {
            r.getChildren().add(pair("Tiền thừa", Fmt.money(hd.getTienThua())));
        }
        if (hd.getMaKh() != null && hd.getTrangThai() == TrangThaiHoaDon.HOAN_THANH) {
            r.getChildren().add(pair("Điểm tích lũy", "+" + BanHangService.diem(nonNull(hd.getPhaiTra()))));
        }
        r.getChildren().add(dashed());
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            r.getChildren().add(center("*** HÓA ĐƠN ĐÃ HỦY ***", "rc-void"));
            if (hd.getGhiChu() != null) {
                r.getChildren().add(center(hd.getGhiChu(), "rc-small"));
            }
        } else {
            r.getChildren().add(center("Cảm ơn quý khách, hẹn gặp lại!", "rc-small"));
        }
        return r;
    }

    /** In phiếu bằng máy in mặc định (hiện hộp thoại chọn máy in). Trả về false nếu người dùng hủy. */
    public static boolean print(Node owner, HoaDonResponse hd) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            throw new IllegalStateException("Máy tính chưa cài máy in");
        }
        if (!job.showPrintDialog(owner.getScene().getWindow())) {
            return false;
        }
        VBox paper = of(hd);
        paper.getStylesheets().addAll(owner.getScene().getStylesheets());
        paper.applyCss();
        boolean ok = job.printPage(paper);
        if (ok) {
            job.endJob();
        }
        return ok;
    }

    private static BigDecimal nonNull(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static Label center(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().add(style);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        l.setWrapText(true);
        return l;
    }

    private static HBox pair(String left, String right) {
        Label l = new Label(left);
        Label rt = new Label(right);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(8, l, gap, rt);
        row.getStyleClass().add("rc-row");
        return row;
    }

    /** Đường kẻ chấm ngăn các phần của phiếu. */
    private static Node dashed() {
        Region line = new Region();
        line.getStyleClass().add("rc-dash");
        VBox box = new VBox(line);
        box.getStyleClass().add("rc-gap");
        return box;
    }
}
