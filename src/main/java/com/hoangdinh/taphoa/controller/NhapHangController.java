package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.dto.request.DongNhapRequest;
import com.hoangdinh.taphoa.dto.request.PhieuNhapRequest;
import com.hoangdinh.taphoa.dto.response.ChiTietPhieuNhapResponse;
import com.hoangdinh.taphoa.dto.response.NhaCungCapResponse;
import com.hoangdinh.taphoa.dto.response.PhieuNhapResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.Ui;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Lập phiếu nhập hàng từ nhà cung cấp và xem lại các phiếu đã nhập. */
public class NhapHangController extends BaseController implements MainController.ParamAware {

    /** Dòng hàng đang soạn trên phiếu. */
    private record Dong(SanPhamResponse sp, int soLuong, BigDecimal donGia) {
        BigDecimal thanhTien() {
            return donGia.multiply(BigDecimal.valueOf(soLuong));
        }
    }

    @FXML private VBox root;
    @FXML private ComboBox<NhaCungCapResponse> cbNcc;
    @FXML private ComboBox<SanPhamResponse> cbSanPham;
    @FXML private TextField txtSoLuong;
    @FXML private TextField txtDonGia;
    @FXML private TableView<Dong> tblDong;
    @FXML private TableColumn<Dong, Dong> colSp;
    @FXML private TableColumn<Dong, Dong> colSl;
    @FXML private TableColumn<Dong, Dong> colGia;
    @FXML private TableColumn<Dong, Dong> colTien;
    @FXML private TableColumn<Dong, Dong> colXoa;
    @FXML private TextField txtGhiChu;
    @FXML private Label lblTong;
    @FXML private Button btnLuu;
    @FXML private Label lblError;
    @FXML private TableView<PhieuNhapResponse> tblPhieu;
    @FXML private TableColumn<PhieuNhapResponse, PhieuNhapResponse> colPhieu;
    @FXML private TableColumn<PhieuNhapResponse, PhieuNhapResponse> colPhieuTien;

    private Integer pendingMaSp;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        cbNcc.setCellFactory(l -> cell(NhaCungCapResponse::getTenNcc));
        cbNcc.setButtonCell(cell(NhaCungCapResponse::getTenNcc));
        cbSanPham.setCellFactory(l -> cell(s -> s.getMaHang() + " · " + s.getTenSp() + "  (tồn " + s.getTonKho() + ")"));
        cbSanPham.setButtonCell(cell(s -> s.getMaHang() + " · " + s.getTenSp()));
        cbSanPham.valueProperty().addListener((o, a, sp) -> {
            if (sp != null) {
                txtDonGia.setText(Fmt.moneyPlain(sp.getGiaNhap()));
                txtSoLuong.requestFocus();
            }
        });
        Fmt.intField(txtSoLuong);
        Fmt.moneyField(txtDonGia);
        txtSoLuong.setOnAction(e -> onThemDong());
        txtDonGia.setOnAction(e -> onThemDong());

        Ui.nodeColumn(colSp, d -> Ui.twoLines(d.sp().getTenSp(), d.sp().getMaHang() + " · " + d.sp().getDonVi()));
        Ui.numberColumn(colSl, d -> String.valueOf(d.soLuong()));
        Ui.numberColumn(colGia, d -> Fmt.money(d.donGia()));
        Ui.numberColumn(colTien, d -> Fmt.money(d.thanhTien()));
        Ui.nodeColumn(colXoa, d -> {
            Button x = new Button(null, new FontIcon("mdi2t-trash-can-outline"));
            x.getStyleClass().addAll("icon-btn", "remove");
            x.setOnAction(e -> {
                tblDong.getItems().remove(d);
                tinhTong();
            });
            return x;
        });

