package com.hoangdinh.taphoa.util;

import com.hoangdinh.taphoa.enums.TinhTrangKho;
import com.hoangdinh.taphoa.enums.TrangThaiHoaDon;
import java.util.Optional;
import java.util.function.Function;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/** Các thành phần giao diện dùng lại: nhãn trạng thái, ô bảng, ô form, hộp thoại, thông báo nhanh. */
public final class Ui {

    private Ui() {
    }

    // ---------- Nhãn trạng thái (dạng "pill" có chấm màu) ----------
    public static Label pill(String text, String kind) {
        Label l = new Label(text);
        Label dot = new Label();
        dot.getStyleClass().add("pill-dot");
        l.setGraphic(dot);
        l.getStyleClass().addAll("pill", "pill-" + kind);
        return l;
    }

    public static Label pill(TinhTrangKho t) {
        return pill(t.getLabel(), switch (t) {
            case CON_HANG -> "green";
            case SAP_HET -> "amber";
            case HET_HANG -> "red";
            case NGUNG_BAN -> "gray";
        });
    }

    public static Label pill(TrangThaiHoaDon t) {
        return pill(t.getLabel(), t == TrangThaiHoaDon.HOAN_THANH ? "green" : "red");
    }

    public static Label pillActive(boolean active, String on, String off) {
        return pill(active ? on : off, active ? "green" : "gray");
    }

    // ---------- Ô bảng ----------
    /** Cột hiển thị chữ lấy từ đối tượng của dòng. */
    public static <S> void textColumn(TableColumn<S, S> col, Function<S, String> text) {
        col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
                setGraphic(null);
            }
        });
    }

    /** Cột số / tiền: căn phải. */
    public static <S> void numberColumn(TableColumn<S, S> col, Function<S, String> text) {
        textColumn(col, text);
        col.getStyleClass().add("col-number");
    }

    /** Cột hiển thị một Node (pill, 2 dòng chữ…) tạo từ đối tượng của dòng. */
    public static <S> void nodeColumn(TableColumn<S, S> col, Function<S, Node> node) {
        col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : node.apply(item));
            }
        });
    }

    /** Chữ đậm + dòng phụ nhỏ (vd: tên sản phẩm + mã vạch). */
    public static Node twoLines(String title, String sub) {
        Label t = new Label(title);
        t.getStyleClass().add("cell-title");
        Label s = new Label(sub);
        s.getStyleClass().add("cell-sub");
        VBox box = new VBox(1, t, s);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static Label code(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("code");
        return l;
    }

    public static FontIcon icon(String literal) {
        return new FontIcon(literal);
    }

    // ---------- Ô form ----------
    /** Nhãn + ô nhập xếp dọc. */
    public static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(5, l, input);
        box.getStyleClass().add("field");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Các ô cùng một hàng, chia đều chiều ngang. */
    public static HBox row(Node... fields) {
        HBox row = new HBox(12, fields);
        for (Node f : fields) {
            HBox.setHgrow(f, Priority.ALWAYS);
        }
        return row;
    }

    // ---------- Hộp thoại ----------
    public static boolean confirm(Node owner, String question, String detail, String okText) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(owner.getScene().getWindow());
        a.setTitle("Xác nhận");
        a.setHeaderText(question);
        a.setContentText(detail);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        ButtonType huy = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(huy, ok);
        a.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        a.getDialogPane().lookupButton(ok).getStyleClass().add("btn-danger");
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ok;
    }

    public static boolean confirmDelete(Node owner, String question) {
        return confirm(owner, question, "Thao tác này không hoàn tác được.", "Xóa");
    }

    /** Hỏi một dòng chữ (vd: lý do hủy hóa đơn); null nếu người dùng bấm hủy. */
    public static String prompt(Node owner, String title, String question, String prompt) {
        TextInputDialog d = new TextInputDialog();
        d.initOwner(owner.getScene().getWindow());
        d.setTitle(title);
        d.setHeaderText(question);
        d.getEditor().setPromptText(prompt);
        d.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        return d.showAndWait().orElse(null);
    }

    public static void error(Node owner, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow());
            a.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        a.setTitle("Có lỗi");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    /** Thông báo nhanh ở góc trên bên phải, hiện dần rồi tự ẩn. Cần một StackPane làm lớp phủ. */
    public static void toast(StackPane host, String message) {
        Label l = new Label(message, new FontIcon("mdi2c-check-circle-outline"));
        l.getStyleClass().add("toast");
        StackPane.setAlignment(l, Pos.TOP_RIGHT);
        StackPane.setMargin(l, new Insets(16, 20, 0, 0));
        l.setMouseTransparent(true);
        l.setOpacity(0);
        host.getChildren().add(l);
        FadeTransition in = new FadeTransition(Duration.millis(180), l);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(300), l);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(2.4)), out);
        seq.setOnFinished(e -> host.getChildren().remove(l));
        seq.play();
    }

    public static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
