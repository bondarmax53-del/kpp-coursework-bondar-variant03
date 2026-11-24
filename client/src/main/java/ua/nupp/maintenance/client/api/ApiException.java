package ua.nupp.maintenance.client.api;

/** Помилка звернення до сервера: код HTTP (0 — сервер недоступний) і зрозуміле повідомлення. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int getStatus() {
        return status;
    }

    /** 409 — конфлікт (дублікат, недопустимий перехід тощо). */
    public boolean isConflict() {
        return status == 409;
    }
}
