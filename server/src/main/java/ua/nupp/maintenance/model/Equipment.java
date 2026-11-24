package ua.nupp.maintenance.model;

import java.time.LocalDate;

/** Одиниця лабораторного обладнання. */
public class Equipment {
    private Long id;
    private String inventoryNumber;   // інвентарний номер, унікальний
    private String name;
    private String model;
    private String location;          // аудиторія / лабораторія
    private int criticality;          // критичність 1..5 (джерело фактора F1)
    private LocalDate commissionedDate;

    public Equipment() {
    }

    public Equipment(Long id, String inventoryNumber, String name, String model,
                     String location, int criticality, LocalDate commissionedDate) {
        this.id = id;
        this.inventoryNumber = inventoryNumber;
        this.name = name;
        this.model = model;
        this.location = location;
        this.criticality = criticality;
        this.commissionedDate = commissionedDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInventoryNumber() { return inventoryNumber; }
    public void setInventoryNumber(String inventoryNumber) { this.inventoryNumber = inventoryNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCriticality() { return criticality; }
    public void setCriticality(int criticality) { this.criticality = criticality; }

    public LocalDate getCommissionedDate() { return commissionedDate; }
    public void setCommissionedDate(LocalDate commissionedDate) { this.commissionedDate = commissionedDate; }
}
