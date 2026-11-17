package com.hoangdinh.taphoa.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Chạy thao tác truy vấn CSDL ở luồng nền để giao diện không bị đứng, rồi trả kết quả về luồng JavaFX. */
public final class Async {

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread t = new Thread(task, "db-call");
        t.setDaemon(true);
        t.start();
    }
}
