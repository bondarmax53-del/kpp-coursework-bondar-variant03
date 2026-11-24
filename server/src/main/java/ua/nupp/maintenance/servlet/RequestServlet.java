package ua.nupp.maintenance.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ua.nupp.maintenance.AppContextListener;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;
import ua.nupp.maintenance.model.ServiceRecord;
import ua.nupp.maintenance.model.StatusChange;
import ua.nupp.maintenance.service.MaintenanceService;
import ua.nupp.maintenance.service.ValidationException;

import java.io.IOException;

/**
 * GET  /api/requests[?status=NEW]     — список, відсортований за пріоритетом
 * GET  /api/requests/{id}             — одна заявка
 * POST /api/requests                  — нова заявка (пріоритет рахує сервер)
 * POST /api/requests/{id}/status      — перехід за життєвим циклом
 * GET  /api/requests/{id}/records     — сервісні записи заявки
 * POST /api/requests/{id}/records     — додати сервісний запис
 * PUT  /api/requests/{id}             — редагувати заявку (поки не виконана)
 * DELETE /api/requests/{id}           — видалити нову заявку
 * PUT, DELETE /api/requests/{id}/records/{recordId} — змінити / видалити запис
 */
public class RequestServlet extends ApiServlet {
    private transient MaintenanceService service;

    /** Конструктор без параметрів — Tomcat створює сервлет сам (web.xml). */
    public RequestServlet() {
    }

    /** Для тестів: сервіс передається напряму. */
    public RequestServlet(MaintenanceService service) {
        this.service = service;
    }

    @Override
    public void init() {
        if (service == null) {
            service = (MaintenanceService) getServletContext().getAttribute(AppContextListener.MAINTENANCE_SERVICE);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length == 0) {
            ok(resp, service.findAll(parseStatus(req.getParameter("status"))));
        } else if (p.length == 1) {
            ok(resp, service.get(parseId(p[0])));
        } else if (p.length == 2 && p[1].equals("records")) {
            ok(resp, service.records(parseId(p[0])));
        } else {
            notFoundRoute(resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length == 0) {
            created(resp, service.create(readJson(req, MaintenanceRequest.class)));
        } else if (p.length == 2 && p[1].equals("status")) {
            ok(resp, service.changeStatus(parseId(p[0]), readJson(req, StatusChange.class)));
        } else if (p.length == 2 && p[1].equals("records")) {
            created(resp, service.addRecord(parseId(p[0]), readJson(req, ServiceRecord.class)));
        } else {
            notFoundRoute(resp);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length == 1) {
            ok(resp, service.update(parseId(p[0]), readJson(req, MaintenanceRequest.class)));
        } else if (p.length == 3 && p[1].equals("records")) {
            ok(resp, service.updateRecord(parseId(p[0]), parseId(p[2]), readJson(req, ServiceRecord.class)));
        } else {
            notFoundRoute(resp);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length == 1) {
            service.delete(parseId(p[0]));
            noContent(resp);
        } else if (p.length == 3 && p[1].equals("records")) {
            service.deleteRecord(parseId(p[0]), parseId(p[2]));
            noContent(resp);
        } else {
            notFoundRoute(resp);
        }
    }

    private static RequestStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RequestStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Невідомий статус: " + raw);
        }
    }
}
