package ua.nupp.maintenance.client.importer;

import org.junit.jupiter.api.Test;
import ua.nupp.maintenance.client.model.Equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentCsvParserTest {

    @Test
    void parsesCompleteLine() throws Exception {
        Equipment e = EquipmentCsvParser.parseLine("INV-1; Осцилограф ;Rigol;ауд. 312;4;2024-09-01");
        assertEquals("INV-1", e.getInventoryNumber());
        assertEquals("Осцилограф", e.getName());
        assertEquals(4, e.getCriticality());
        assertEquals("2024-09-01", e.getCommissionedDate());
    }

    @Test
    void optionalFieldsMayBeEmpty() throws Exception {
        Equipment e = EquipmentCsvParser.parseLine("INV-2;Паяльник;;;1;");
        assertNull(e.getModel());
        assertNull(e.getLocation());
        assertNull(e.getCommissionedDate());
    }

    @Test
    void rejectsWrongFieldCount() {
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine("INV-3;Назва;4"));
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine(";Назва;;;3;"));
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine("INV-4;;;;3;"));
    }

    @Test
    void rejectsCriticalityOutOfRangeOrNotNumber() {
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine("INV-5;Назва;;;7;"));
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine("INV-5;Назва;;;висока;"));
    }

    @Test
    void rejectsWrongDateFormat() {
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.parseLine("INV-6;Назва;;;3;01.03.2023"));
    }

    @Test
    void headerWithBomIsAccepted() throws Exception {
        EquipmentCsvParser.checkHeader("﻿inventory_number;name;model;location;criticality;commissioned_date");
        assertThrows(CsvFormatException.class, () -> EquipmentCsvParser.checkHeader("id;name"));
    }
}
