package ua.nupp.maintenance.client.importer;

import java.util.ArrayList;
import java.util.List;

/** Підсумок CSV-імпорту: скільки додано, скільки пропущено і чому. */
public class ImportResult {
    private int total;
    private int imported;
    private final List<String> errors = new ArrayList<>();

    public void addTotal() { total++; }
    public void addImported() { imported++; }
    public void addError(int lineNo, String message) { errors.add("Рядок " + lineNo + ": " + message); }

    public int getTotal() { return total; }
    public int getImported() { return imported; }
    public int getSkipped() { return errors.size(); }
    public List<String> getErrors() { return errors; }

    public String summary() {
        return "Оброблено рядків: " + total + ", імпортовано: " + imported + ", пропущено: " + getSkipped();
    }
}
