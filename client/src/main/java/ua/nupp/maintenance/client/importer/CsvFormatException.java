package ua.nupp.maintenance.client.importer;

/** Рядок CSV не відповідає формату. */
public class CsvFormatException extends Exception {
    public CsvFormatException(String message) {
        super(message);
    }
}
