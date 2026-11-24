package ua.nupp.maintenance.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ua.nupp.maintenance.AppContextListener;
import ua.nupp.maintenance.model.Equipment;
import ua.nupp.maintenance.service.EquipmentService;

import java.io.IOException;

/**
 * GET    /api/equipment        — список
 * GET    /api/equipment/{id}   — один запис
 * POST   /api/equipment        — створити
 * PUT    /api/equipment/{id}   — змінити
 * DELETE /api/equipment/{id}   — видалити
 */
public class EquipmentServlet extends ApiServlet {
    private transient EquipmentService service;

    /** Конструктор без параметрів — Tomcat створює сервлет сам (web.xml). */
    public EquipmentServlet() {
    }

    /** Для тестів: сервіс передається напряму. */
    public EquipmentServlet(EquipmentService service) {
        this.service = service;
    }

    @Override
    public void init() {
        if (service == null) {
            service = (EquipmentService) getServletContext().getAttribute(AppContextListener.EQUIPMENT_SERVICE);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length == 0) {
            ok(resp, service.findAll());
        } else if (p.length == 1) {
            ok(resp, service.get(parseId(p[0])));
        } else {
            notFoundRoute(resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (segments(req).length != 0) {
            notFoundRoute(resp);
            return;
        }
        created(resp, service.create(readJson(req, Equipment.class)));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length != 1) {
            notFoundRoute(resp);
            return;
        }
        ok(resp, service.update(parseId(p[0]), readJson(req, Equipment.class)));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length != 1) {
            notFoundRoute(resp);
            return;
        }
        service.delete(parseId(p[0]));
        noContent(resp);
    }
}
