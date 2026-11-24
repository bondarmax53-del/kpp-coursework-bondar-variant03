package ua.nupp.maintenance.dao;

/** Помилка доступу до даних (обгортка над SQLException). */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
