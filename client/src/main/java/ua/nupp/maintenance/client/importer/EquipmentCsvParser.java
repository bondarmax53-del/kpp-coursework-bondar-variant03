package ua.nupp.maintenance.client.importer;

import ua.nupp.maintenance.client.model.Equipment;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Розбір CSV реєстру обладнання. Формат (UTF-8, роздільник «;», перший рядок — заголовок):
 *
 *   inventory_number;name;model;location;criticality;commissioned_date
 *   INV-0101;Осцилограф;Rigol DS1202;ауд. 312;4;2024-09-01
 *
 * Обов'язкові: inventory_number, name, criticality (ціле 1..5).
 * model, location, commissioned_date (РРРР-ММ-ДД) — можуть бути порожніми.
 * Клас не залежить від JavaFX, тому його легко покрити тестами.
 */
public final class EquipmentCsvParser {
    public static final String DELIMITER = ";";
    public static final List<String> HEADER =
            List.of("inventory_number", "name", "model", "location", "criticality", "commissioned_date");

    private EquipmentCsvParser() {
    }

    /** Перевіряє заголовок (з урахуванням можливого BOM на початку файлу). */
    public static void checkHeader(String line) throws CsvFormatException {
        if (line == null) {
            throw new CsvFormatException("Файл порожній");
        }
        String clean = stripBom(line).trim().toLowerCase();
        List<String> cols = List.of(clean.split(DELIMITER, -1)).stream().map(String::trim).toList();
        if (!cols.equals(HEADER)) {
            throw new CsvFormatException("Очікуваний заголовок: " + String.join(DELIMITER, HEADER));
        }
    }

    public static Equipment parseLine(String line) throws CsvFormatException {
        String[] p = line.split(DELIMITER, -1);
        if (p.length != HEADER.size()) {
            throw new CsvFormatException("очікується " + HEADER.size() + " полів, отримано " + p.length);
        }
        String inv = p[0].trim();
        String name = p[1].trim();
        if (inv.isEmpty()) {
            throw new CsvFormatException("порожній інвентарний номер");
        }
        if (name.isEmpty()) {
            throw new CsvFormatException("порожня назва");
        }
        int criticality;
        try {
            criticality = Integer.parseInt(p[4].trim());
        } catch (NumberFormatException e) {
            throw new CsvFormatException("критичність має бути цілим числом: «" + p[4].trim() + "»");
        }
        if (criticality < 1 || criticality > 5) {
            throw new CsvFormatException("критичність поза межами 1..5: " + criticality);
        }
        String date = p[5].trim();
        if (!date.isEmpty()) {
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new CsvFormatException("дата має формат РРРР-ММ-ДД: «" + date + "»");
            }
        }
        return new Equipment(inv, name, emptyToNull(p[2]), emptyToNull(p[3]), criticality,
                date.isEmpty() ? null : date);
    }

    static String stripBom(String s) {
        return !s.isEmpty() && s.charAt(0) == '﻿' ? s.substring(1) : s;
    }

    private static String emptyToNull(String s) {
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
