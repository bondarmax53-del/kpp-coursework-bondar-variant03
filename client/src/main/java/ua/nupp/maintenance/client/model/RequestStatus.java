package ua.nupp.maintenance.client.model;

/** Ті самі статуси, що й на сервері (імена мають збігатися для JSON). */
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

    public RequestStatus next() {
        RequestStatus[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    @Override
    public String toString() {
        return title;
    }
}
