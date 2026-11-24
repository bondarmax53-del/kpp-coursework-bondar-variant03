package ua.nupp.maintenance.client.model;

/** Тіло запиту зміни статусу заявки. */
public class StatusChange {
    private RequestStatus status;
    private Long specialistId;
    private String diagnosis;

    public StatusChange() {
    }

    public StatusChange(RequestStatus status, Long specialistId, String diagnosis) {
        this.status = status;
        this.specialistId = specialistId;
        this.diagnosis = diagnosis;
    }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public Long getSpecialistId() { return specialistId; }
    public void setSpecialistId(Long specialistId) { this.specialistId = specialistId; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
