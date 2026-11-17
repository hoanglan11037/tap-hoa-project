package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.dto.request.DongHangRequest;
import com.hoangdinh.taphoa.dto.request.HoaDonRequest;
import com.hoangdinh.taphoa.dto.request.KhachHangRequest;
import com.hoangdinh.taphoa.dto.response.DanhMucResponse;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.KhachHangResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.enums.PhuongThucThanhToan;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Receipt;
import com.hoangdinh.taphoa.util.Ui;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/** Quầy bán hàng: chọn / quét sản phẩm vào giỏ, chọn khách, thanh toán và in phiếu. */
public class BanHangController extends BaseController implements MainController.ParamAware {

    private static final long[] MENH_GIA = {50_000, 100_000, 200_000, 500_000};

    /** Một dòng trong giỏ hàng. */
    private static final class Dong {
        final SanPhamResponse sp;
        int soLuong;

        Dong(SanPhamResponse sp) {
            this.sp = sp;
        }

        BigDecimal thanhTien() {
            return sp.getGiaBan().multiply(BigDecimal.valueOf(soLuong));
        }
    }

    @FXML private HBox root;
    @FXML private TextField txtScan;
    @FXML private HBox boxDanhMuc;
    @FXML private ToggleGroup dmGroup;
    @FXML private FlowPane grid;
    @FXML private Label lblSoMon;
    @FXML private HBox boxTimKhach;
    @FXML private TextField txtKhach;
    @FXML private HBox boxKhach;
    @FXML private Label lblKhachTen;
    @FXML private Label lblKhachDiem;
    @FXML private VBox cartList;
    @FXML private VBox cartEmpty;
    @FXML private Label lblTongTien;
    @FXML private TextField txtGiamGia;
    @FXML private Label lblPhaiTra;
    @FXML private ToggleGroup ptGroup;
    @FXML private VBox boxTienMat;
    @FXML private TextField txtKhachDua;
    @FXML private HBox boxMenhGia;
    @FXML private Label lblTienThua;
    @FXML private Label lblError;
    @FXML private Button btnThanhToan;

