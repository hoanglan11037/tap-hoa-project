package com.hoangdinh.taphoa.util;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Hộp thoại form dùng chung cho các màn thêm / sửa: phần đầu có biểu tượng + tiêu đề, dòng báo lỗi,
 * nội dung form và hai nút. Bấm nút lưu thì đọc form (luồng giao diện), gọi service ở luồng nền,
 * thành công mới đóng hộp thoại; lỗi hiện ngay trên form.
 *
 * <pre>
 * FormDialog dlg = new FormDialog(root, "mdi2t-tag-outline", "Thêm danh mục", null, form, "Lưu");
 * dlg.onSubmit(() -> { var req = ...; return () -> Services.DANH_MUC.create(req); }, saved -> toast("Đã lưu"));
 * dlg.show();
 * </pre>
 */
public class FormDialog {

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final Label error = new Label();
    private final Button okButton;
    private final String okText;

    public FormDialog(Node owner, String icon, String title, String subtitle, Node form, String okText) {
        this.okText = okText;
        dialog.initOwner(owner.getScene().getWindow());
        dialog.setTitle(title);

        StackPane badge = new StackPane(new FontIcon(icon));
        badge.getStyleClass().add("dialog-icon");
        Label t = new Label(title);
        t.getStyleClass().add("dialog-title");
        VBox titles = new VBox(2, t);
        if (subtitle != null) {
            Label s = new Label(subtitle);
            s.getStyleClass().add("dialog-sub");
            titles.getChildren().add(s);
        }
        HBox head = new HBox(12, badge, titles);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("dialog-head");

        error.getStyleClass().add("alert-error");
        error.setWrapText(true);
        error.setMaxWidth(Double.MAX_VALUE);
        Ui.show(error, false);

        VBox body = new VBox(14, head, error, form);
        body.getStyleClass().add("dialog-body");
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getStylesheets().add(App.class.getResource(App.CSS).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE), ok);
        okButton = (Button) dialog.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add("btn-primary");
    }

    /**
     * build: chạy ở luồng giao diện, đọc form và trả về công việc gọi service (chạy ở luồng nền).
     * build có thể ném IllegalArgumentException (vd: số tiền sai định dạng) để hiện lỗi ngay.
     */
    public <T> FormDialog onSubmit(Supplier<Callable<T>> build, Consumer<T> onSuccess) {
        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();   // tự đóng khi lưu thành công
            Callable<T> work;
            try {
                work = build.get();
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
                return;
            }
            busy(true);
            Async.run(work, result -> {
                busy(false);
                dialog.close();
                onSuccess.accept(result);
            }, err -> {
                busy(false);
                if (err instanceof BusinessException be && be.getErrorCode() == ErrorCode.UNAUTHORIZED) {
                    dialog.close();
                    Session.logout();
                    App.showLogin();
                    return;
                }
                showError(err.getMessage() != null ? err.getMessage() : err.getClass().getSimpleName());
            });
        });
        return this;
    }

    /** Nút phụ bên trái (vd: Xóa) — chạy action rồi đóng hộp thoại. */
    public FormDialog extraButton(String text, String styleClass, Runnable action) {
        ButtonType extra = new ButtonType(text, ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().add(0, extra);
        Button b = (Button) dialog.getDialogPane().lookupButton(extra);
        b.getStyleClass().add(styleClass);
        b.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            dialog.close();
            action.run();
        });
        return this;
    }

    public void showError(String message) {
        error.setText(message);
        Ui.show(error, message != null);
    }

    public void show() {
        dialog.showAndWait();
    }

    private void busy(boolean busy) {
        okButton.setDisable(busy);
        okButton.setText(busy ? "Đang lưu…" : okText);
    }
}
