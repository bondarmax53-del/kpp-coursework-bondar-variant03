package ua.nupp.maintenance.model;

/**
 * Життєвий цикл заявки на обслуговування:
 * НОВА → ДІАГНОСТОВАНО → ОБСЛУГОВУВАННЯ → ВИКОНАНО.
 * Дозволено лише перехід на один крок уперед.
 */
public enum RequestStatus {
    NEW("Нова"),
    DIAGNOSED("Діагностовано"),
    IN_SERVICE("Обслуговування"),
    DONE("Виконано");

    private final String title;

    RequestStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Наступний допустимий статус або null, якщо заявка вже виконана. */
    public RequestStatus next() {
        RequestStatus[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    /** Чи можна перевести заявку з цього статусу в target. */
    public boolean canMoveTo(RequestStatus target) {
        return target != null && target == next();
    }
}
