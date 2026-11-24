package ua.nupp.maintenance.client.model;

public class MaintenanceRequest {
    private Long id;
    private Long equipmentId;
    private Long specialistId;
    private String description;
    private int severity;
    private int downtimeHours;
    private String diagnosis;
    private RequestStatus status;
    private double priority;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getSpecialistId() { return specialistId; }
    public void setSpecialistId(Long specialistId) { this.specialistId = specialistId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public int getDowntimeHours() { return downtimeHours; }
    public void setDowntimeHours(int downtimeHours) { this.downtimeHours = downtimeHours; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public double getPriority() { return priority; }
    public void setPriority(double priority) { this.priority = priority; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