        Ui.nodeColumn(colPhieu, p -> Ui.twoLines(p.getSoPn() + " · " + p.getTenNcc(),
                Fmt.dateTime(p.getNgayNhap()) + " · " + p.getSoMatHang() + " mặt hàng"));
        Ui.numberColumn(colPhieuTien, p -> Fmt.money(p.getTongTien()));
        tblPhieu.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                call(() -> Services.NHAP_HANG.chiTiet(p.getMaPn()), this::showPhieu);
            }
        });

        call(() -> Services.NHA_CUNG_CAP.search(null, true), list -> cbNcc.getItems().setAll(list));
        call(() -> Services.SAN_PHAM.search(null, null, null, 0, 200).getItems(), list -> {
            cbSanPham.getItems().setAll(list);
            chonSanPhamCho();
        });
        loadPhieu();
        tinhTong();
    }

    /** Mở từ Tổng quan với mã sản phẩm sắp hết: chọn sẵn sản phẩm đó. */
    @Override
    public void receive(Object param) {
        if (param instanceof Integer maSp) {
            pendingMaSp = maSp;
            chonSanPhamCho();
        }
    }

    private void chonSanPhamCho() {
        if (pendingMaSp != null && !cbSanPham.getItems().isEmpty()) {
            cbSanPham.getItems().stream().filter(s -> s.getMaSp().equals(pendingMaSp)).findFirst()
                    .ifPresent(cbSanPham::setValue);
            pendingMaSp = null;
        }
    }

    private static <T> ListCell<T> cell(Function<T, String> text) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }

    private void loadPhieu() {
        call(Services.NHAP_HANG::findAll, list -> tblPhieu.getItems().setAll(list));
    }

    @FXML
    private void onThemDong() {
        SanPhamResponse sp = cbSanPham.getValue();
        if (sp == null) {
            formError(lblError, "Chọn sản phẩm cần nhập");
            return;
        }
        Integer sl;
        BigDecimal gia;
        try {
            sl = Fmt.parseInt(txtSoLuong.getText(), "Số lượng");
            gia = Fmt.parseMoney(txtDonGia.getText());
        } catch (IllegalArgumentException e) {
            formError(lblError, e.getMessage());
            return;
        }
        if (sl == null || sl <= 0) {
            formError(lblError, "Nhập số lượng lớn hơn 0");
            return;
        }
        if (gia == null) {
            formError(lblError, "Nhập đơn giá nhập");
            return;
        }
        // cùng sản phẩm thì gộp vào dòng cũ (lấy đơn giá mới)
        Dong cu = tblDong.getItems().stream().filter(d -> d.sp().getMaSp().equals(sp.getMaSp())).findFirst().orElse(null);
        if (cu != null) {
            tblDong.getItems().set(tblDong.getItems().indexOf(cu), new Dong(sp, cu.soLuong() + sl, gia));
        } else {
            tblDong.getItems().add(new Dong(sp, sl, gia));
        }
        formError(lblError, null);
        cbSanPham.setValue(null);
        txtSoLuong.clear();
        txtDonGia.clear();
        cbSanPham.requestFocus();
        tinhTong();
    }

    private void tinhTong() {
        lblTong.setText(Fmt.money(tblDong.getItems().stream().map(Dong::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add)));
        btnLuu.setDisable(tblDong.getItems().isEmpty());
    }

    @FXML
    private void onLuu() {
        if (cbNcc.getValue() == null) {
            formError(lblError, "Chọn nhà cung cấp");
            return;
        }
        PhieuNhapRequest req = PhieuNhapRequest.builder()
                .maNcc(cbNcc.getValue().getMaNcc())
                .ghiChu(txtGhiChu.getText())
                .items(tblDong.getItems().stream()
                        .map(d -> new DongNhapRequest(d.sp().getMaSp(), d.soLuong(), d.donGia())).toList())
                .build();
        btnLuu.setDisable(true);
        call(() -> Services.NHAP_HANG.create(req), pn -> {
            toast("Đã lưu phiếu " + pn.getSoPn() + " · " + Fmt.money(pn.getTongTien()));
            tblDong.getItems().clear();
            txtGhiChu.clear();
            formError(lblError, null);
            tinhTong();
            loadPhieu();
            // giá nhập / tồn kho đã đổi
            call(() -> Services.SAN_PHAM.search(null, null, null, 0, 200).getItems(), list -> cbSanPham.getItems().setAll(list));
        }, msg -> {
            btnLuu.setDisable(false);
            formError(lblError, msg);
        });
    }

    private void showPhieu(PhieuNhapResponse pn) {
        TableView<ChiTietPhieuNhapResponse> t = new TableView<>();
        t.getStyleClass().add("grid");
        TableColumn<ChiTietPhieuNhapResponse, ChiTietPhieuNhapResponse> sp = new TableColumn<>("Sản phẩm");
        TableColumn<ChiTietPhieuNhapResponse, ChiTietPhieuNhapResponse> sl = new TableColumn<>("SL");
        TableColumn<ChiTietPhieuNhapResponse, ChiTietPhieuNhapResponse> gia = new TableColumn<>("Đơn giá");
        TableColumn<ChiTietPhieuNhapResponse, ChiTietPhieuNhapResponse> tien = new TableColumn<>("Thành tiền");
        Ui.nodeColumn(sp, c -> Ui.twoLines(c.getTenSp(), c.getMaHang()));
        Ui.numberColumn(sl, c -> c.getSoLuong() + " " + c.getDonVi().toLowerCase());
        Ui.numberColumn(gia, c -> Fmt.money(c.getDonGia()));
        Ui.numberColumn(tien, c -> Fmt.money(c.getThanhTien()));
        sp.setPrefWidth(230);
        List.of(sl, gia, tien).forEach(c -> c.setPrefWidth(110));
        t.getColumns().setAll(List.of(sp, sl, gia, tien));
        t.getItems().setAll(pn.getChiTiet());
        t.setPrefSize(580, 300);

        Label info = new Label(pn.getTenNcc() + " · " + Fmt.dateTime(pn.getNgayNhap()) + " · người nhập: " + pn.getNguoiNhap()
                + (pn.getGhiChu() != null ? "\n" + pn.getGhiChu() : ""));
        info.getStyleClass().add("muted");
        Label tong = new Label("Tổng tiền: " + Fmt.money(pn.getTongTien()));
        tong.getStyleClass().add("import-total");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(root.getScene().getWindow());
        a.setTitle("Phiếu nhập " + pn.getSoPn());
        a.setHeaderText("Phiếu nhập " + pn.getSoPn());
        a.setGraphic(new FontIcon("mdi2t-truck-delivery-outline"));
        a.getDialogPane().setContent(new VBox(10, info, t, tong));
        a.getDialogPane().getStylesheets().add(App.class.getResource(App.CSS).toExternalForm());
        a.showAndWait();
        tblPhieu.getSelectionModel().clearSelection();
    }
}
