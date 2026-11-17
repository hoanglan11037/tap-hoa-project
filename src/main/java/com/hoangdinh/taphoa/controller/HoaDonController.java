package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.Receipt;
import com.hoangdinh.taphoa.util.Ui;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/** Tra cứu hóa đơn theo khoảng ngày, xem phiếu, in lại, hủy (chủ cửa hàng). */
public class HoaDonController extends BaseController {

    @FXML private VBox root;
    @FXML private TextField txtSearch;
    @FXML private ToggleGroup rangeGroup;
    @FXML private ToggleButton rHomNay;
    @FXML private DatePicker dpTu;
    @FXML private DatePicker dpDen;
    @FXML private Label sumSo;
    @FXML private Label sumThu;
    @FXML private Label sumGiam;
    @FXML private Label sumHuy;
    @FXML private TableView<HoaDonResponse> table;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colSo;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colNgay;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colKhach;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colPt;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colTien;
    @FXML private TableColumn<HoaDonResponse, HoaDonResponse> colTt;
    @FXML private ScrollPane receiptScroll;
    @FXML private StackPane receiptHost;
    @FXML private VBox noSelection;
    @FXML private HBox actions;
    @FXML private Button btnHuy;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));
    private HoaDonResponse selected;
    private boolean settingRange;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Ui.nodeColumn(colSo, h -> Ui.code(h.getSoHd()));
        Ui.textColumn(colNgay, h -> Fmt.dateTime(h.getNgayLap()));
        Ui.nodeColumn(colKhach, h -> h.getTenKhachHang() != null
                ? Ui.twoLines(h.getTenKhachHang(), h.getSdtKhachHang()) : Ui.twoLines("Khách lẻ", h.getSoMatHang() + " mặt hàng"));
        Ui.textColumn(colPt, h -> h.getPhuongThuc().getLabel());
        Ui.numberColumn(colTien, h -> Fmt.money(h.getPhaiTra()));
        Ui.nodeColumn(colTt, h -> Ui.pill(h.getTrangThai()));

        Fmt.setupDatePicker(dpTu);
        Fmt.setupDatePicker(dpDen);
        rangeGroup.selectedToggleProperty().addListener((o, a, t) -> {
            if (t != null) {
                int days = Integer.parseInt((String) t.getUserData());
                settingRange = true;
                dpDen.setValue(LocalDate.now());
                dpTu.setValue(LocalDate.now().minusDays(days));
                settingRange = false;
                load();
            }
        });
        dpTu.valueProperty().addListener((o, a, b) -> onDateChanged());
        dpDen.valueProperty().addListener((o, a, b) -> onDateChanged());
        searchDelay.setOnFinished(e -> load());
        txtSearch.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, h) -> {
            if (h != null) {
                openDetail(h.getMaHd());
            }
        });
        rHomNay.setSelected(true);
    }

    private void onDateChanged() {
        if (!settingRange) {
            rangeGroup.selectToggle(null);   // người dùng tự chọn ngày
            load();
        }
    }

    private void load() {
        String kw = Fmt.blankToNull(txtSearch.getText());
        LocalDate tu = dpTu.getValue();
        LocalDate den = dpDen.getValue();
        Integer keep = selected != null ? selected.getMaHd() : null;
        call(() -> Services.BAN_HANG.search(kw, tu, den), list -> {
            table.getItems().setAll(list);
            renderSummary(list);
            list.stream().filter(h -> h.getMaHd().equals(keep)).findFirst()
                    .ifPresentOrElse(h -> table.getSelectionModel().select(h), this::clearDetail);
        });
    }

    private void renderSummary(List<HoaDonResponse> list) {
        List<HoaDonResponse> ok = list.stream().filter(h -> h.getTrangThai() == TrangThaiHoaDon.HOAN_THANH).toList();
        sumSo.setText(String.valueOf(ok.size()));
        sumThu.setText(Fmt.money(ok.stream().map(HoaDonResponse::getPhaiTra).reduce(BigDecimal.ZERO, BigDecimal::add)));
        sumGiam.setText(Fmt.money(ok.stream().map(HoaDonResponse::getGiamGia).reduce(BigDecimal.ZERO, BigDecimal::add)));
        sumHuy.setText(String.valueOf(list.size() - ok.size()));
    }

    private void openDetail(int maHd) {
        call(() -> Services.BAN_HANG.chiTiet(maHd), this::showDetail);
    }

    private void showDetail(HoaDonResponse hd) {
        selected = hd;
        receiptHost.getChildren().setAll(Receipt.of(hd));
        Ui.show(receiptScroll, true);
        Ui.show(noSelection, false);
        Ui.show(actions, true);
        Ui.show(btnHuy, Session.isAdmin() && hd.getTrangThai() == TrangThaiHoaDon.HOAN_THANH);
    }

    private void clearDetail() {
        selected = null;
        Ui.show(receiptScroll, false);
        Ui.show(noSelection, true);
        Ui.show(actions, false);
    }

    @FXML
    private void onIn() {
        if (selected != null) {
            try {
                Receipt.print(root, selected);
            } catch (IllegalStateException e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    private void onHuy() {
        if (selected == null) {
            return;
        }
        String lyDo = Ui.prompt(root, "Hủy hóa đơn", "Hủy hóa đơn " + selected.getSoHd() + "?\n"
                + "Hàng sẽ được hoàn về kho và trừ lại điểm của khách.", "Lý do hủy (không bắt buộc)");
        if (lyDo == null) {
            return;
        }
        int id = selected.getMaHd();
        call(() -> Services.BAN_HANG.huy(id, lyDo), hd -> {
            toast("Đã hủy hóa đơn " + hd.getSoHd());
            load();
        });
    }

    @FXML
    private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất danh sách hóa đơn");
        fc.setInitialFileName("hoa-don-" + LocalDate.now() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
        File file = fc.showSaveDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        List<HoaDonResponse> rows = List.copyOf(table.getItems());
        call(() -> {
            try (PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
                w.print('﻿'); // BOM để Excel đọc đúng tiếng Việt
                w.println("Số HĐ,Thời gian,Khách hàng,Thu ngân,Tiền hàng,Giảm giá,Thanh toán,Hình thức,Trạng thái");
                for (HoaDonResponse h : rows) {
                    w.printf("%s,%s,\"%s\",\"%s\",%s,%s,%s,%s,%s%n", h.getSoHd(), Fmt.dateTime(h.getNgayLap()),
                            h.getTenKhachHang() != null ? h.getTenKhachHang() : "Khách lẻ", h.getThuNgan(),
                            h.getTongTien(), h.getGiamGia(), h.getPhaiTra(), h.getPhuongThuc().getLabel(),
                            h.getTrangThai().getLabel());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Không ghi được file: " + e.getMessage());
            }
            return rows.size();
        }, n -> toast("Đã xuất " + n + " hóa đơn ra " + file.getName()));
    }
}
