package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.request.SanPhamRequest;
import com.hoangdinh.taphoa.dto.response.DanhMucResponse;
import com.hoangdinh.taphoa.dto.response.DemTonKhoResponse;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import com.hoangdinh.taphoa.dto.response.SanPhamResponse;
import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class SanPhamController extends BaseController implements MainController.ParamAware {

    private static final int PAGE_SIZE = 25;

    @FXML private VBox root;
    @FXML private Label lblTong;
    @FXML private Button btnThem;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<DanhMucResponse> cbDanhMuc;
    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleButton fAll;
    @FXML private ToggleButton fCon;
    @FXML private ToggleButton fSap;
    @FXML private ToggleButton fHet;
    @FXML private ToggleButton fNgung;
    @FXML private TableView<SanPhamResponse> table;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colMa;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colTen;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colDm;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colGiaBan;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colGiaNhap;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colTon;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colTt;
    @FXML private TableColumn<SanPhamResponse, SanPhamResponse> colAct;
    @FXML private Label lblPage;
    @FXML private Label lblHint;
    @FXML private Label lblPageNo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));
    private final boolean admin = Session.isAdmin();
    private int page;
    private PageResponse<SanPhamResponse> current;
    private Integer pendingMaDm;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Ui.nodeColumn(colMa, s -> Ui.code(s.getMaHang()));
        Ui.nodeColumn(colTen, s -> Ui.twoLines(s.getTenSp(),
                (s.getMaVach() != null ? s.getMaVach() : "Chưa có mã vạch") + " · " + s.getDonVi()));
        Ui.textColumn(colDm, SanPhamResponse::getTenDm);
        Ui.numberColumn(colGiaBan, s -> Fmt.money(s.getGiaBan()));
        Ui.numberColumn(colGiaNhap, s -> Fmt.money(s.getGiaNhap()));
        Ui.nodeColumn(colTon, this::stockCell);
        Ui.nodeColumn(colTt, s -> Ui.pill(s.getTinhTrang()));
        Ui.nodeColumn(colAct, s -> {
            Button edit = new Button(null, new FontIcon("mdi2p-pencil-outline"));
            edit.getStyleClass().add("icon-btn");
            edit.setOnAction(e -> openForm(s));
            return edit;
        });
        // thu ngân chỉ xem: ẩn giá nhập và các nút sửa
        colGiaNhap.setVisible(admin);
        colAct.setVisible(admin);
        Ui.show(btnThem, admin);
        Ui.show(lblHint, admin);
        table.setRowFactory(tv -> {
            TableRow<SanPhamResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (admin && e.getClickCount() == 2 && !row.isEmpty()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });

        cbDanhMuc.setCellFactory(l -> danhMucCell());
        cbDanhMuc.setButtonCell(danhMucCell());
        cbDanhMuc.valueProperty().addListener((o, a, b) -> reload());
        searchDelay.setOnFinished(e -> reload());
        txtSearch.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        filterGroup.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
            } else {
                reload();
            }
        });

        call(Services.DANH_MUC::findAll, list -> {
            cbDanhMuc.getItems().setAll((DanhMucResponse) null);
            cbDanhMuc.getItems().addAll(list);
            if (pendingMaDm != null) {
                list.stream().filter(d -> d.getMaDm().equals(pendingMaDm)).findFirst().ifPresent(cbDanhMuc::setValue);
                pendingMaDm = null;
            }
        });
        load();
        loadCounts();
    }

    /** Mở từ màn khác: tham số là từ khóa (String) hoặc mã danh mục cần lọc (Integer). */
    @Override
    public void receive(Object param) {
        if (param instanceof String keyword) {
            txtSearch.setText(keyword);
        } else if (param instanceof Integer maDm) {
            pendingMaDm = maDm;
        }
    }

    private static ListCell<DanhMucResponse> danhMucCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(DanhMucResponse d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty ? null : d == null ? "Mọi danh mục" : d.getTenDm());
            }
        };
    }

    /** Số tồn + thanh nhỏ so với mức tồn tối thiểu (đầy khi tồn gấp 3 lần mức tối thiểu). */
    private Node stockCell(SanPhamResponse s) {
        Label n = new Label(s.getTonKho() + " " + s.getDonVi().toLowerCase());
        n.getStyleClass().add("cell-title");
        double max = Math.max(s.getTonToiThieu() * 3.0, 1);
        ProgressBar bar = new ProgressBar(Math.min(1, s.getTonKho() / max));
        bar.getStyleClass().addAll("stock-bar", "stock-" + s.getTinhTrang().name().toLowerCase());
        bar.setPrefWidth(70);
        HBox box = new HBox(10, n, bar);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ---------- Danh sách ----------
    private void reload() {
        page = 0;
        load();
    }

    private void load() {
        String kw = Fmt.blankToNull(txtSearch.getText());
        Integer maDm = cbDanhMuc.getValue() != null ? cbDanhMuc.getValue().getMaDm() : null;
        Object f = filterGroup.getSelectedToggle() != null ? filterGroup.getSelectedToggle().getUserData() : null;
        TinhTrangKho tt = f != null ? TinhTrangKho.valueOf((String) f) : null;
        int p = page;
        call(() -> Services.SAN_PHAM.search(kw, maDm, tt, p, PAGE_SIZE), res -> {
            current = res;
            table.getItems().setAll(res.getItems());
            long from = res.getTotalItems() == 0 ? 0 : (long) res.getPage() * res.getSize() + 1;
            long to = (long) res.getPage() * res.getSize() + res.getItems().size();
            lblPage.setText("Hiển thị " + from + "–" + to + " trong " + res.getTotalItems() + " sản phẩm");
            lblPageNo.setText((res.getPage() + 1) + " / " + Math.max(1, res.getTotalPages()));
            btnPrev.setDisable(res.getPage() <= 0);
            btnNext.setDisable(res.getPage() >= res.getTotalPages() - 1);
        });
    }

    private void loadCounts() {
        call(Services.SAN_PHAM::demTonKho, this::renderCounts);
    }

    private void renderCounts(DemTonKhoResponse d) {
        lblTong.setText(d.getTatCa() + " mặt hàng");
        fAll.setText("Tất cả " + d.getTatCa());
        fCon.setText("Còn hàng " + d.getConHang());
        fSap.setText("Sắp hết " + d.getSapHet());
        fHet.setText("Hết hàng " + d.getHetHang());
        fNgung.setText("Ngừng bán " + d.getNgungBan());
    }

    @FXML
    private void onPrev() {
        page = Math.max(0, page - 1);
        load();
    }

    @FXML
    private void onNext() {
        if (current != null && page < current.getTotalPages() - 1) {
            page++;
            load();
        }
    }

    // ---------- Thêm / sửa ----------
    @FXML
    private void onNew() {
        openForm(null);
    }

    private void openForm(SanPhamResponse sp) {
        boolean moi = sp == null;
        TextField ten = new TextField(moi ? "" : sp.getTenSp());
        TextField maVach = new TextField(moi ? "" : Fmt.nullToEmpty(sp.getMaVach()));
        maVach.setPromptText("Quét hoặc gõ mã vạch");
        ComboBox<DanhMucResponse> dm = new ComboBox<>();
        dm.getItems().setAll(cbDanhMuc.getItems().stream().filter(d -> d != null).toList());
        dm.setCellFactory(l -> danhMucCell());
        dm.setButtonCell(danhMucCell());
        dm.setMaxWidth(Double.MAX_VALUE);
        if (!moi) {
            dm.getItems().stream().filter(d -> d.getMaDm().equals(sp.getMaDm())).findFirst().ifPresent(dm::setValue);
        }
        TextField donVi = new TextField(moi ? "" : sp.getDonVi());
        donVi.setPromptText("Gói, Chai, Hộp…");
        TextField giaBan = new TextField();
        TextField giaNhap = new TextField();
        Fmt.moneyField(giaBan);
        Fmt.moneyField(giaNhap);
        TextField tonKho = new TextField(moi ? "0" : String.valueOf(sp.getTonKho()));
        Fmt.intField(tonKho);
        tonKho.setDisable(!moi);
        TextField tonToiThieu = new TextField(moi ? "5" : String.valueOf(sp.getTonToiThieu()));
        Fmt.intField(tonToiThieu);
        CheckBox dangBan = new CheckBox("Đang bán (bỏ chọn để ngừng kinh doanh)");
        dangBan.setSelected(moi || sp.isDangBan());
        if (!moi) {
            giaBan.setText(Fmt.moneyPlain(sp.getGiaBan()));
            giaNhap.setText(Fmt.moneyPlain(sp.getGiaNhap()));
        }
        Label lai = new Label();
        lai.getStyleClass().add("hint");
        Runnable tinhLai = () -> {
            try {
                var b = Fmt.parseMoney(giaBan.getText());
                var n = Fmt.parseMoney(giaNhap.getText());
                lai.setText(b != null && n != null && b.signum() > 0
                        ? "Lãi mỗi " + donVi.getText().toLowerCase() + ": " + Fmt.money(b.subtract(n)) + " ("
                        + b.subtract(n).multiply(java.math.BigDecimal.valueOf(100)).divide(b, 0, java.math.RoundingMode.HALF_UP) + "%)"
                        : "");
            } catch (IllegalArgumentException ignored) {
                lai.setText("");
            }
        };
        giaBan.textProperty().addListener((o, a, b) -> tinhLai.run());
        giaNhap.textProperty().addListener((o, a, b) -> tinhLai.run());
        tinhLai.run();

        VBox form = new VBox(12,
                Ui.field("Tên sản phẩm *", ten),
                Ui.row(Ui.field("Mã vạch", maVach), Ui.field("Danh mục *", dm)),
                Ui.row(Ui.field("Đơn vị tính *", donVi), Ui.field("Giá bán *", giaBan), Ui.field("Giá nhập", giaNhap)),
                lai,
                Ui.row(Ui.field(moi ? "Tồn kho ban đầu" : "Tồn kho (đổi qua nhập / bán)", tonKho),
                        Ui.field("Cảnh báo khi còn ≤", tonToiThieu)),
                dangBan);
        form.setPrefWidth(520);

        FormDialog dlg = new FormDialog(root, "mdi2p-package-variant-closed", moi ? "Thêm sản phẩm" : sp.getTenSp(),
                moi ? "Mã SP được tạo tự động" : sp.getMaHang() + " · " + sp.getTenDm(), form, moi ? "Thêm" : "Lưu thay đổi");
        dlg.onSubmit(() -> {
            SanPhamRequest req = SanPhamRequest.builder()
                    .tenSp(ten.getText())
                    .maVach(Fmt.blankToNull(maVach.getText()))
                    .maDm(dm.getValue() != null ? dm.getValue().getMaDm() : null)
                    .donVi(donVi.getText())
                    .giaBan(Fmt.parseMoney(giaBan.getText()))
                    .giaNhap(Fmt.parseMoney(giaNhap.getText()))
                    .tonKho(Fmt.parseInt(tonKho.getText(), "Tồn kho"))
                    .tonToiThieu(Fmt.parseInt(tonToiThieu.getText(), "Mức tồn tối thiểu"))
                    .dangBan(dangBan.isSelected())
                    .build();
            return () -> moi ? Services.SAN_PHAM.create(req) : Services.SAN_PHAM.update(sp.getMaSp(), req);
        }, saved -> {
            toast(moi ? "Đã thêm " + saved.getMaHang() : "Đã lưu " + saved.getTenSp());
            load();
            loadCounts();
        });
        if (!moi) {
            dlg.extraButton("Xóa", "btn-danger-outline", () -> onDelete(sp));
        }
        dlg.show();
    }

    private void onDelete(SanPhamResponse sp) {
        if (!Ui.confirmDelete(root, "Xóa sản phẩm \"" + sp.getTenSp() + "\"?")) {
            return;
        }
        call(() -> {
            Services.SAN_PHAM.delete(sp.getMaSp());
            return null;
        }, ok -> {
            toast("Đã xóa sản phẩm");
            load();
            loadCounts();
        });
    }

    // ---------- Xuất CSV ----------
    @FXML
    private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất danh sách sản phẩm");
        fc.setInitialFileName("san-pham-" + LocalDate.now() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
        File file = fc.showSaveDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        String kw = Fmt.blankToNull(txtSearch.getText());
        Integer maDm = cbDanhMuc.getValue() != null ? cbDanhMuc.getValue().getMaDm() : null;
        Object f = filterGroup.getSelectedToggle().getUserData();
        TinhTrangKho tt = f != null ? TinhTrangKho.valueOf((String) f) : null;
        call(() -> {
            List<SanPhamResponse> all = Services.SAN_PHAM.search(kw, maDm, tt, 0, 200).getItems();
            try (PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
                w.print('﻿'); // BOM để Excel đọc đúng tiếng Việt
                w.println("Mã SP,Mã vạch,Tên sản phẩm,Danh mục,Đơn vị,Giá bán,Tồn kho,Tình trạng");
                for (SanPhamResponse s : all) {
                    w.printf("%s,%s,\"%s\",\"%s\",%s,%s,%d,%s%n", s.getMaHang(), Fmt.nullToEmpty(s.getMaVach()),
                            s.getTenSp(), s.getTenDm(), s.getDonVi(), s.getGiaBan(), s.getTonKho(),
                            s.getTinhTrang().getLabel());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Không ghi được file: " + e.getMessage());
            }
            return all.size();
        }, n -> toast("Đã xuất " + n + " sản phẩm ra " + file.getName()));
    }
}
