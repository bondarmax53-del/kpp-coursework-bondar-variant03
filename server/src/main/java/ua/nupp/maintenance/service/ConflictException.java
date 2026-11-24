package ua.nupp.maintenance.service;

/**
 * Операція суперечить поточному стану даних → HTTP 409.
 * Наприклад: недопустимий перехід статусу, дублікат інвентарного номера,
 * видалення обладнання, на яке є заявки.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
