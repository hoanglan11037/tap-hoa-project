package com.hoangdinh.taphoa.controller;

import com.hoangdinh.taphoa.dto.request.DanhMucRequest;
import com.hoangdinh.taphoa.dto.response.DanhMucResponse;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.util.Fmt;
import com.hoangdinh.taphoa.util.FormDialog;
import com.hoangdinh.taphoa.util.Ui;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Danh mục hàng dạng thẻ; bấm "Xem hàng" để mở danh sách sản phẩm của danh mục. */
public class DanhMucController extends BaseController {

    /** Màu nhấn xoay vòng cho các thẻ. */
    private static final String[] TONES = {"tone-green", "tone-blue", "tone-amber", "tone-violet", "tone-rose", "tone-teal"};

    @FXML private VBox root;
    @FXML private Label lblTong;
    @FXML private FlowPane cards;

    @Override
    protected Node root() {
        return root;
    }

    @FXML
    private void initialize() {
        load();
    }

    private void load() {
        call(Services.DANH_MUC::findAll, this::render);
    }

    private void render(List<DanhMucResponse> list) {
        lblTong.setText(list.size() + " nhóm hàng");
        cards.getChildren().clear();
        for (int i = 0; i < list.size(); i++) {
            cards.getChildren().add(card(list.get(i), TONES[i % TONES.length]));
        }
        VBox add = new VBox(8, new FontIcon("mdi2p-plus"), new Label("Thêm danh mục"));
        add.getStyleClass().add("card-add");
        add.setAlignment(Pos.CENTER);
        add.setOnMouseClicked(e -> openForm(null));
        cards.getChildren().add(add);
    }

    private Node card(DanhMucResponse d, String tone) {
        StackPane icon = new StackPane(new FontIcon("mdi2s-shape-outline"));
        icon.getStyleClass().addAll("stat-icon", tone);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button edit = new Button(null, new FontIcon("mdi2p-pencil-outline"));
        edit.getStyleClass().add("icon-btn");
        edit.setOnAction(e -> openForm(d));
        HBox top = new HBox(icon, gap, edit);
        top.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(d.getTenDm());
        name.getStyleClass().add("card-name");
        Label desc = new Label(d.getMoTa() != null ? d.getMoTa() : "Chưa có mô tả");
        desc.getStyleClass().add("muted");
        desc.setWrapText(true);
        desc.setMinHeight(38);

        Label count = new Label(d.getSoSanPham() + " sản phẩm");
        count.getStyleClass().add("card-stat");
        Label value = new Label("Tồn " + Fmt.moneyShort(d.getGiaTriTon()));
        value.getStyleClass().add("card-stat-sub");
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        Button xem = new Button("Xem hàng");
        xem.getStyleClass().addAll("link");
        xem.setOnAction(e -> MainController.open("SanPham", d.getMaDm()));
        HBox foot = new HBox(8, new VBox(2, count, value), gap2, xem);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getStyleClass().add("card-foot");

        VBox card = new VBox(10, top, name, desc, foot);
        card.getStyleClass().add("cat-card");
        return card;
    }

    @FXML
    private void onNew() {
        openForm(null);
    }

    private void openForm(DanhMucResponse d) {
        boolean moi = d == null;
        TextField ten = new TextField(moi ? "" : d.getTenDm());
        TextArea moTa = new TextArea(moi ? "" : Fmt.nullToEmpty(d.getMoTa()));
        moTa.setPrefRowCount(3);
        moTa.setWrapText(true);
        VBox form = new VBox(12, Ui.field("Tên danh mục *", ten), Ui.field("Mô tả", moTa));
        form.setPrefWidth(400);
        FormDialog dlg = new FormDialog(root, "mdi2s-shape-outline", moi ? "Thêm danh mục" : "Sửa danh mục",
                moi ? null : d.getSoSanPham() + " sản phẩm", form, moi ? "Thêm" : "Lưu");
        dlg.onSubmit(() -> {
            DanhMucRequest req = new DanhMucRequest(ten.getText(), moTa.getText());
            return () -> moi ? Services.DANH_MUC.create(req) : Services.DANH_MUC.update(d.getMaDm(), req);
        }, saved -> {
            toast(moi ? "Đã thêm danh mục " + saved.getTenDm() : "Đã lưu danh mục");
            load();
        });
        if (!moi) {
            dlg.extraButton("Xóa", "btn-danger-outline", () -> {
                if (Ui.confirmDelete(root, "Xóa danh mục \"" + d.getTenDm() + "\"?")) {
                    call(() -> {
                        Services.DANH_MUC.delete(d.getMaDm());
                        return null;
                    }, ok -> {
                        toast("Đã xóa danh mục");
                        load();
                    });
                }
            });
        }
        dlg.show();
    }
}
