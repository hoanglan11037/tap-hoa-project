package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.request.NhaCungCapRequest;
import com.hoangdinh.taphoa.dto.response.NhaCungCapResponse;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NhaCungCapController extends BaseController {

    @FXML private VBox root;
    @FXML private Label lblTong;
    @FXML private TextField txtSearch;
    @FXML private CheckBox chkDangHopTac;
    @FXML private TableView<NhaCungCapResponse> table;
    @FXML private TableColumn<NhaCungCapResponse, NhaCungCapResponse> colTen;
    @FXML private TableColumn<NhaCungCapResponse, NhaCungCapResponse> colLienHe;
    @FXML private TableColumn<NhaCungCapResponse, NhaCungCapResponse> colDiaChi;
    @FXML private TableColumn<NhaCungCapResponse, NhaCungCapResponse> colNhap;
    @FXML private TableColumn<NhaCungCapResponse, NhaCungCapResponse> colTt;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Ui.nodeColumn(colTen, n -> Ui.twoLines(n.getTenNcc(), n.getSoPhieuNhap() + " phiếu nhập"));
        Ui.nodeColumn(colLienHe, n -> Ui.twoLines(Fmt.nullToEmpty(n.getNguoiLienHe()), n.getSdt()));
        Ui.textColumn(colDiaChi, n -> Fmt.nullToEmpty(n.getDiaChi()));
        Ui.textColumn(colNhap, n -> n.getLanNhapCuoi() != null ? Fmt.dateTime(n.getLanNhapCuoi()) : "Chưa nhập");
        Ui.nodeColumn(colTt, n -> Ui.pillActive(n.isTrangThai(), "Đang hợp tác", "Ngừng hợp tác"));
        table.setRowFactory(tv -> {
            TableRow<NhaCungCapResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });
        searchDelay.setOnFinished(e -> load());
        txtSearch.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        chkDangHopTac.selectedProperty().addListener((o, a, b) -> load());
        load();
    }

    private void load() {
        String kw = Fmt.blankToNull(txtSearch.getText());
        boolean chi = chkDangHopTac.isSelected();
        call(() -> Services.NHA_CUNG_CAP.search(kw, chi), list -> {
            table.getItems().setAll(list);
            lblTong.setText(list.size() + " đối tác");
        });
    }

    @FXML
    private void onNew() {
        openForm(null);
    }

    private void openForm(NhaCungCapResponse n) {
        boolean moi = n == null;
        TextField ten = new TextField(moi ? "" : n.getTenNcc());
        TextField lienHe = new TextField(moi ? "" : Fmt.nullToEmpty(n.getNguoiLienHe()));
        TextField sdt = new TextField(moi ? "" : n.getSdt());
        TextField diaChi = new TextField(moi ? "" : Fmt.nullToEmpty(n.getDiaChi()));
        CheckBox hopTac = new CheckBox("Đang hợp tác (bỏ chọn để ngừng nhập hàng)");
        hopTac.setSelected(moi || n.isTrangThai());
        VBox form = new VBox(12, Ui.field("Tên nhà cung cấp *", ten),
                Ui.row(Ui.field("Người liên hệ", lienHe), Ui.field("Số điện thoại *", sdt)),
                Ui.field("Địa chỉ", diaChi), hopTac);
        form.setPrefWidth(460);
        FormDialog dlg = new FormDialog(root, "mdi2f-factory", moi ? "Thêm nhà cung cấp" : n.getTenNcc(),
                moi ? null : n.getSoPhieuNhap() + " phiếu nhập", form, moi ? "Thêm" : "Lưu thay đổi");
        dlg.onSubmit(() -> {
            NhaCungCapRequest req = NhaCungCapRequest.builder().tenNcc(ten.getText()).nguoiLienHe(lienHe.getText())
                    .sdt(sdt.getText().trim()).diaChi(diaChi.getText()).trangThai(hopTac.isSelected()).build();
            return () -> moi ? Services.NHA_CUNG_CAP.create(req) : Services.NHA_CUNG_CAP.update(n.getMaNcc(), req);
        }, saved -> {
            toast(moi ? "Đã thêm " + saved.getTenNcc() : "Đã lưu thay đổi");
            load();
        });
        if (!moi) {
            dlg.extraButton("Xóa", "btn-danger-outline", () -> {
                if (Ui.confirmDelete(root, "Xóa nhà cung cấp \"" + n.getTenNcc() + "\"?")) {
                    call(() -> {
                        Services.NHA_CUNG_CAP.delete(n.getMaNcc());
                        return null;
                    }, ok -> {
                        toast("Đã xóa nhà cung cấp");
                        load();
                    });
                }
            });
        }
        dlg.show();
    }
}
