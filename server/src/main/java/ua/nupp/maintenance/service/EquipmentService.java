package ua.nupp.maintenance.service;

import ua.nupp.maintenance.dao.EquipmentDao;
import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;

import java.util.List;
import java.util.logging.Logger;

public class EquipmentService {
    private static final Logger LOG = Logger.getLogger(EquipmentService.class.getName());
    private final EquipmentDao equipmentDao;
    private final MaintenanceRequestDao requestDao;

    public EquipmentService(EquipmentDao equipmentDao, MaintenanceRequestDao requestDao) {
        this.equipmentDao = equipmentDao;
        this.requestDao = requestDao;
    }

    public List<Equipment> findAll() {
        return equipmentDao.findAll();
    }

    public Equipment get(long id) {
        return equipmentDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Обладнання з id=" + id + " не знайдено"));
    }

    public Equipment create(Equipment e) {
        normalize(e);
        if (equipmentDao.findByInventoryNumber(e.getInventoryNumber()).isPresent()) {
            throw new ConflictException("Інвентарний номер " + e.getInventoryNumber() + " уже існує");
        }
        e.setId(null);
        Equipment saved = equipmentDao.insert(e);
        LOG.info("Додано обладнання id=" + saved.getId() + " (" + saved.getInventoryNumber() + ")");
        return saved;
    }

    public Equipment update(long id, Equipment e) {
        Equipment old = get(id);
        normalize(e);
        equipmentDao.findByInventoryNumber(e.getInventoryNumber())
                .filter(other -> other.getId() != id)
                .ifPresent(other -> {
                    throw new ConflictException("Інвентарний номер " + e.getInventoryNumber() + " уже існує");
                });
        e.setId(id);
        equipmentDao.update(e);
        LOG.info("Змінено обладнання id=" + id);
        // Критичність входить у формулу пріоритету — перераховуємо відкриті заявки
        if (old.getCriticality() != e.getCriticality()) {
            recalculateOpenRequests(e);
        }
        return e;
    }

    public void delete(long id) {
        get(id);
        if (requestDao.countByEquipment(id) > 0) {
            throw new ConflictException("Неможливо видалити обладнання: на нього є заявки");
        }
        equipmentDao.delete(id);
        LOG.info("Видалено обладнання id=" + id);
    }

    private void recalculateOpenRequests(Equipment e) {
        for (MaintenanceRequest r : requestDao.findAll(null)) {
            if (r.getEquipmentId().equals(e.getId()) && r.getStatus() != RequestStatus.DONE) {
                r.setPriority(PriorityCalculator.calculate(e.getCriticality(), r.getSeverity(), r.getDowntimeHours()));
                requestDao.update(r);
                LOG.info("Перераховано пріоритет заявки id=" + r.getId() + ": I=" + r.getPriority());
            }
        }
    }

    private static void normalize(Equipment e) {
        Validators.requireNotNull(e, "обладнання");
        e.setInventoryNumber(Validators.requireText(e.getInventoryNumber(), "Інвентарний номер", 32));
        e.setName(Validators.requireText(e.getName(), "Назва", 120));
        e.setModel(Validators.optionalText(e.getModel(), 120, "Модель"));
        e.setLocation(Validators.optionalText(e.getLocation(), 64, "Розташування"));
        if (e.getCriticality() < 1 || e.getCriticality() > 5) {
            throw new ValidationException("Критичність обладнання має бути в межах 1..5");
        }
    }
}
