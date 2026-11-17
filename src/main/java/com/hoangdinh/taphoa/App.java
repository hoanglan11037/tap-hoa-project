package com.hoangdinh.taphoa;

import atlantafx.base.theme.CupertinoLight;
import com.hoangdinh.taphoa.config.Database;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    public static final String CSS = "/com/hoangdinh/taphoa/css/app.css";
    private static final double MAIN_WIDTH = 1320;
    private static final double MAIN_HEIGHT = 820;
    /** Chừa chỗ cho thanh tiêu đề và viền cửa sổ của Windows. */
    private static final double TITLE_BAR = 40;
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        Database.warmUp();
        setupTheme();
        stage.setTitle("Hoàng Đinh – Quản lý tạp hóa");
        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        Database.shutdown();
    }

    public static void setupTheme() {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
    }

    public static void showLogin() {
        stage.setMaximized(false);
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setScene(scene(load("Login.fxml"), 960, 600));
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    /** Màn hình chính; nếu màn hình nhỏ hơn kích thước mặc định thì mở phóng to vừa vùng làm việc. */
    public static void showMain() {
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double width = Math.min(MAIN_WIDTH, area.getWidth());
        double height = Math.min(MAIN_HEIGHT, area.getHeight() - TITLE_BAR);
        stage.setScene(scene(load("Main.fxml"), width, height));
        stage.setResizable(true);
        stage.setMinWidth(Math.min(1180, area.getWidth()));
        stage.setMinHeight(Math.min(700, area.getHeight() - TITLE_BAR));
        if (width < MAIN_WIDTH || height < MAIN_HEIGHT) {
            stage.setMaximized(true);
        } else {
            stage.sizeToScene();
            stage.centerOnScreen();
        }
    }

    public static Parent load(String fxml) {
        try {
            return FXMLLoader.load(App.class.getResource("/com/hoangdinh/taphoa/view/" + fxml));
        } catch (IOException e) {
            throw new UncheckedIOException("Không tải được " + fxml, e);
        }
    }

    public static Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(App.class.getResource(CSS).toExternalForm());
        return scene;
    }
}
