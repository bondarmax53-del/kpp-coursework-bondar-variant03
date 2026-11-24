package ua.nupp.maintenance.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ua.nupp.maintenance.dao.DaoException;
import ua.nupp.maintenance.service.ConflictException;
import ua.nupp.maintenance.service.NotFoundException;
import ua.nupp.maintenance.service.ValidationException;
import ua.nupp.maintenance.util.Json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Базовий сервлет REST API: читання/запис JSON, розбір шляху
 * і перетворення винятків сервісного шару на HTTP-коди.
 */
public abstract class ApiServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ApiServlet.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            super.service(req, resp);
        } catch (ValidationException | JsonProcessingException | IllegalArgumentException e) {
            LOG.info("400 " + req.getMethod() + " " + req.getRequestURI() + ": " + e.getMessage());
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, clientMessage(e));
        } catch (NotFoundException e) {
            LOG.info("404 " + req.getMethod() + " " + req.getRequestURI() + ": " + e.getMessage());
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (ConflictException e) {
            LOG.info("409 " + req.getMethod() + " " + req.getRequestURI() + ": " + e.getMessage());
            sendError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (DaoException e) {
            // Технічні деталі — лише в журнал, клієнту — загальне повідомлення
            LOG.log(Level.SEVERE, "Помилка БД: " + req.getMethod() + " " + req.getRequestURI(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Помилка роботи з базою даних. Спробуйте пізніше");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Неочікувана помилка: " + req.getMethod() + " " + req.getRequestURI(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера");
        }
    }

    /** Помилки розбору JSON Jackson дуже технічні — замінюємо зрозумілим текстом. */
    private static String clientMessage(Exception e) {
        return e instanceof JsonProcessingException
                ? "Некоректний формат JSON або недопустиме значення поля"
                : e.getMessage();
    }

    /** Сегменти шляху після адреси сервлета: "/5/status" → ["5", "status"]. */
    protected static String[] segments(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            return new String[0];
        }
        return path.substring(1).split("/");
    }

    protected static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException("Некоректний id: " + raw);
        }
    }

    protected static <T> T readJson(HttpServletRequest req, Class<T> type) throws IOException {
        T value = Json.MAPPER.readValue(req.getReader(), type);
        if (value == null) {
            throw new ValidationException("Порожнє тіло запиту");
        }
        return value;
    }

    protected static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        Json.MAPPER.writeValue(resp.getWriter(), body);
    }

    protected static void ok(HttpServletResponse resp, Object body) throws IOException {
        writeJson(resp, HttpServletResponse.SC_OK, body);
    }

    protected static void created(HttpServletResponse resp, Object body) throws IOException {
        writeJson(resp, HttpServletResponse.SC_CREATED, body);
    }

    protected static void noContent(HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", message == null ? "Помилка" : message);
        writeJson(resp, status, body);
    }

    protected static void notFoundRoute(HttpServletResponse resp) throws IOException {
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Невідомий маршрут");
    }
}
