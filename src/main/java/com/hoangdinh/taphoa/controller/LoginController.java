package com.hoangdinh.taphoa.controller;

import atlantafx.base.theme.Styles;
import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Async;
import com.hoangdinh.taphoa.util.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordShown;
    @FXML private ToggleButton btnShowPassword;
    @FXML private Label errUsername;
    @FXML private Label errPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    @FXML
    private void initialize() {
        txtUsername.textProperty().addListener((o, a, b) -> clearError(txtUsername, errUsername));
        txtPassword.textProperty().addListener((o, a, b) -> clearError(txtPassword, errPassword));
        // ô hiện mật khẩu dùng chung nội dung với ô mật khẩu
        txtPasswordShown.textProperty().bindBidirectional(txtPassword.textProperty());
        btnShowPassword.selectedProperty().addListener((o, a, show) -> {
            Ui.show(txtPasswordShown, show);
            Ui.show(txtPassword, !show);
            ((FontIcon) btnShowPassword.getGraphic()).setIconLiteral(show ? "mdi2e-eye-off-outline" : "mdi2e-eye-outline");
        });
    }

    @FXML
    private void onLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        showMessage(null);
        boolean ok = true;
        if (username.isEmpty()) {
            showError(txtUsername, errUsername, "Nhập tên đăng nhập");
            ok = false;
        }
        if (password.isEmpty()) {
            showError(txtPassword, errPassword, "Nhập mật khẩu");
            ok = false;
        }
        if (!ok) {
            return;
        }

        setBusy(true);
        Async.run(() -> Services.AUTH.login(username, password),
                res -> {
                    setBusy(false);
                    App.showMain();
                },
                err -> {
                    setBusy(false);
                    showMessage(err.getMessage());
                    txtPassword.clear();
                    txtPassword.requestFocus();
                });
    }

    private void setBusy(boolean busy) {
        btnLogin.setDisable(busy);
        btnLogin.setText(busy ? "Đang kiểm tra…" : "Đăng nhập");
    }

    private void showMessage(String message) {
        lblError.setText(message);
        Ui.show(lblError, message != null);
    }

    private static void showError(TextInputControl input, Label label, String message) {
        input.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        label.setText(message);
        Ui.show(label, true);
    }

    private static void clearError(TextInputControl input, Label label) {
        input.pseudoClassStateChanged(Styles.STATE_DANGER, false);
        Ui.show(label, false);
    }
}
