package ua.nupp.maintenance.model;

/**
 * Тіло запиту POST /api/requests/{id}/status.
 * specialistId і diagnosis потрібні лише для переходу NEW → DIAGNOSED.
 */
public class StatusChange {
    private RequestStatus status;
    private Long specialistId;
    private String diagnosis;

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Long getSpecialistId() { return specialistId; }
    public void setSpecialistId(Long specialistId) { this.specialistId = specialistId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
