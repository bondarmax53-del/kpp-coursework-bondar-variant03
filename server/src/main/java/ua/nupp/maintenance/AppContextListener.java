package ua.nupp.maintenance;

import ua.nupp.maintenance.dao.EquipmentDao;
import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.dao.ServiceRecordDao;
import ua.nupp.maintenance.dao.SpecialistDao;
import ua.nupp.maintenance.dao.jdbc.ConnectionFactory;
import ua.nupp.maintenance.dao.jdbc.JdbcEquipmentDao;
import ua.nupp.maintenance.dao.jdbc.JdbcMaintenanceRequestDao;
import ua.nupp.maintenance.dao.jdbc.JdbcServiceRecordDao;
import ua.nupp.maintenance.dao.jdbc.JdbcSpecialistDao;
import ua.nupp.maintenance.service.EquipmentService;
import ua.nupp.maintenance.service.MaintenanceService;
import ua.nupp.maintenance.service.SpecialistService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Запускається Tomcat один раз під час розгортання застосунку.
 * Збирає шари DAO → Service і кладе сервіси в ServletContext,
 * звідки їх беруть сервлети. Також налаштовує файловий журнал.
 */
public class AppContextListener implements ServletContextListener {
    public static final String EQUIPMENT_SERVICE = "equipmentService";
    public static final String SPECIALIST_SERVICE = "specialistService";
    public static final String MAINTENANCE_SERVICE = "maintenanceService";

    private static final Logger ROOT = Logger.getLogger("ua.nupp.maintenance");
    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());
    private FileHandler fileHandler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        setUpFileLog();
        try {
            // У веб-застосунку драйвер краще зареєструвати явно
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Драйвер MySQL не знайдено", e);
        }

        ConnectionFactory cf = ConnectionFactory.fromEnvironment();
        EquipmentDao equipmentDao = new JdbcEquipmentDao(cf);
        SpecialistDao specialistDao = new JdbcSpecialistDao(cf);
        MaintenanceRequestDao requestDao = new JdbcMaintenanceRequestDao(cf);
        ServiceRecordDao recordDao = new JdbcServiceRecordDao(cf);

        ServletContext ctx = event.getServletContext();
        ctx.setAttribute(EQUIPMENT_SERVICE, new EquipmentService(equipmentDao, requestDao));
        ctx.setAttribute(SPECIALIST_SERVICE, new SpecialistService(specialistDao, requestDao));
        ctx.setAttribute(MAINTENANCE_SERVICE, new MaintenanceService(
                requestDao, equipmentDao, specialistDao, recordDao, Clock.systemDefaultZone()));
        LOG.info("Сервер ІС обслуговування обладнання запущено");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOG.info("Сервер зупинено");
        if (fileHandler != null) {
            ROOT.removeHandler(fileHandler);
            fileHandler.close();
        }
    }

    /** Журнал у каталозі logs Tomcat (або в робочому каталозі) — один обробник на весь час роботи. */
    private void setUpFileLog() {
        String base = System.getProperty("catalina.base");
        File dir = base == null ? new File("logs") : new File(base, "logs");
        try {
            dir.mkdirs();
            fileHandler = new FileHandler(new File(dir, "lab-maintenance.log").getPath(), true);
            fileHandler.setEncoding("UTF-8");
            fileHandler.setFormatter(new SimpleFormatter());
            ROOT.addHandler(fileHandler);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Файловий журнал недоступний, лише консоль", e);
        }
    }
}
