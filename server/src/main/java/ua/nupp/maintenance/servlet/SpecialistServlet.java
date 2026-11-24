package ua.nupp.maintenance.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ua.nupp.maintenance.AppContextListener;
import ua.nupp.maintenance.model.Specialist;
import ua.nupp.maintenance.service.SpecialistService;

import java.io.IOException;

/** CRUD фахівців: /api/specialists, /api/specialists/{id}. */
public class SpecialistServlet extends ApiServlet {
    private transient SpecialistService service;

    /** Конструктор без параметрів — Tomcat створює сервлет сам (web.xml). */
    public SpecialistServlet() {
    }

    /** Для тестів: сервіс передається напряму. */
    public SpecialistServlet(SpecialistService service) {
        this.service = service;
    }

    @Override
    public void init() {
        if (service == null) {
            service = (SpecialistService) getServletContext().getAttribute(AppContextListener.SPECIALIST_SERVICE);
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
        created(resp, service.create(readJson(req, Specialist.class)));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] p = segments(req);
        if (p.length != 1) {
            notFoundRoute(resp);
            return;
        }
        ok(resp, service.update(parseId(p[0]), readJson(req, Specialist.class)));
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
