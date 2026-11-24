package ua.nupp.maintenance.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.nupp.maintenance.client.model.Equipment;
import ua.nupp.maintenance.client.model.MaintenanceRequest;
import ua.nupp.maintenance.client.model.RequestStatus;
import ua.nupp.maintenance.client.model.ServiceRecord;
import ua.nupp.maintenance.client.model.Specialist;
import ua.nupp.maintenance.client.model.StatusChange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Єдина точка зв'язку JavaFX-клієнта із сервером (HTTP/JSON).
 * GUI не знає про HTTP: він викликає методи цього класу й отримує об'єкти моделі.
 */
public class ApiClient {
    private static final Logger LOG = Logger.getLogger(ApiClient.class.getName());

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Адреса сервера: змінна SERVER_URL або client.properties у classpath. */
    public static ApiClient fromConfig() {
        String url = System.getenv("SERVER_URL");
        if (url == null || url.isBlank()) {
            Properties p = new Properties();
            try (InputStream in = ApiClient.class.getClassLoader().getResourceAsStream("client.properties")) {
                if (in != null) {
                    p.load(in);
                }
            } catch (IOException e) {
                LOG.warning("Не вдалося прочитати client.properties: " + e.getMessage());
            }
            url = p.getProperty("server.url", "http://localhost:8080/lab-maintenance-server");
        }
        return new ApiClient(url);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ---------------- сервер ----------------

    public boolean isServerUp() {
        try {
            send("GET", "/api/health", null);
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    // ---------------- обладнання ----------------

    public List<Equipment> getEquipment() {
        return read(send("GET", "/api/equipment", null), new TypeReference<List<Equipment>>() { });
    }

    public Equipment createEquipment(Equipment e) {
        return read(send("POST", "/api/equipment", e), Equipment.class);
    }

    public Equipment updateEquipment(Equipment e) {
        return read(send("PUT", "/api/equipment/" + e.getId(), e), Equipment.class);
    }

    public void deleteEquipment(long id) {
        send("DELETE", "/api/equipment/" + id, null);
    }

    // ---------------- фахівці ----------------

    public List<Specialist> getSpecialists() {
        return read(send("GET", "/api/specialists", null), new TypeReference<List<Specialist>>() { });
    }

    public Specialist createSpecialist(Specialist s) {
        return read(send("POST", "/api/specialists", s), Specialist.class);
    }

    public Specialist updateSpecialist(Specialist s) {
        return read(send("PUT", "/api/specialists/" + s.getId(), s), Specialist.class);
    }

    public void deleteSpecialist(long id) {
        send("DELETE", "/api/specialists/" + id, null);
    }

    // ---------------- заявки ----------------

    public List<MaintenanceRequest> getRequests(RequestStatus statusOrNull) {
        String q = statusOrNull == null ? "" : "?status=" + statusOrNull.name();
        return read(send("GET", "/api/requests" + q, null), new TypeReference<List<MaintenanceRequest>>() { });
    }

    public MaintenanceRequest createRequest(MaintenanceRequest r) {
        return read(send("POST", "/api/requests", r), MaintenanceRequest.class);
    }

    public MaintenanceRequest updateRequest(MaintenanceRequest r) {
        return read(send("PUT", "/api/requests/" + r.getId(), r), MaintenanceRequest.class);
    }

    public void deleteRequest(long id) {
        send("DELETE", "/api/requests/" + id, null);
    }

    public MaintenanceRequest changeStatus(long requestId, StatusChange change) {
        return read(send("POST", "/api/requests/" + requestId + "/status", change), MaintenanceRequest.class);
    }

    public List<ServiceRecord> getRecords(long requestId) {
        return read(send("GET", "/api/requests/" + requestId + "/records", null), new TypeReference<List<ServiceRecord>>() { });
    }

    public ServiceRecord addRecord(long requestId, ServiceRecord rec) {
        return read(send("POST", "/api/requests/" + requestId + "/records", rec), ServiceRecord.class);
    }

    public ServiceRecord updateRecord(ServiceRecord rec) {
        return read(send("PUT", "/api/requests/" + rec.getRequestId() + "/records/" + rec.getId(), rec),
                ServiceRecord.class);
    }

    public void deleteRecord(long requestId, long recordId) {
        send("DELETE", "/api/requests/" + requestId + "/records/" + recordId, null);
    }

    // ---------------- HTTP ----------------

    /** Надсилає запит і повертає тіло відповіді; для кодів ≥ 400 кидає ApiException з текстом від сервера. */
    private String send(String method, String path, Object body) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .method(method, publisher)
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                String msg = errorMessage(resp.body(), resp.statusCode());
                LOG.info(method + " " + path + " → " + resp.statusCode() + ": " + msg);
                throw new ApiException(resp.statusCode(), msg);
            }
            return resp.body();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Сервер недоступний: " + method + " " + path, e);
            throw new ApiException("Сервер недоступний (" + baseUrl + "). Перевірте, що Tomcat запущено", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Запит перервано", e);
        }
    }

    private String errorMessage(String body, int status) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node != null && node.hasNonNull("error")) {
                return node.get("error").asText();
            }
        } catch (IOException ignored) {
            // тіло не JSON — нижче загальне повідомлення
        }
        return "Помилка сервера (код " + status + ")";
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException("Некоректна відповідь сервера", e);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException("Некоректна відповідь сервера", e);
        }
    }
}
