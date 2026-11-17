package com.hoangdinh.taphoa.dev;

import com.hoangdinh.taphoa.App;
import com.hoangdinh.taphoa.service.Services;
import com.hoangdinh.taphoa.controller.MainController;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/**
 * Công cụ dev: render màn hình ra PNG để kiểm tra giao diện mà không cần mở cửa sổ (cần CSDL taphoa_db).
 * Dùng: Snapshot &lt;thư mục ảnh&gt; &lt;tenDangNhap:matKhau&gt; [Login] [TongQuan] [BanHang] ...
 */
public class Snapshot {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String[] cred = args[1].split(":", 2);
        Deque<String> views = new ArrayDeque<>();
        for (int i = 2; i < args.length; i++) {
            views.add(args[i]);
        }
        Services.AUTH.login(cred[0], cred[1]);
        Platform.startup(() -> {
            App.setupTheme();
            next(dir, views, null);
        });
    }

    private static void next(File dir, Deque<String> views, Scene mainScene) {
        if (views.isEmpty()) {
            Platform.exit();
            return;
        }
        String view = views.poll();
        Scene scene;
        if ("Login".equals(view)) {
            scene = App.scene(App.load("Login.fxml"), 960, 600);
        } else {
            scene = mainScene != null ? mainScene : App.scene(App.load("Main.fxml"), 1320, 820);
            MainController.open(view, null);
        }
        Parent root = scene.getRoot();
        root.applyCss();
        root.layout();
        Scene main = "Login".equals(view) ? mainScene : scene;
        PauseTransition wait = new PauseTransition(Duration.seconds(2.5)); // chờ API trả dữ liệu
        wait.setOnFinished(e -> {
            try {
                save(scene.snapshot(null), new File(dir, "ui_" + view + ".png"));
                System.out.println("saved ui_" + view + ".png");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            next(dir, views, main);
        });
        wait.play();
    }

    private static void save(WritableImage img, File out) throws Exception {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        ImageIO.write(bi, "png", out);
    }
}
