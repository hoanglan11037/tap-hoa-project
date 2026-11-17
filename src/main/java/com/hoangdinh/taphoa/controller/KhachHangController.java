package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.request.KhachHangRequest;
import com.hoangdinh.taphoa.dto.response.HoaDonResponse;
import com.hoangdinh.taphoa.dto.response.KhachHangResponse;
import com.hoangdinh.taphoa.dto.response.PageResponse;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Khách hàng thân thiết: danh sách + "thẻ thành viên" và lịch sử mua của khách đang chọn. */
public class KhachHangController extends BaseController {

    private static final int PAGE_SIZE = 25;

    @FXML private VBox root;
    @FXML private Label lblTong;
    @FXML private TextField txtSearch;
    @FXML private TableView<KhachHangResponse> table;
    @FXML private TableColumn<KhachHangResponse, KhachHangResponse> colMa;
    @FXML private TableColumn<KhachHangResponse, KhachHangResponse> colTen;
    @FXML private TableColumn<KhachHangResponse, KhachHangResponse> colDiem;
    @FXML private TableColumn<KhachHangResponse, KhachHangResponse> colChi;
    @FXML private TableColumn<KhachHangResponse, KhachHangResponse> colLan;
    @FXML private Label lblPage;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private VBox profile;
    @FXML private VBox noSelection;
    @FXML private Label lblMaThe;
    @FXML private Label lblTen;
    @FXML private Label lblSdt;
    @FXML private Label lblDiem;
    @FXML private Label lblTu;
    @FXML private Label lblSoLan;
    @FXML private Label lblTongChi;
    @FXML private Label lblDiaChi;
    @FXML private VBox listMua;
    @FXML private Button btnXoa;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));
    private int page;
    private PageResponse<KhachHangResponse> current;
    private KhachHangResponse selected;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Ui.nodeColumn(colMa, k -> Ui.code(k.getMaThe()));
        Ui.nodeColumn(colTen, k -> Ui.twoLines(k.getHoTen(), k.getSdt()));
        Ui.numberColumn(colDiem, k -> String.valueOf(k.getDiemTichLuy()));
        Ui.numberColumn(colChi, k -> Fmt.money(k.getTongChiTieu()));
        Ui.textColumn(colLan, k -> k.getLanMuaCuoi() != null ? Fmt.date(k.getLanMuaCuoi().toLocalDate()) : "Chưa mua");
        Ui.show(btnXoa, Session.isAdmin());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, k) -> {
            if (k != null) {
                showProfile(k);
            }
        });
        searchDelay.setOnFinished(e -> {
            page = 0;
            load();
        });
        txtSearch.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        load();
    }

    private void load() {
        String kw = Fmt.blankToNull(txtSearch.getText());
        int p = page;
        Integer keep = selected != null ? selected.getMaKh() : null;
        call(() -> Services.KHACH_HANG.search(kw, p, PAGE_SIZE), res -> {
            current = res;
            table.getItems().setAll(res.getItems());
            lblTong.setText(res.getTotalItems() + " khách");
            lblPage.setText("Trang " + (res.getPage() + 1) + " / " + Math.max(1, res.getTotalPages()));
            btnPrev.setDisable(res.getPage() <= 0);
            btnNext.setDisable(res.getPage() >= res.getTotalPages() - 1);
            res.getItems().stream().filter(k -> k.getMaKh().equals(keep)).findFirst()
                    .ifPresent(k -> table.getSelectionModel().select(k));
        });
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

    private void showProfile(KhachHangResponse k) {
        selected = k;
        Ui.show(profile, true);
        Ui.show(noSelection, false);
        lblMaThe.setText(k.getMaThe());
        lblTen.setText(k.getHoTen());
        lblSdt.setText(k.getSdt());
        lblDiem.setText(k.getDiemTichLuy() + " điểm");
        lblTu.setText(Fmt.date(k.getNgayTao().toLocalDate()));
        lblSoLan.setText(String.valueOf(k.getSoLanMua()));
        lblTongChi.setText(Fmt.moneyShort(k.getTongChiTieu()));
        lblDiaChi.setText(k.getDiaChi() != null ? k.getDiaChi() : "Chưa có địa chỉ");
        listMua.getChildren().clear();
        int maKh = k.getMaKh();
        call(() -> Services.KHACH_HANG.lichSuMua(maKh), this::renderLichSu);
    }

    private void renderLichSu(List<HoaDonResponse> list) {
        listMua.getChildren().clear();
        if (list.isEmpty()) {
            Label l = new Label("Chưa có hóa đơn nào");
            l.getStyleClass().add("muted");
            listMua.getChildren().add(l);
        }
        for (HoaDonResponse h : list) {
            Node info = Ui.twoLines(h.getSoHd(), Fmt.dateTime(h.getNgayLap()) + " · " + h.getSoMatHang() + " mặt hàng");
            HBox.setHgrow(info, Priority.ALWAYS);
            Node tien = h.getTrangThai() == TrangThaiHoaDon.DA_HUY ? Ui.pill(h.getTrangThai()) : moneyLabel(h);
            HBox row = new HBox(8, info, tien);
            row.getStyleClass().add("list-row");
            listMua.getChildren().add(row);
        }
    }

    private static Label moneyLabel(HoaDonResponse h) {
        Label l = new Label(Fmt.money(h.getPhaiTra()));
        l.getStyleClass().add("money");
        return l;
    }

    @FXML
    private void onNew() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(KhachHangResponse k) {
        boolean moi = k == null;
        TextField ten = new TextField(moi ? "" : k.getHoTen());
        TextField sdt = new TextField(moi ? "" : k.getSdt());
        TextField diaChi = new TextField(moi ? "" : Fmt.nullToEmpty(k.getDiaChi()));
        VBox form = new VBox(12, Ui.field("Họ tên *", ten), Ui.field("Số điện thoại *", sdt), Ui.field("Địa chỉ", diaChi));
        form.setPrefWidth(380);
        new FormDialog(root, "mdi2a-account-outline", moi ? "Thêm khách hàng" : "Sửa thông tin khách",
                moi ? "Mã thẻ được tạo tự động" : k.getMaThe(), form, moi ? "Thêm" : "Lưu")
                .onSubmit(() -> {
                    KhachHangRequest req = new KhachHangRequest(ten.getText(), sdt.getText().trim(), diaChi.getText());
                    return () -> moi ? Services.KHACH_HANG.create(req) : Services.KHACH_HANG.update(k.getMaKh(), req);
                }, saved -> {
                    toast(moi ? "Đã thêm khách " + saved.getMaThe() : "Đã lưu thay đổi");
                    selected = saved;
                    load();
                    showProfile(saved);
                })
                .show();
    }

    @FXML
    private void onDelete() {
        if (selected == null || !Ui.confirm(root, "Xóa khách hàng " + selected.getHoTen() + "?",
                "Các hóa đơn cũ vẫn được giữ nhưng chuyển thành khách lẻ, điểm tích lũy sẽ mất.", "Xóa")) {
            return;
        }
        int id = selected.getMaKh();
        call(() -> {
            Services.KHACH_HANG.delete(id);
            return null;
        }, ok -> {
            toast("Đã xóa khách hàng");
            selected = null;
            Ui.show(profile, false);
            Ui.show(noSelection, true);
            load();
        });
    }

    @FXML
    private void onBanHang() {
        if (selected != null) {
            MainController.open("BanHang", selected);
        }
    }
}
