package ua.nupp.maintenance.client.ui;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Допоміжні методи інтерфейсу. Головне — async(): HTTP-запити виконуються
 * у фоновому потоці, щоб вікно не «зависало», а результат повертається в потік JavaFX.
 */
final class Ui {
    private static final Logger LOG = Logger.getLogger(Ui.class.getName());

    private Ui() {
    }

    static <T> void async(Supplier<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            LOG.log(Level.WARNING, "Помилка операції", ex);
            error(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        });
        Thread t = new Thread(task, "api-call");
        t.setDaemon(true);
        t.start();
    }

    static void async(Runnable work, Runnable onSuccess) {
        async(() -> {
            work.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run());
    }

    static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText("Помилка");
        a.setTitle("Помилка");
        a.showAndWait();
    }

    static void info(String header, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle("Інформація");
        a.setHeaderText(header);
        a.showAndWait();
    }

    static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Підтвердження");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Текст поля без пробілів по краях або null, якщо поле порожнє. */
    static String text(TextInputControl field) {
        String s = field.getText() == null ? "" : field.getText().trim();
        return s.isEmpty() ? null : s;
    }

    static String nvl(Object o) {
        return o == null ? "" : o.toString();
    }
}
