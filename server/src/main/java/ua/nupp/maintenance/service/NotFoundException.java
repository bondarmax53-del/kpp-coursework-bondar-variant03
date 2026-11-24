package ua.nupp.maintenance.service;

/** Об'єкт не знайдено → HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
