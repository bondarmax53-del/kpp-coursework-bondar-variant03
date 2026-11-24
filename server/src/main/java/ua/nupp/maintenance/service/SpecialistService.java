package ua.nupp.maintenance.service;

import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.dao.SpecialistDao;
import ua.nupp.maintenance.model.Specialist;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SpecialistService {
    private static final Logger LOG = Logger.getLogger(SpecialistService.class.getName());
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SpecialistDao specialistDao;
    private final MaintenanceRequestDao requestDao;

    public SpecialistService(SpecialistDao specialistDao, MaintenanceRequestDao requestDao) {
        this.specialistDao = specialistDao;
        this.requestDao = requestDao;
    }

    public List<Specialist> findAll() {
        return specialistDao.findAll();
    }

    public Specialist get(long id) {
        return specialistDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Фахівця з id=" + id + " не знайдено"));
    }

    public Specialist create(Specialist s) {
        normalize(s);
        s.setId(null);
        Specialist saved = specialistDao.insert(s);
        LOG.info("Додано фахівця id=" + saved.getId());
        return saved;
    }

    public Specialist update(long id, Specialist s) {
        get(id);
        normalize(s);
        s.setId(id);
        specialistDao.update(s);
        LOG.info("Змінено фахівця id=" + id);
        return s;
    }

    public void delete(long id) {
        get(id);
        if (requestDao.countBySpecialist(id) > 0) {
            throw new ConflictException("Неможливо видалити фахівця: за ним закріплені заявки. Позначте його неактивним");
        }
        specialistDao.delete(id);
        LOG.info("Видалено фахівця id=" + id);
    }

    private static void normalize(Specialist s) {
        Validators.requireNotNull(s, "фахівець");
        s.setFullName(Validators.requireText(s.getFullName(), "ПІБ", 120));
        s.setSpecialization(Validators.optionalText(s.getSpecialization(), 120, "Спеціалізація"));
        s.setPhone(Validators.optionalText(s.getPhone(), 20, "Телефон"));
        s.setEmail(Validators.optionalText(s.getEmail(), 120, "Email"));
        if (s.getEmail() != null && !EMAIL.matcher(s.getEmail()).matches()) {
            throw new ValidationException("Некоректний email: " + s.getEmail());
        }
    }
}
