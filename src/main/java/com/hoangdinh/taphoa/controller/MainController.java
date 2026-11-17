package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.dto.response.TaiKhoanResponse;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Khung chính: thanh trên cùng (thương hiệu, đồng hồ, tài khoản) + dải tab chức năng + vùng nội dung. */
public class MainController {

    /** Màn hình nhận tham số khi được mở từ màn khác (vd: mở Nhập hàng cho một sản phẩm sắp hết). */
    public interface ParamAware {
        void receive(Object param);
    }

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("EEEE, dd/MM · HH:mm", new Locale("vi", "VN"));

    private static MainController instance;

    @FXML private BorderPane root;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navTongQuan;
    @FXML private ToggleButton navDanhMuc;
    @FXML private ToggleButton navNhaCungCap;
    @FXML private ToggleButton navThongKe;
    @FXML private ToggleButton navTaiKhoan;
    @FXML private StackPane contentArea;
    @FXML private Label lblClock;
    @FXML private Label lblAvatar;
    @FXML private MenuButton userMenu;
    @FXML private Label lblUserRole;

    private Object pendingParam;

    @FXML
    private void initialize() {
        instance = this;
        TaiKhoanResponse tk = Session.taiKhoan();
        userMenu.setText(tk.getHoTen());
        lblUserRole.setText(tk.getVaiTro().getLabel());
        lblAvatar.setText(Fmt.initials(tk.getHoTen()));

        // chức năng chỉ dành cho chủ cửa hàng
        for (ToggleButton adminOnly : new ToggleButton[] {navDanhMuc, navNhaCungCap, navThongKe, navTaiKhoan}) {
            Ui.show(adminOnly, Session.isAdmin());
        }

        navGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true); // không cho bỏ chọn tab đang mở
            } else {
                show((String) newT.getUserData());
            }
        });

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> tick()), new KeyFrame(Duration.seconds(20)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // F2: mở nhanh màn bán hàng từ bất kỳ đâu
        root.sceneProperty().addListener((o, a, scene) -> {
            if (scene != null) {
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), () -> open("BanHang", null));
            }
        });
        navTongQuan.setSelected(true);
    }

    private void tick() {
        String s = LocalDateTime.now().format(CLOCK);
        lblClock.setText(Character.toUpperCase(s.charAt(0)) + s.substring(1));
    }

    /** Chuyển sang màn hình khác, có thể kèm tham số. */
    public static void open(String view, Object param) {
        if (instance == null) {
            return;
        }
        instance.pendingParam = param;
        for (Toggle t : instance.navGroup.getToggles()) {
            if (view.equals(t.getUserData())) {
                if (t.isSelected()) {
                    instance.show(view);
                } else {
                    t.setSelected(true);
                }
                return;
            }
        }
    }

    private void show(String view) {
        URL fxml = App.class.getResource("/com/hoangdinh/taphoa/view/" + view + ".fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Node page;
        try {
            page = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Không tải được màn hình " + view, e);
        }
        if (pendingParam != null && loader.getController() instanceof ParamAware pa) {
            pa.receive(pendingParam);
        }
        pendingParam = null;
        contentArea.getChildren().setAll(page);
    }

    @FXML
    private void onBanHang() {
        open("BanHang", null);
    }

    @FXML
    private void onDoiMatKhau() {
        PasswordField cu = new PasswordField();
        PasswordField moi = new PasswordField();
        PasswordField nhapLai = new PasswordField();
        moi.setPromptText("Tối thiểu 6 ký tự");
        VBox form = new VBox(12,
                Ui.field("Mật khẩu hiện tại", cu),
                Ui.field("Mật khẩu mới", moi),
                Ui.field("Nhập lại mật khẩu mới", nhapLai));
        form.setPrefWidth(360);
        new FormDialog(contentArea, "mdi2k-key-variant", "Đổi mật khẩu", Session.current().getTenDangNhap(), form, "Đổi mật khẩu")
                .<Void>onSubmit(() -> {
                    if (!moi.getText().equals(nhapLai.getText())) {
                        throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
                    }
                    String a = cu.getText();
                    String b = moi.getText();
                    return () -> {
                        Services.AUTH.doiMatKhau(a, b);
                        return null;
                    };
                }, ok -> Ui.toast(contentArea, "Đã đổi mật khẩu"))
                .show();
    }

    @FXML
    private void onLogout() {
        Services.AUTH.logout();
        instance = null;
        App.showLogin();
    }
}
