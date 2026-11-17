package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.util.Async;
import com.hoangdinh.taphoa.security.Session;
import com.hoangdinh.taphoa.util.Ui;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Lớp cha của các màn hình nghiệp vụ: gọi service (truy vấn CSDL) ở luồng nền để giao diện không bị đứng,
 * xử lý lỗi thống nhất (chưa đăng nhập -> quay về đăng nhập, lỗi khác -> hiện thông báo).
 */
public abstract class BaseController {

    /** Node gốc của màn hình, dùng để mở hộp thoại và hiện thông báo nhanh. */
    protected abstract Node root();

    protected <T> void call(Callable<T> work, Consumer<T> onSuccess) {
        call(work, onSuccess, this::showError);
    }

    /** onError nhận message tiếng Việt (vd: hiện ngay trong form). */
    protected <T> void call(Callable<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Async.run(work, onSuccess, err -> {
            if (err instanceof BusinessException be && be.getErrorCode() == ErrorCode.UNAUTHORIZED) {
                Session.logout();
                App.showLogin();
                return;
            }
            String msg = err.getMessage() != null ? err.getMessage() : err.getClass().getSimpleName();
            onError.accept(msg);
        });
    }

    protected void showError(String message) {
        Ui.error(root(), message);
    }

    protected void toast(String message) {
        Node n = root();
        while (n != null && !(n instanceof StackPane sp && "contentArea".equals(sp.getId()))) {
            n = n.getParent();
        }
        if (n instanceof StackPane host) {
            Ui.toast(host, message);
        }
    }

    /** Hiện / ẩn dòng lỗi trong form. */
    protected static void formError(Label label, String message) {
        label.setText(message);
        Ui.show(label, message != null);
    }
}
