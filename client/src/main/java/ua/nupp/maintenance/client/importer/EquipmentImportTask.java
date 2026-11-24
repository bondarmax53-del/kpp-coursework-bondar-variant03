package ua.nupp.maintenance.client.importer;

import javafx.concurrent.Task;
import ua.nupp.maintenance.client.api.ApiClient;
import ua.nupp.maintenance.client.api.ApiException;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Фоновий імпорт реєстру обладнання з CSV. Виконується в окремому потоці,
 * тому інтерфейс не блокується; прогрес передається через updateProgress()
 * у ProgressBar, а підсумок — як результат задачі (ImportResult).
 * Помилковий рядок не зупиняє імпорт: його пропускаємо і запам'ятовуємо причину.
 */
public class EquipmentImportTask extends Task<ImportResult> {
    private static final Logger LOG = Logger.getLogger(EquipmentImportTask.class.getName());

    private final Path file;
    private final ApiClient api;

    public EquipmentImportTask(Path file, ApiClient api) {
        this.file = file;
        this.api = api;
    }

    @Override
    protected ImportResult call() throws Exception {
        LOG.info("Початок імпорту CSV: " + file.getFileName());
        List<String> lines;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            lines = reader.lines().toList();
        }
        if (lines.isEmpty()) {
            throw new CsvFormatException("Файл порожній");
        }
        EquipmentCsvParser.checkHeader(lines.get(0));

        ImportResult result = new ImportResult();
        int dataLines = lines.size() - 1;
        for (int i = 1; i < lines.size(); i++) {
            if (isCancelled()) {
                LOG.info("Імпорт скасовано на рядку " + (i + 1));
                updateMessage("Скасовано. " + result.summary());
                return result;
            }
            String line = lines.get(i);
            if (line.isBlank()) {
                updateProgress(i, dataLines);
                continue;
            }
            int lineNo = i + 1;
            result.addTotal();
            try {
                api.createEquipment(EquipmentCsvParser.parseLine(line));
                result.addImported();
            } catch (CsvFormatException e) {
                result.addError(lineNo, e.getMessage());
                LOG.warning("CSV рядок " + lineNo + " пропущено: " + e.getMessage());
            } catch (ApiException e) {
                if (e.getStatus() == 0) {
                    throw e;   // сервер недоступний — далі немає сенсу
                }
                result.addError(lineNo, e.getMessage());   // напр. 409 дублікат інвентарного номера
                LOG.warning("CSV рядок " + lineNo + " відхилено сервером: " + e.getMessage());
            }
            updateProgress(i, dataLines);
            updateMessage("Рядок " + i + " з " + dataLines);
        }
        updateMessage(result.summary());
        LOG.info("Імпорт завершено. " + result.summary());
        return result;
    }
}
