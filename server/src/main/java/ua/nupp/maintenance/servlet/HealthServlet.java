package ua.nupp.maintenance.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** GET /api/health — перевірка, що сервер запущено (клієнт викликає при старті). */
public class HealthServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ok(resp, Map.of("status", "UP"));
    }
}
