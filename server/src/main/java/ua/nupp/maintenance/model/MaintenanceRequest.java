package ua.nupp.maintenance.model;

import java.time.LocalDateTime;

/** Заявка на технічне обслуговування обладнання. */
public class MaintenanceRequest {
    private Long id;
    private Long equipmentId;
    private Long specialistId;        // призначається під час діагностики
    private String description;       // опис несправності від користувача
    private int severity;             // тяжкість несправності 1..5 (фактор F2)
    private int downtimeHours;        // очікуваний простій, год (фактор F3)
    private String diagnosis;         // заповнює фахівець
    private RequestStatus status = RequestStatus.NEW;
    private double priority;          // інтегральний показник I, 0..100
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaintenanceRequest() {
    }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
