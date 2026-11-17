package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.response.DoanhThuNgayResponse;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.dto.response.TongQuanResponse;
import com.hoangdinh.taphoa.dto.response.TopSanPhamResponse;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.Ui;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TongQuanController extends BaseController {

    private static final DateTimeFormatter THU = DateTimeFormatter.ofPattern("EEE dd/MM", new Locale("vi", "VN"));

    @FXML private ScrollPane root;
    @FXML private Label lblChao;
    @FXML private Label lblChaoSub;
    @FXML private Label kpiDoanhThu;
    @FXML private Label kpiDoanhThuNote;
    @FXML private Label kpiHoaDon;
    @FXML private Label kpiHoaDonNote;
    @FXML private Label kpiLoiNhuan;
    @FXML private Label kpiLoiNhuanNote;
    @FXML private Label kpiKho;
    @FXML private Label kpiKhoNote;
    @FXML private AreaChart<String, Number> chart;
    @FXML private Label lblTong7Ngay;
    @FXML private VBox listCanNhap;
    @FXML private VBox listBanChay;
    @FXML private TableView<HoaDonResponse> tblHoaDon;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colSo;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colGio;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colKhach;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colTien;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colTt;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        int gio = LocalTime.now().getHour();
        String buoi = gio < 11 ? "buổi sáng" : gio < 14 ? "buổi trưa" : gio < 18 ? "buổi chiều" : "buổi tối";
        lblChao.setText("Chào " + buoi + ", " + Session.current().getHoTen());

        Ui.nodeColumn(colSo, h -> Ui.code(h.getSoHd()));
        Ui.textColumn(colGio, h -> Fmt.dateTime(h.getNgayLap()));
        Ui.textColumn(colKhach, h -> h.getTenKhachHang() != null ? h.getTenKhachHang() : "Khách lẻ");
        Ui.numberColumn(colTien, h -> Fmt.money(h.getPhaiTra()));
        Ui.nodeColumn(colTt, h -> Ui.pill(h.getTrangThai()));
        reload();
    }

    @FXML
    private void reload() {
        call(Services.THONG_KE::tongQuan, this::render);
    }

    private void render(TongQuanResponse t) {
        lblChaoSub.setText("Hôm nay cửa hàng đã bán " + t.getHoaDonHomNay() + " hóa đơn, thu về "
                + Fmt.money(t.getDoanhThuHomNay()) + ".");
        kpiDoanhThu.setText(Fmt.moneyShort(t.getDoanhThuHomNay()));
        String pct = Fmt.percentChange(t.getDoanhThuHomNay(), t.getDoanhThuHomQua());
        kpiDoanhThuNote.setText(pct != null ? pct + " so với hôm qua" : "Hôm qua chưa có doanh thu");
        kpiDoanhThuNote.getStyleClass().removeAll("up", "down");
        if (pct != null) {
            kpiDoanhThuNote.getStyleClass().add(pct.startsWith("-") ? "down" : "up");
        }
        kpiHoaDon.setText(String.valueOf(t.getHoaDonHomNay()));
        kpiHoaDonNote.setText(t.getHoaDonHomNay() == 0 ? "Chưa có hóa đơn"
                : "TB " + Fmt.money(t.getDoanhThuHomNay().divide(BigDecimal.valueOf(t.getHoaDonHomNay()), 0,
                java.math.RoundingMode.HALF_UP)) + " / đơn");
        kpiLoiNhuan.setText(Fmt.moneyShort(t.getLoiNhuanHomNay()));
        kpiLoiNhuanNote.setText(t.getDoanhThuHomNay().signum() == 0 ? "–"
                : "Biên lãi " + t.getLoiNhuanHomNay().multiply(BigDecimal.valueOf(100))
                .divide(t.getDoanhThuHomNay(), 0, java.math.RoundingMode.HALF_UP) + "%");
        kpiKho.setText(String.valueOf(t.getSapHetHang() + t.getHetHang()));
        kpiKhoNote.setText(t.getHetHang() + " hết hàng · " + t.getSapHetHang() + " sắp hết");

        renderChart(t.getDoanhThu7Ngay());
        renderCanNhap(t.getCanNhapThem());
        renderBanChay(t.getBanChayHomNay());
        tblHoaDon.getItems().setAll(t.getHoaDonGanDay());
    }

    private void renderChart(List<DoanhThuNgayResponse> list) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        BigDecimal tong = BigDecimal.ZERO;
        for (DoanhThuNgayResponse d : list) {
            s.getData().add(new XYChart.Data<>(d.getNgay().format(THU), d.getDoanhThu().doubleValue() / 1000));
            tong = tong.add(d.getDoanhThu());
        }
        chart.getData().setAll(List.of(s));
        for (int i = 0; i < list.size(); i++) {
            Node dot = s.getData().get(i).getNode();
            DoanhThuNgayResponse d = list.get(i);
            if (dot != null) {
                Tooltip.install(dot, new Tooltip(Fmt.date(d.getNgay()) + ": " + Fmt.money(d.getDoanhThu())
                        + " · " + d.getSoHoaDon() + " hóa đơn"));
            }
        }
        lblTong7Ngay.setText("Tổng " + Fmt.money(tong));
    }

    private void renderCanNhap(List<SanPhamResponse> list) {
        listCanNhap.getChildren().clear();
        if (list.isEmpty()) {
            listCanNhap.getChildren().add(empty("Kho đang đủ hàng"));
            return;
        }
        for (SanPhamResponse sp : list) {
            Label qty = new Label(String.valueOf(sp.getTonKho()));
            qty.getStyleClass().addAll("qty-chip", sp.getTinhTrang() == TinhTrangKho.HET_HANG ? "qty-out" : "qty-low");
            Node info = Ui.twoLines(sp.getTenSp(), sp.getDonVi() + " · tối thiểu " + sp.getTonToiThieu());
            HBox.setHgrow(info, Priority.ALWAYS);
            Button nhap = new Button("Nhập");
            nhap.getStyleClass().addAll("btn-outline", "btn-small");
            nhap.setOnAction(e -> MainController.open("NhapHang", sp.getMaSp()));
            HBox row = new HBox(qty, info, nhap);
            row.getStyleClass().add("list-row");
            listCanNhap.getChildren().add(row);
        }
    }

    private void renderBanChay(List<TopSanPhamResponse> list) {
        listBanChay.getChildren().clear();
        if (list.isEmpty()) {
            listBanChay.getChildren().add(empty("Hôm nay chưa bán được sản phẩm nào"));
            return;
        }
        int hang = 1;
        for (TopSanPhamResponse sp : list) {
            Label rank = new Label(String.valueOf(hang++));
            rank.getStyleClass().add("rank");
            Node info = Ui.twoLines(sp.getTenSp(), sp.getSoLuong() + " " + sp.getDonVi().toLowerCase());
            HBox.setHgrow(info, Priority.ALWAYS);
            Label tien = new Label(Fmt.money(sp.getDoanhThu()));
            tien.getStyleClass().add("money");
            HBox row = new HBox(rank, info, tien);
            row.getStyleClass().add("list-row");
            listBanChay.getChildren().add(row);
        }
    }

    private static Label empty(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("muted", "empty-line");
        return l;
    }

    @FXML
    private void openBanHang() {
        MainController.open("BanHang", null);
    }

    @FXML
    private void openSanPham() {
        MainController.open("SanPham", null);
    }

    @FXML
    private void openHoaDon() {
        MainController.open("HoaDon", null);
    }
}
