package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.request.TaiKhoanRequest;
import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.enums.VaiTro;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Quản lý tài khoản nhân viên (chỉ chủ cửa hàng). */
public class TaiKhoanController extends BaseController {

    @FXML private VBox root;
    @FXML private Label lblTong;
    @FXML private TableView<TaiKhoanResponse> table;
    @FXML private TableColumn<TaiKhoanResponse, TaiKhoanResponse> colTen;
    @FXML private TableColumn<TaiKhoanResponse, TaiKhoanResponse> colVaiTro;
    @FXML private TableColumn<TaiKhoanResponse, TaiKhoanResponse> colNgay;
    @FXML private TableColumn<TaiKhoanResponse, TaiKhoanResponse> colTt;
    @FXML private TableColumn<TaiKhoanResponse, TaiKhoanResponse> colAct;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        Ui.nodeColumn(colTen, t -> {
            Label av = new Label(Fmt.initials(t.getHoTen()));
            av.getStyleClass().add("avatar-soft");
            Node text = Ui.twoLines(t.getHoTen(), "@" + t.getTenDangNhap()
                    + (t.getMaTk().equals(Session.taiKhoan().getMaTk()) ? " · bạn" : ""));
            HBox box = new HBox(10, av, text);
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        });
        Ui.nodeColumn(colVaiTro, t -> Ui.pill(t.getVaiTro().getLabel(), t.getVaiTro() == VaiTro.ADMIN ? "violet" : "blue"));
        Ui.textColumn(colNgay, t -> Fmt.dateTime(t.getNgayTao()));
        Ui.nodeColumn(colTt, t -> Ui.pillActive(t.isTrangThai(), "Hoạt động", "Đã khóa"));
        Ui.nodeColumn(colAct, t -> {
            Button edit = new Button(null, new FontIcon("mdi2p-pencil-outline"));
            edit.getStyleClass().add("icon-btn");
            edit.setOnAction(e -> openForm(t));
            return edit;
        });
        table.setRowFactory(tv -> {
            TableRow<TaiKhoanResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });
        load();
    }

    private void load() {
        call(Services.TAI_KHOAN::findAll, list -> {
            table.getItems().setAll(list);
            lblTong.setText(list.size() + " tài khoản");
        });
    }

    @FXML
    private void onNew() {
        openForm(null);
    }

    private void openForm(TaiKhoanResponse t) {
        boolean moi = t == null;
        TextField user = new TextField(moi ? "" : t.getTenDangNhap());
        user.setPromptText("chữ không dấu, số, . _");
        TextField ten = new TextField(moi ? "" : t.getHoTen());
        PasswordField mk = new PasswordField();
        mk.setPromptText(moi ? "Tối thiểu 6 ký tự" : "Bỏ trống để giữ nguyên");

        // chọn vai trò bằng hai nút lớn thay cho danh sách thả xuống
        ToggleGroup vaiTro = new ToggleGroup();
        HBox roles = new HBox(10);
        for (VaiTro v : VaiTro.values()) {
            ToggleButton b = new ToggleButton(v.getLabel(),
                    new FontIcon(v == VaiTro.ADMIN ? "mdi2s-shield-account-outline" : "mdi2c-cash-register"));
            b.getStyleClass().add("role-option");
            b.setUserData(v);
            b.setToggleGroup(vaiTro);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, javafx.scene.layout.Priority.ALWAYS);
            b.setSelected(v == (moi ? VaiTro.THU_NGAN : t.getVaiTro()));
            roles.getChildren().add(b);
        }
        vaiTro.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
            }
        });
        CheckBox hoatDong = new CheckBox("Cho phép đăng nhập (bỏ chọn để khóa)");
        hoatDong.setSelected(moi || t.isTrangThai());

        VBox form = new VBox(12, Ui.row(Ui.field("Tên đăng nhập *", user), Ui.field("Họ tên *", ten)),
                Ui.field("Vai trò *", roles), Ui.field(moi ? "Mật khẩu *" : "Đặt lại mật khẩu", mk), hoatDong);
        form.setPrefWidth(460);
        new FormDialog(root, "mdi2s-shield-account-outline", moi ? "Thêm nhân viên" : t.getHoTen(),
                moi ? "Tài khoản đăng nhập phần mềm" : "@" + t.getTenDangNhap(), form, moi ? "Tạo tài khoản" : "Lưu thay đổi")
                .onSubmit(() -> {
                    TaiKhoanRequest req = TaiKhoanRequest.builder()
                            .tenDangNhap(user.getText().trim())
                            .hoTen(ten.getText())
                            .vaiTro((VaiTro) vaiTro.getSelectedToggle().getUserData())
                            .matKhau(Fmt.blankToNull(mk.getText()))
                            .trangThai(hoatDong.isSelected())
                            .build();
                    return () -> moi ? Services.TAI_KHOAN.create(req) : Services.TAI_KHOAN.update(t.getMaTk(), req);
                }, saved -> {
                    toast(moi ? "Đã tạo tài khoản " + saved.getTenDangNhap() : "Đã lưu thay đổi");
                    load();
                })
                .show();
    }
}
