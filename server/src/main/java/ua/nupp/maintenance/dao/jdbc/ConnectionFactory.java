package ua.nupp.maintenance.dao.jdbc;

import ua.nupp.maintenance.dao.DaoException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Видає JDBC-з'єднання з MySQL. Пріоритет джерел налаштувань:
 *   1) змінні середовища DB_URL, DB_USER, DB_PASSWORD;
 *   2) db.properties у classpath (локальний файл, внесений до .gitignore);
 *   3) db-example.properties у classpath (приклад без пароля).
 * Абсолютних шляхів немає, пароль у репозиторій не потрапляє.
 */
public class ConnectionFactory {
    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());

    private final String url;
    private final String user;
    private final String password;

    public ConnectionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static ConnectionFactory fromEnvironment() {
        Properties p = load("db.properties");
        if (p == null) {
            p = load("db-example.properties");
        }
        if (p == null) {
            p = new Properties();
        }
        String url = env("DB_URL", p.getProperty("db.url"));
        String user = env("DB_USER", p.getProperty("db.user"));
        String password = env("DB_PASSWORD", p.getProperty("db.password", ""));
        if (url == null) {
            throw new IllegalStateException("Не задано URL бази даних (DB_URL або db.properties)");
        }
        // Пароль у журнал не пишемо
        LOG.info("Підключення до БД: " + url + ", користувач " + user);
        return new ConnectionFactory(url, user, password);
    }

    public Connection get() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DaoException("Немає з'єднання з базою даних", e);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static Properties load(String resource) {
        try (InputStream in = ConnectionFactory.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            LOG.warning("Не вдалося прочитати " + resource + ": " + e.getMessage());
            return null;
        }
    }
}
