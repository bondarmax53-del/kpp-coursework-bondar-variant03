package ua.nupp.maintenance.fake;

import ua.nupp.maintenance.dao.EquipmentDao;
import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.dao.ServiceRecordDao;
import ua.nupp.maintenance.dao.SpecialistDao;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;
import ua.nupp.maintenance.model.ServiceRecord;
import ua.nupp.maintenance.model.Specialist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Реалізації DAO у пам'яті для модульних тестів сервісів:
 * тести не потребують запущеного MySQL.
 */
public class InMemoryStore {
    private final Map<Long, Equipment> equipment = new LinkedHashMap<>();
    private final Map<Long, Specialist> specialists = new LinkedHashMap<>();
    private final Map<Long, MaintenanceRequest> requests = new LinkedHashMap<>();
    private final Map<Long, ServiceRecord> records = new LinkedHashMap<>();
    private long seq = 1;

    public final EquipmentDao equipmentDao = new EquipmentDao() {
        public List<Equipment> findAll() { return new ArrayList<>(equipment.values()); }
        public Optional<Equipment> findById(long id) { return Optional.ofNullable(equipment.get(id)); }
        public Optional<Equipment> findByInventoryNumber(String inv) {
            return equipment.values().stream().filter(e -> e.getInventoryNumber().equals(inv)).findFirst();
        }
        public Equipment insert(Equipment e) { e.setId(seq++); equipment.put(e.getId(), e); return e; }
        public boolean update(Equipment e) { return equipment.replace(e.getId(), e) != null; }
        public boolean delete(long id) { return equipment.remove(id) != null; }
    };

    public final SpecialistDao specialistDao = new SpecialistDao() {
        public List<Specialist> findAll() { return new ArrayList<>(specialists.values()); }
        public Optional<Specialist> findById(long id) { return Optional.ofNullable(specialists.get(id)); }
        public Specialist insert(Specialist s) { s.setId(seq++); specialists.put(s.getId(), s); return s; }
        public boolean update(Specialist s) { return specialists.replace(s.getId(), s) != null; }
        public boolean delete(long id) { return specialists.remove(id) != null; }
    };

    public final MaintenanceRequestDao requestDao = new MaintenanceRequestDao() {
        public List<MaintenanceRequest> findAll(RequestStatus st) {
            return requests.values().stream()
                    .filter(r -> st == null || r.getStatus() == st)
                    .sorted(Comparator.comparingDouble(MaintenanceRequest::getPriority).reversed())
                    .toList();
        }
        public Optional<MaintenanceRequest> findById(long id) { return Optional.ofNullable(requests.get(id)); }
        public MaintenanceRequest insert(MaintenanceRequest r) { r.setId(seq++); requests.put(r.getId(), r); return r; }
        public boolean update(MaintenanceRequest r) { return requests.replace(r.getId(), r) != null; }
        public boolean delete(long id) {
            records.values().removeIf(rec -> rec.getRequestId() == id);   // як ON DELETE CASCADE
            return requests.remove(id) != null;
        }
        public int countByEquipment(long id) {
            return (int) requests.values().stream().filter(r -> r.getEquipmentId() == id).count();
        }
        public int countBySpecialist(long id) {
            return (int) requests.values().stream().filter(r -> Objects.equals(r.getSpecialistId(), id)).count();
        }
    };

    public final ServiceRecordDao recordDao = new ServiceRecordDao() {
        public List<ServiceRecord> findByRequest(long rid) {
            return records.values().stream().filter(r -> r.getRequestId() == rid).toList();
        }
        public Optional<ServiceRecord> findById(long id) { return Optional.ofNullable(records.get(id)); }
        public ServiceRecord insert(ServiceRecord r) { r.setId(seq++); records.put(r.getId(), r); return r; }
        public boolean update(ServiceRecord r) { return records.replace(r.getId(), r) != null; }
        public boolean delete(long id) { return records.remove(id) != null; }
        public int countByRequest(long rid) { return findByRequest(rid).size(); }
    };
}
