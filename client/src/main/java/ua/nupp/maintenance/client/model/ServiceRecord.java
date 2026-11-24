package ua.nupp.maintenance.client.model;

import java.math.BigDecimal;

public class ServiceRecord {
    private Long id;
    private Long requestId;
    private Long specialistId;
    private String workDescription;
    private BigDecimal hoursSpent;
    private BigDecimal cost;
    private String performedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getSpecialistId() { return specialistId; }
    public void setSpecialistId(Long specialistId) { this.specialistId = specialistId; }
    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }
    public BigDecimal getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(BigDecimal hoursSpent) { this.hoursSpent = hoursSpent; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getPerformedAt() { return performedAt; }
    public void setPerformedAt(String performedAt) { this.performedAt = performedAt; }
}
