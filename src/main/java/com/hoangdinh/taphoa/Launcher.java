package com.hoangdinh.taphoa;

import javafx.application.Application;

/** Điểm vào chương trình; tách khỏi App để chạy được bằng java -jar khi JavaFX nằm trên classpath. */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
