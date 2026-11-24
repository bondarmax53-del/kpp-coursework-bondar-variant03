package ua.nupp.maintenance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.nupp.maintenance.fake.InMemoryStore;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;
import ua.nupp.maintenance.model.ServiceRecord;
import ua.nupp.maintenance.model.Specialist;
import ua.nupp.maintenance.model.StatusChange;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaintenanceServiceTest {
    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneId.of("Europe/Kyiv"));

    private InMemoryStore store;
    private MaintenanceService service;
    private Equipment oscilloscope;
    private Specialist engineer;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        service = new MaintenanceService(store.requestDao, store.equipmentDao,
                store.specialistDao, store.recordDao, FIXED);
        oscilloscope = store.equipmentDao.insert(
                new Equipment(null, "INV-1", "Осцилограф", "DS1054Z", "312", 4, null));
        engineer = store.specialistDao.insert(
                new Specialist(null, "Коваленко І.П.", "Електроніка", null, null, true));
    }

    private MaintenanceRequest newRequest(int severity, int downtime) {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(oscilloscope.getId());
        r.setDescription("Немає зображення");
        r.setSeverity(severity);
        r.setDowntimeHours(downtime);
        return service.create(r);
    }

    private static StatusChange to(RequestStatus s) {
        StatusChange c = new StatusChange();
        c.setStatus(s);
        return c;
    }

    private StatusChange diagnose() {
        StatusChange c = to(RequestStatus.DIAGNOSED);
        c.setSpecialistId(engineer.getId());
        c.setDiagnosis("Пошкоджено роз'єм");
        return c;
    }

    private ServiceRecord work() {
        ServiceRecord rec = new ServiceRecord();
        rec.setWorkDescription("Заміна роз'єму");
        rec.setHoursSpent(new BigDecimal("1.5"));
        rec.setCost(new BigDecimal("250"));
        return rec;
    }

    @Test
    void createComputesPriorityAndStartsAsNew() {
        MaintenanceRequest r = newRequest(3, 24);
        assertEquals(RequestStatus.NEW, r.getStatus());
        assertEquals(55.83, r.getPriority(), 1e-9);
        assertEquals(LocalDateTime.of(2026, 11, 24, 10, 0), r.getCreatedAt());
        assertNull(r.getSpecialistId());
    }

    @Test
    void createIgnoresClientSuppliedStatusAndPriority() {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(oscilloscope.getId());
        r.setDescription("Тест");
        r.setSeverity(1);
        r.setDowntimeHours(0);
        r.setStatus(RequestStatus.DONE);
        r.setPriority(99);
        MaintenanceRequest saved = service.create(r);
        assertEquals(RequestStatus.NEW, saved.getStatus());
        assertEquals(30.0, saved.getPriority(), 1e-9);   // лише F1 = 75 · 0,4
    }

    @Test
    void createFailsForUnknownEquipment() {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(999L);
        r.setDescription("x");
        r.setSeverity(3);
        assertThrows(NotFoundException.class, () -> service.create(r));
    }

    @Test
    void createValidatesDescriptionAndSeverity() {
        MaintenanceRequest noText = new MaintenanceRequest();
        noText.setEquipmentId(oscilloscope.getId());
        noText.setDescription("   ");
        noText.setSeverity(3);
        assertThrows(ValidationException.class, () -> service.create(noText));
        assertThrows(ValidationException.class, () -> newRequest(7, 0));
    }

    @Test
    void fullLifecycle() {
        long id = newRequest(3, 24).getId();
        service.changeStatus(id, diagnose());
        service.changeStatus(id, to(RequestStatus.IN_SERVICE));
        service.addRecord(id, work());
        MaintenanceRequest done = service.changeStatus(id, to(RequestStatus.DONE));

        assertEquals(RequestStatus.DONE, done.getStatus());
        assertEquals(engineer.getId(), done.getSpecialistId());
        assertEquals(1, service.records(id).size());
    }

    @Test
    void cannotSkipOrGoBack() {
        long id = newRequest(3, 24).getId();
        assertThrows(ConflictException.class, () -> service.changeStatus(id, to(RequestStatus.IN_SERVICE)));
        service.changeStatus(id, diagnose());
        assertThrows(ConflictException.class, () -> service.changeStatus(id, to(RequestStatus.NEW)));
    }

    @Test
    void diagnosisRequiresSpecialistAndText() {
        long id = newRequest(3, 24).getId();
        assertThrows(ValidationException.class, () -> service.changeStatus(id, to(RequestStatus.DIAGNOSED)));

        StatusChange noText = diagnose();
        noText.setDiagnosis("");
        assertThrows(ValidationException.class, () -> service.changeStatus(id, noText));
        assertEquals(RequestStatus.NEW, service.get(id).getStatus());   // стан не змінився
    }

    @Test
    void inactiveSpecialistCannotBeAssigned() {
        engineer.setActive(false);
        long id = newRequest(3, 24).getId();
        assertThrows(ConflictException.class, () -> service.changeStatus(id, diagnose()));
    }

    @Test
    void cannotFinishWithoutServiceRecord() {
        long id = newRequest(3, 24).getId();
        service.changeStatus(id, diagnose());
        service.changeStatus(id, to(RequestStatus.IN_SERVICE));
        assertThrows(ConflictException.class, () -> service.changeStatus(id, to(RequestStatus.DONE)));
    }

    @Test
    void recordsOnlyInServiceStatus() {
        long id = newRequest(3, 24).getId();
        assertThrows(ConflictException.class, () -> service.addRecord(id, work()));
    }

    @Test
    void recordDefaultsToAssignedSpecialistAndValidatesHours() {
        long id = newRequest(3, 24).getId();
        service.changeStatus(id, diagnose());
        service.changeStatus(id, to(RequestStatus.IN_SERVICE));

        ServiceRecord saved = service.addRecord(id, work());
        assertEquals(engineer.getId(), saved.getSpecialistId());

        ServiceRecord zeroHours = work();
        zeroHours.setHoursSpent(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () -> service.addRecord(id, zeroHours));
    }

    @Test
    void listIsSortedByPriorityDescending() {
        newRequest(1, 0);    // 30,00
        newRequest(5, 72);   // 90,00
        newRequest(3, 24);   // 55,83
        List<Double> priorities = service.findAll(null).stream().map(MaintenanceRequest::getPriority).toList();
        assertEquals(List.of(90.0, 55.83, 30.0), priorities);
    }

    @Test
    void filterByStatus() {
        long id = newRequest(3, 24).getId();
        newRequest(2, 10);
        service.changeStatus(id, diagnose());
        assertEquals(1, service.findAll(RequestStatus.NEW).size());
        assertEquals(id, service.findAll(RequestStatus.DIAGNOSED).get(0).getId());
    }

    @Test
    void updateRecalculatesPriorityAndKeepsStatus() {
        MaintenanceRequest r = newRequest(1, 0);          // 30,00
        long id = r.getId();
        service.changeStatus(id, diagnose());
        MaintenanceRequest changes = new MaintenanceRequest();
        changes.setEquipmentId(oscilloscope.getId());
        changes.setDescription("Уточнений опис");
        changes.setSeverity(5);
        changes.setDowntimeHours(72);
        MaintenanceRequest updated = service.update(id, changes);
        assertEquals(90.0, updated.getPriority(), 1e-9);
        assertEquals(RequestStatus.DIAGNOSED, updated.getStatus());
    }

    @Test
    void onlyNewRequestCanBeDeleted() {
        long id = newRequest(3, 24).getId();
        service.changeStatus(id, diagnose());
        assertThrows(ConflictException.class, () -> service.delete(id));

        long fresh = newRequest(2, 4).getId();
        service.delete(fresh);
        assertThrows(NotFoundException.class, () -> service.get(fresh));
    }

    @Test
    void recordCanBeEditedAndDeletedOnlyWithinItsRequest() {
        long id = newRequest(3, 24).getId();
        service.changeStatus(id, diagnose());
        service.changeStatus(id, to(RequestStatus.IN_SERVICE));
        ServiceRecord saved = service.addRecord(id, work());

        ServiceRecord changed = work();
        changed.setHoursSpent(new BigDecimal("3"));
        assertEquals(new BigDecimal("3"), service.updateRecord(id, saved.getId(), changed).getHoursSpent());
        assertThrows(NotFoundException.class, () -> service.deleteRecord(id, 9999));

        service.deleteRecord(id, saved.getId());
        assertEquals(0, service.records(id).size());
    }
}
