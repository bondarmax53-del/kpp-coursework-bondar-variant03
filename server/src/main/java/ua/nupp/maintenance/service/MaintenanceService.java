package ua.nupp.maintenance.service;

import ua.nupp.maintenance.dao.EquipmentDao;
import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.dao.ServiceRecordDao;
import ua.nupp.maintenance.dao.SpecialistDao;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;
import ua.nupp.maintenance.model.ServiceRecord;
import ua.nupp.maintenance.model.Specialist;
import ua.nupp.maintenance.model.StatusChange;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

/**
 * Бізнес-логіка заявок: створення з розрахунком пріоритету,
 * керування життєвим циклом і сервісні записи.
 */
public class MaintenanceService {
    private static final Logger LOG = Logger.getLogger(MaintenanceService.class.getName());
    private final MaintenanceRequestDao requestDao;
    private final EquipmentDao equipmentDao;
    private final SpecialistDao specialistDao;
    private final ServiceRecordDao recordDao;
    private final Clock clock;   // підміняється в тестах, щоб час був передбачуваним

    public MaintenanceService(MaintenanceRequestDao requestDao, EquipmentDao equipmentDao,
                              SpecialistDao specialistDao, ServiceRecordDao recordDao, Clock clock) {
        this.requestDao = requestDao;
        this.equipmentDao = equipmentDao;
        this.specialistDao = specialistDao;
        this.recordDao = recordDao;
        this.clock = clock;
    }

    /** Список заявок, відсортований за пріоритетом I (спочатку найтерміновіші). */
    public List<MaintenanceRequest> findAll(RequestStatus statusOrNull) {
        return requestDao.findAll(statusOrNull);
    }

    public MaintenanceRequest get(long id) {
        return requestDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявку з id=" + id + " не знайдено"));
    }

    public MaintenanceRequest create(MaintenanceRequest r) {
        Validators.requireNotNull(r, "заявка");
        Validators.requireNotNull(r.getEquipmentId(), "Обладнання");
        Equipment eq = equipmentDao.findById(r.getEquipmentId())
                .orElseThrow(() -> new NotFoundException("Обладнання з id=" + r.getEquipmentId() + " не знайдено"));
        r.setDescription(Validators.requireText(r.getDescription(), "Опис несправності", 1000));

        // calculate() сам перевіряє діапазони severity та downtimeHours
        r.setPriority(PriorityCalculator.calculate(eq.getCriticality(), r.getSeverity(), r.getDowntimeHours()));

        // Нова заявка завжди починає з NEW, без фахівця і діагнозу
        r.setId(null);
        r.setStatus(RequestStatus.NEW);
        r.setSpecialistId(null);
        r.setDiagnosis(null);
        LocalDateTime now = now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        MaintenanceRequest saved = requestDao.insert(r);
        LOG.info("Створено заявку id=" + saved.getId() + " на обладнання " + eq.getInventoryNumber()
                + ", I=" + saved.getPriority());
        return saved;
    }

    /**
     * Редагування заявки (обладнання, опис, тяжкість, простій) — доки вона не виконана.
     * Статус, фахівця й діагноз тут змінити не можна: для цього є changeStatus().
     */
    public MaintenanceRequest update(long id, MaintenanceRequest changes) {
        Validators.requireNotNull(changes, "заявка");
        MaintenanceRequest r = get(id);
        if (r.getStatus() == RequestStatus.DONE) {
            throw new ConflictException("Виконану заявку редагувати не можна");
        }
        Validators.requireNotNull(changes.getEquipmentId(), "Обладнання");
        Equipment eq = equipmentDao.findById(changes.getEquipmentId())
                .orElseThrow(() -> new NotFoundException("Обладнання з id=" + changes.getEquipmentId() + " не знайдено"));
        r.setEquipmentId(eq.getId());
        r.setDescription(Validators.requireText(changes.getDescription(), "Опис несправності", 1000));
        r.setSeverity(changes.getSeverity());
        r.setDowntimeHours(changes.getDowntimeHours());
        r.setPriority(PriorityCalculator.calculate(eq.getCriticality(), r.getSeverity(), r.getDowntimeHours()));
        r.setUpdatedAt(now());
        requestDao.update(r);
        LOG.info("Змінено заявку id=" + id + ", I=" + r.getPriority());
        return r;
    }

    /** Видалити можна лише нову заявку: по решті вже ведеться робота, вони зберігаються для історії. */
    public void delete(long id) {
        MaintenanceRequest r = get(id);
        if (r.getStatus() != RequestStatus.NEW) {
            throw new ConflictException("Видаляти можна лише заявки у статусі «Нова», поточний — «"
                    + r.getStatus().getTitle() + "»");
        }
        requestDao.delete(id);
        LOG.info("Видалено заявку id=" + id);
    }

