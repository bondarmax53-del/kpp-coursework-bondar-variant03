package ua.nupp.maintenance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.nupp.maintenance.fake.InMemoryStore;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.model.MaintenanceRequest;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentServiceTest {
    private InMemoryStore store;
    private EquipmentService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        service = new EquipmentService(store.equipmentDao, store.requestDao);
    }

    private static Equipment eq(String inv, int criticality) {
        return new Equipment(null, inv, "Генератор", null, "312", criticality, null);
    }

    @Test
    void createTrimsAndSaves() {
        Equipment saved = service.create(eq("  INV-7  ", 3));
        assertEquals("INV-7", saved.getInventoryNumber());
        assertTrue(store.equipmentDao.findById(saved.getId()).isPresent());
    }

    @Test
    void duplicateInventoryNumberIsConflict() {
        service.create(eq("INV-7", 3));
        assertThrows(ConflictException.class, () -> service.create(eq("INV-7", 2)));
    }

    @Test
    void validatesRequiredFieldsAndCriticality() {
        assertThrows(ValidationException.class, () -> service.create(eq("", 3)));
        assertThrows(ValidationException.class, () -> service.create(eq("INV-8", 0)));
    }

    @Test
    void cannotDeleteEquipmentWithRequests() {
        Equipment e = service.create(eq("INV-9", 3));
        MaintenanceService ms = new MaintenanceService(store.requestDao, store.equipmentDao,
                store.specialistDao, store.recordDao, Clock.systemDefaultZone());
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(e.getId());
        r.setDescription("Не вмикається");
        r.setSeverity(2);
        ms.create(r);
        assertThrows(ConflictException.class, () -> service.delete(e.getId()));
    }

    @Test
    void changingCriticalityRecalculatesOpenRequests() {
        Equipment e = service.create(eq("INV-10", 1));
        MaintenanceService ms = new MaintenanceService(store.requestDao, store.equipmentDao,
                store.specialistDao, store.recordDao, Clock.systemDefaultZone());
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(e.getId());
        r.setDescription("Шум вентилятора");
        r.setSeverity(1);
        long id = ms.create(r).getId();
        assertEquals(0.0, ms.get(id).getPriority(), 1e-9);

        service.update(e.getId(), eq("INV-10", 5));
        assertEquals(40.0, ms.get(id).getPriority(), 1e-9);   // F1 = 100 · 0,4
    }

    @Test
    void getUnknownIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.get(42));
    }
}
