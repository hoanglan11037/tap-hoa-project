package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.response.DoanhThuThangResponse;
import com.hoangdinh.taphoa.dto.response.TopSanPhamResponse;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.Ui;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Báo cáo doanh thu – lãi gộp theo tháng và sản phẩm bán chạy trong năm (chỉ chủ cửa hàng). */
public class ThongKeController extends BaseController {

    @FXML private ScrollPane root;
    @FXML private ComboBox<Integer> cbNam;
    @FXML private Label kpiDoanhThu;
    @FXML private Label kpiLai;
    @FXML private Label kpiBien;
    @FXML private Label kpiHd;
    @FXML private Label kpiTb;
    @FXML private Label kpiMax;
    @FXML private Label kpiMaxNote;
    @FXML private BarChart<String, Number> chart;
    @FXML private VBox listTop;
    @FXML private TableView<DoanhThuThangResponse> table;
    @FXML private TableColumn<DoanhThuThangResponse, DoanhThuThangResponse> colThang;
    @FXML private TableColumn<DoanhThuThangResponse, DoanhThuThangResponse> colHd;
    @FXML private TableColumn<DoanhThuThangResponse, DoanhThuThangResponse> colDt;
    @FXML private TableColumn<DoanhThuThangResponse, DoanhThuThangResponse> colLai;
    @FXML private TableColumn<DoanhThuThangResponse, DoanhThuThangResponse> colBien;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        int nam = Year.now().getValue();
        for (int y = nam; y >= nam - 4; y--) {
            cbNam.getItems().add(y);
        }
        cbNam.setValue(nam);
        cbNam.valueProperty().addListener((o, a, b) -> load());
        Ui.textColumn(colThang, d -> "Tháng " + d.getThang());
        Ui.numberColumn(colHd, d -> String.valueOf(d.getSoHoaDon()));
        Ui.numberColumn(colDt, d -> Fmt.money(d.getDoanhThu()));
        Ui.numberColumn(colLai, d -> Fmt.money(d.getLoiNhuan()));
        Ui.numberColumn(colBien, d -> bien(d.getLoiNhuan(), d.getDoanhThu()));
        load();
    }

    private void load() {
        int nam = cbNam.getValue();
        call(() -> Services.THONG_KE.doanhThuNam(nam), this::render);
        call(() -> Services.THONG_KE.topSanPham(nam), this::renderTop);
    }

    private static String bien(BigDecimal lai, BigDecimal dt) {
        return dt.signum() == 0 ? "–"
                : lai.multiply(BigDecimal.valueOf(100)).divide(dt, 1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%";
    }

    private void render(List<DoanhThuThangResponse> list) {
        table.getItems().setAll(list);
        BigDecimal dt = list.stream().map(DoanhThuThangResponse::getDoanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lai = list.stream().map(DoanhThuThangResponse::getLoiNhuan).reduce(BigDecimal.ZERO, BigDecimal::add);
        long hd = list.stream().mapToLong(DoanhThuThangResponse::getSoHoaDon).sum();
        kpiDoanhThu.setText(Fmt.moneyShort(dt));
        kpiLai.setText(Fmt.moneyShort(lai));
        kpiBien.setText("Biên lãi " + bien(lai, dt));
        kpiHd.setText(String.valueOf(hd));
        kpiTb.setText(hd == 0 ? "–" : "TB " + Fmt.money(dt.divide(BigDecimal.valueOf(hd), 0, RoundingMode.HALF_UP)) + " / đơn");
        DoanhThuThangResponse max = list.stream().max(Comparator.comparing(DoanhThuThangResponse::getDoanhThu)).orElse(null);
        if (max != null && max.getDoanhThu().signum() > 0) {
            kpiMax.setText("Tháng " + max.getThang());
            kpiMaxNote.setText(Fmt.money(max.getDoanhThu()));
        } else {
            kpiMax.setText("–");
            kpiMaxNote.setText("Chưa có doanh thu");
        }

        XYChart.Series<String, Number> sDt = new XYChart.Series<>();
        XYChart.Series<String, Number> sLai = new XYChart.Series<>();
        sDt.setName("Doanh thu");
        sLai.setName("Lãi gộp");
        for (DoanhThuThangResponse d : list) {
            sDt.getData().add(new XYChart.Data<>("T" + d.getThang(), d.getDoanhThu().doubleValue() / 1_000_000));
            sLai.getData().add(new XYChart.Data<>("T" + d.getThang(), d.getLoiNhuan().doubleValue() / 1_000_000));
        }
        chart.getData().setAll(List.of(sDt, sLai));
        for (int i = 0; i < list.size(); i++) {
            DoanhThuThangResponse d = list.get(i);
            String tip = "Tháng " + d.getThang() + "\nDoanh thu: " + Fmt.money(d.getDoanhThu())
                    + "\nLãi gộp: " + Fmt.money(d.getLoiNhuan());
            for (XYChart.Series<String, Number> s : List.of(sDt, sLai)) {
                Node bar = s.getData().get(i).getNode();
                if (bar != null) {
                    Tooltip.install(bar, new Tooltip(tip));
                }
            }
        }
    }

    private void renderTop(List<TopSanPhamResponse> list) {
        listTop.getChildren().clear();
        if (list.isEmpty()) {
            Label l = new Label("Chưa có dữ liệu bán hàng");
            l.getStyleClass().addAll("muted", "empty-line");
            listTop.getChildren().add(l);
            return;
        }
        long max = list.get(0).getSoLuong();
        int hang = 1;
        for (TopSanPhamResponse sp : list) {
            Label rank = new Label(String.valueOf(hang++));
            rank.getStyleClass().add("rank");
            Label ten = new Label(sp.getTenSp());
            ten.getStyleClass().add("cell-title");
            ProgressBar bar = new ProgressBar((double) sp.getSoLuong() / Math.max(max, 1));
            bar.getStyleClass().add("top-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            Label sub = new Label(sp.getSoLuong() + " " + sp.getDonVi().toLowerCase() + " · " + Fmt.moneyShort(sp.getDoanhThu()));
            sub.getStyleClass().add("cell-sub");
            VBox info = new VBox(3, ten, bar, sub);
            HBox.setHgrow(info, Priority.ALWAYS);
            HBox row = new HBox(rank, info);
            row.getStyleClass().add("list-row");
            listTop.getChildren().add(row);
        }
    }
}