    private final Map<Integer, Dong> gio = new LinkedHashMap<>();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(300));
    private KhachHangResponse khach;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Fmt.moneyField(txtGiamGia);
        Fmt.moneyField(txtKhachDua);
        txtGiamGia.textProperty().addListener((o, a, b) -> tinhTien());
        txtKhachDua.textProperty().addListener((o, a, b) -> tinhTien());
        ptGroup.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
                return;
            }
            Ui.show(boxTienMat, phuongThuc() == PhuongThucThanhToan.TIEN_MAT);
            tinhTien();
        });
        dmGroup.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
            } else {
                loadSanPham();
            }
        });
        searchDelay.setOnFinished(e -> loadSanPham());
        txtScan.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        txtScan.setOnAction(e -> onScan());
        txtScan.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                txtScan.clear();
            }
        });

        Button dungTien = new Button("Đủ tiền");
        dungTien.setOnAction(e -> txtKhachDua.setText(Fmt.moneyPlain(phaiTra())));
        boxMenhGia.getChildren().add(dungTien);
        for (long m : MENH_GIA) {
            Button b = new Button(m / 1000 + "k");
            b.setOnAction(e -> txtKhachDua.setText(Fmt.moneyPlain(BigDecimal.valueOf(m))));
            boxMenhGia.getChildren().add(b);
        }
        boxMenhGia.getChildren().forEach(n -> {
            HBox.setHgrow(n, Priority.ALWAYS);
            ((Button) n).setMaxWidth(Double.MAX_VALUE);
        });

        root.sceneProperty().addListener((o, a, scene) -> {
            if (scene != null) {
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F9), this::onThanhToan);
            }
        });

        call(Services.DANH_MUC::findAll, this::renderDanhMuc);
        loadSanPham();
        renderGio();
        Platform.runLater(txtScan::requestFocus);
    }

    /** Mở từ màn Khách hàng: bán cho khách đó. */
    @Override
    public void receive(Object param) {
        if (param instanceof KhachHangResponse kh) {
            chonKhach(kh);
        }
    }

    // ---------- Lưới sản phẩm ----------
    private void renderDanhMuc(List<DanhMucResponse> list) {
        for (DanhMucResponse dm : list) {
            ToggleButton t = new ToggleButton(dm.getTenDm());
            t.getStyleClass().add("cat");
            t.setUserData(dm.getMaDm());
            t.setToggleGroup(dmGroup);
            boxDanhMuc.getChildren().add(t);
        }
    }

    private void loadSanPham() {
        String kw = Fmt.blankToNull(txtScan.getText());
        Integer maDm = dmGroup.getSelectedToggle() != null ? (Integer) dmGroup.getSelectedToggle().getUserData() : null;
        call(() -> Services.SAN_PHAM.dangBan(kw, maDm), this::renderGrid);
    }

    private void renderGrid(List<SanPhamResponse> list) {
        grid.getChildren().clear();
        if (list.isEmpty()) {
            Label l = new Label("Không tìm thấy sản phẩm phù hợp");
            l.getStyleClass().addAll("muted", "empty-line");
            grid.getChildren().add(l);
        }
        for (SanPhamResponse sp : list) {
            grid.getChildren().add(tile(sp));
        }
    }

    private Node tile(SanPhamResponse sp) {
        Label cat = new Label(sp.getTenDm());
        cat.getStyleClass().add("tile-cat");
        Label name = new Label(sp.getTenSp());
        name.getStyleClass().add("tile-name");
        name.setWrapText(true);
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        Label price = new Label(Fmt.money(sp.getGiaBan()));
        price.getStyleClass().add("tile-price");
        Label stock = new Label(sp.getTonKho() == 0 ? "Hết hàng" : "Còn " + sp.getTonKho() + " " + sp.getDonVi().toLowerCase());
        stock.getStyleClass().add("tile-stock");
        if (sp.getTinhTrang() == TinhTrangKho.SAP_HET) {
            stock.getStyleClass().add("tile-stock-low");
        }
        VBox tile = new VBox(4, cat, name, gap, price, stock);
        tile.getStyleClass().add("tile");
        if (sp.getTonKho() == 0) {
            tile.getStyleClass().add("tile-out");
            tile.setDisable(true);
        }
        tile.setOnMouseClicked(e -> them(sp, 1));
        return tile;
    }

    /** Enter ở ô quét: khớp đúng mã vạch / mã hàng thì thêm luôn; nếu lưới chỉ còn 1 sản phẩm thì thêm sản phẩm đó. */
    private void onScan() {
        String ma = txtScan.getText().trim();
        if (ma.isEmpty()) {
            return;
        }
        searchDelay.stop();
        call(() -> {
            try {
                return Services.SAN_PHAM.findByMa(ma);
            } catch (RuntimeException notFound) {
                List<SanPhamResponse> found = Services.SAN_PHAM.dangBan(ma, null);
                if (found.size() == 1) {
                    return found.get(0);
                }
                throw notFound;
            }
        }, sp -> {
            them(sp, 1);
            txtScan.clear();
        }, msg -> loadSanPham());
    }

    // ---------- Giỏ hàng ----------
    private void them(SanPhamResponse sp, int soLuong) {
        Dong d = gio.computeIfAbsent(sp.getMaSp(), k -> new Dong(sp));
        if (d.soLuong + soLuong > sp.getTonKho()) {
            hienLoi("\"" + sp.getTenSp() + "\" chỉ còn " + sp.getTonKho() + " " + sp.getDonVi().toLowerCase());
            if (d.soLuong == 0) {
                gio.remove(sp.getMaSp());
            }
            return;
        }
        d.soLuong += soLuong;
        if (d.soLuong <= 0) {
            gio.remove(sp.getMaSp());
        }
        hienLoi(null);
        renderGio();
        txtScan.requestFocus();
    }

    private void renderGio() {
        cartList.getChildren().clear();
        for (Dong d : gio.values()) {
            cartList.getChildren().add(cartRow(d));
        }
        Ui.show(cartEmpty, gio.isEmpty());
        int soMon = gio.values().stream().mapToInt(d -> d.soLuong).sum();
        lblSoMon.setText(String.valueOf(soMon));
        tinhTien();
    }

    private Node cartRow(Dong d) {
        Label ten = new Label(d.sp.getTenSp());
        ten.getStyleClass().add("cell-title");
        ten.setWrapText(true);
        Label gia = new Label(Fmt.money(d.sp.getGiaBan()) + " / " + d.sp.getDonVi().toLowerCase());
        gia.getStyleClass().add("cell-sub");
        VBox info = new VBox(2, ten, gia);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button minus = stepper("mdi2m-minus", () -> them(d.sp, -1));
        Label qty = new Label(String.valueOf(d.soLuong));
        qty.getStyleClass().add("qty");
        Button plus = stepper("mdi2p-plus", () -> them(d.sp, 1));
        HBox step = new HBox(minus, qty, plus);
        step.getStyleClass().add("stepper");
        step.setAlignment(Pos.CENTER);

        Label tien = new Label(Fmt.money(d.thanhTien()));
        tien.getStyleClass().add("line-total");
        Button xoa = stepper("mdi2t-trash-can-outline", () -> {
            gio.remove(d.sp.getMaSp());
            renderGio();
        });
        xoa.getStyleClass().add("remove");
        VBox right = new VBox(6, tien, new HBox(6, step, xoa));
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(10, info, right);
        row.getStyleClass().add("cart-row");
        return row;
    }

    private static Button stepper(String icon, Runnable action) {
        Button b = new Button(null, new FontIcon(icon));
        b.getStyleClass().add("step-btn");
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    @FXML
    private void onClearCart() {
        gio.clear();
        txtGiamGia.clear();
        txtKhachDua.clear();
        hienLoi(null);
        renderGio();
    }

    // ---------- Tiền ----------
    private BigDecimal tongTien() {
        return gio.values().stream().map(Dong::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal giamGia() {
        BigDecimal g = Fmt.parseMoney(txtGiamGia.getText());
        return g != null ? g : BigDecimal.ZERO;
    }

    private BigDecimal phaiTra() {
        return tongTien().subtract(giamGia()).max(BigDecimal.ZERO);
    }

    private PhuongThucThanhToan phuongThuc() {
        return PhuongThucThanhToan.valueOf((String) ptGroup.getSelectedToggle().getUserData());
    }

    private void tinhTien() {
        lblTongTien.setText(Fmt.money(tongTien()));
        lblPhaiTra.setText(Fmt.money(phaiTra()));
        BigDecimal dua = Fmt.parseMoney(txtKhachDua.getText());
        if (dua == null) {
            lblTienThua.setText("–");
            lblTienThua.getStyleClass().remove("change-bad");
        } else {
            BigDecimal thua = dua.subtract(phaiTra());
            lblTienThua.setText(thua.signum() < 0 ? "Thiếu " + Fmt.money(thua.negate()) : Fmt.money(thua));
            lblTienThua.getStyleClass().remove("change-bad");
            if (thua.signum() < 0) {
                lblTienThua.getStyleClass().add("change-bad");
            }
        }
        btnThanhToan.setDisable(gio.isEmpty());
    }

    // ---------- Khách hàng ----------
    @FXML
    private void onTimKhach() {
        String key = txtKhach.getText().trim();
        if (key.isEmpty()) {
            return;
        }
        call(() -> Services.KHACH_HANG.timTaiQuay(key), this::chonKhach, this::hienLoi);
    }

    private void chonKhach(KhachHangResponse kh) {
        khach = kh;
        lblKhachTen.setText(kh.getHoTen() + " · " + kh.getMaThe());
        lblKhachDiem.setText(kh.getSdt() + " · " + kh.getDiemTichLuy() + " điểm");
        Ui.show(boxKhach, true);
        Ui.show(boxTimKhach, false);
        hienLoi(null);
    }

    @FXML
    private void onBoKhach() {
        khach = null;
        txtKhach.clear();
        Ui.show(boxKhach, false);
        Ui.show(boxTimKhach, true);
    }

    @FXML
    private void onKhachMoi() {
        TextField ten = new TextField();
        TextField sdt = new TextField(txtKhach.getText().trim());
        TextField diaChi = new TextField();
        VBox form = new VBox(12, Ui.field("Họ tên *", ten), Ui.field("Số điện thoại *", sdt), Ui.field("Địa chỉ", diaChi));
        form.setPrefWidth(380);
        new FormDialog(root, "mdi2a-account-plus-outline", "Khách hàng mới", "Tích 1 điểm cho mỗi 10.000 đ", form, "Thêm khách")
                .onSubmit(() -> {
                    KhachHangRequest req = KhachHangRequest.builder().hoTen(ten.getText())
                            .sdt(sdt.getText().trim()).diaChi(diaChi.getText()).build();
                    return () -> Services.KHACH_HANG.create(req);
                }, kh -> {
                    chonKhach(kh);
                    toast("Đã thêm khách " + kh.getMaThe());
                })
                .show();
    }

    // ---------- Thanh toán ----------
    @FXML
    private void onThanhToan() {
        if (gio.isEmpty() || btnThanhToan.isDisabled()) {
            return;
        }
        PhuongThucThanhToan pt = phuongThuc();
        BigDecimal dua = Fmt.parseMoney(txtKhachDua.getText());
        if (pt == PhuongThucThanhToan.TIEN_MAT && dua != null && dua.compareTo(phaiTra()) < 0) {
            hienLoi("Khách đưa chưa đủ tiền");
            return;
        }
        HoaDonRequest req = HoaDonRequest.builder()
                .maKh(khach != null ? khach.getMaKh() : null)
                .items(gio.values().stream().map(d -> new DongHangRequest(d.sp.getMaSp(), d.soLuong)).toList())
                .giamGia(giamGia())
                .khachTra(pt == PhuongThucThanhToan.TIEN_MAT ? dua : null)
                .phuongThuc(pt)
                .build();
        btnThanhToan.setDisable(true);
        call(() -> Services.BAN_HANG.thanhToan(req), hd -> {
            onClearCart();
            onBoKhach();
            loadSanPham();
            toast("Đã thanh toán hóa đơn " + hd.getSoHd());
            showReceipt(hd);
        }, msg -> {
            btnThanhToan.setDisable(false);
            hienLoi(msg);
            loadSanPham();   // tồn kho có thể đã thay đổi
        });
    }

    private void showReceipt(HoaDonResponse hd) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(root.getScene().getWindow());
        dlg.setTitle("Thanh toán thành công");
        VBox box = new VBox(12);
        Label ok = new Label("Thanh toán thành công", new FontIcon("mdi2c-check-circle-outline"));
        ok.getStyleClass().add("success-title");
        box.getChildren().addAll(ok, Receipt.of(hd));
        box.setPrefWidth(340);
        dlg.getDialogPane().setContent(box);
        dlg.getDialogPane().getStylesheets().add(App.class.getResource(App.CSS).toExternalForm());
        ButtonType in = new ButtonType("In phiếu", ButtonBar.ButtonData.LEFT);
        ButtonType moi = new ButtonType("Đơn mới", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(in, moi);
        dlg.getDialogPane().lookupButton(moi).getStyleClass().add("btn-primary");
        dlg.getDialogPane().lookupButton(in).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            try {
                Receipt.print(root, hd);
            } catch (IllegalStateException ex) {
                showError(ex.getMessage());
            }
        });
        dlg.showAndWait();
        txtScan.requestFocus();
    }

    private void hienLoi(String msg) {
        formError(lblError, msg);
    }
}