    /**
     * Переводить заявку в наступний статус. Правила:
     *  NEW → DIAGNOSED    — потрібні активний фахівець і текст діагнозу;
     *  DIAGNOSED → IN_SERVICE — без додаткових умов;
     *  IN_SERVICE → DONE  — має бути хоча б один сервісний запис.
     * Будь-який інший перехід (назад, через крок, з DONE) — ConflictException.
     */
    public MaintenanceRequest changeStatus(long id, StatusChange change) {
        Validators.requireNotNull(change, "зміна статусу");
        RequestStatus target = Validators.requireNotNull(change.getStatus(), "Статус");
        MaintenanceRequest r = get(id);
        RequestStatus current = r.getStatus();

        if (!current.canMoveTo(target)) {
            throw new ConflictException("Недопустимий перехід: " + current.getTitle() + " → " + target.getTitle()
                    + (current.next() == null ? " (заявка вже виконана)"
                    : ". Дозволено лише → " + current.next().getTitle()));
        }

        switch (target) {
            case DIAGNOSED -> {
                Long specialistId = Validators.requireNotNull(change.getSpecialistId(), "Фахівець");
                Specialist s = specialistDao.findById(specialistId)
                        .orElseThrow(() -> new NotFoundException("Фахівця з id=" + specialistId + " не знайдено"));
                if (!s.isActive()) {
                    throw new ConflictException("Фахівець " + s.getFullName() + " неактивний");
                }
                r.setSpecialistId(specialistId);
                r.setDiagnosis(Validators.requireText(change.getDiagnosis(), "Діагноз", 1000));
            }
            case DONE -> {
                if (recordDao.countByRequest(id) == 0) {
                    throw new ConflictException("Не можна завершити заявку без жодного сервісного запису");
                }
            }
            default -> {
                // IN_SERVICE — додаткових умов немає
            }
        }

        r.setStatus(target);
        r.setUpdatedAt(now());
        requestDao.update(r);
        LOG.info("Заявка id=" + id + ": " + current + " → " + target);
        return r;
    }

    public List<ServiceRecord> records(long requestId) {
        get(requestId);
        return recordDao.findByRequest(requestId);
    }

    /** Сервісний запис можна додати лише до заявки у статусі «Обслуговування». */
    public ServiceRecord addRecord(long requestId, ServiceRecord rec) {
        MaintenanceRequest r = requireInService(requestId);
        validateRecord(rec, r);
        rec.setId(null);
        rec.setRequestId(requestId);
        if (rec.getPerformedAt() == null) {
            rec.setPerformedAt(now());
        }
        ServiceRecord saved = recordDao.insert(rec);
        LOG.info("Додано сервісний запис id=" + saved.getId() + " до заявки id=" + requestId);
        return saved;
    }

    public ServiceRecord updateRecord(long requestId, long recordId, ServiceRecord rec) {
        MaintenanceRequest r = requireInService(requestId);
        ServiceRecord old = requireRecord(requestId, recordId);
        validateRecord(rec, r);
        rec.setId(recordId);
        rec.setRequestId(requestId);
        if (rec.getPerformedAt() == null) {
            rec.setPerformedAt(old.getPerformedAt());
        }
        recordDao.update(rec);
        LOG.info("Змінено сервісний запис id=" + recordId);
        return rec;
    }

    public void deleteRecord(long requestId, long recordId) {
        requireInService(requestId);
        requireRecord(requestId, recordId);
        recordDao.delete(recordId);
        LOG.info("Видалено сервісний запис id=" + recordId);
    }

    private MaintenanceRequest requireInService(long requestId) {
        MaintenanceRequest r = get(requestId);
        if (r.getStatus() != RequestStatus.IN_SERVICE) {
            throw new ConflictException("Сервісні записи змінюються лише у статусі «Обслуговування», зараз — «"
                    + r.getStatus().getTitle() + "»");
        }
        return r;
    }

    private ServiceRecord requireRecord(long requestId, long recordId) {
        ServiceRecord rec = recordDao.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Сервісний запис з id=" + recordId + " не знайдено"));
        if (rec.getRequestId() == null || rec.getRequestId() != requestId) {
            throw new NotFoundException("Сервісний запис id=" + recordId + " не належить заявці id=" + requestId);
        }
        return rec;
    }

    private void validateRecord(ServiceRecord rec, MaintenanceRequest r) {
        Validators.requireNotNull(rec, "сервісний запис");
        rec.setWorkDescription(Validators.requireText(rec.getWorkDescription(), "Опис робіт", 1000));
        BigDecimal hours = Validators.requireNotNull(rec.getHoursSpent(), "Витрачено годин");
        if (hours.signum() <= 0) {
            throw new ValidationException("Витрачений час має бути більшим за 0");
        }
        if (rec.getCost() == null) {
            rec.setCost(BigDecimal.ZERO);
        } else if (rec.getCost().signum() < 0) {
            throw new ValidationException("Вартість не може бути від'ємною");
        }
        // Якщо виконавця не вказано — це фахівець, призначений на заявку
        if (rec.getSpecialistId() == null) {
            rec.setSpecialistId(r.getSpecialistId());
        } else if (specialistDao.findById(rec.getSpecialistId()).isEmpty()) {
            throw new NotFoundException("Фахівця з id=" + rec.getSpecialistId() + " не знайдено");
        }
    }

    private LocalDateTime now() {
        // MySQL DATETIME зберігає секунди, тож відкидаємо наносекунди
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
