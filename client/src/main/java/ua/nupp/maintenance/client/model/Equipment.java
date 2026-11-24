package ua.nupp.maintenance.client.model;

/**
 * Клієнтська модель обладнання. Поля збігаються із серверною моделлю,
 * дата передається рядком у форматі РРРР-ММ-ДД.
 */
public class Equipment {
    private Long id;
    private String inventoryNumber;
    private String name;
    private String model;
    private String location;
    private int criticality;
    private String commissionedDate;

    public Equipment() {
    }

    public Equipment(String inventoryNumber, String name, String model, String location,
                     int criticality, String commissionedDate) {
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
    public String getCommissionedDate() { return commissionedDate; }
    public void setCommissionedDate(String commissionedDate) { this.commissionedDate = commissionedDate; }

    /** Так обладнання показується у випадних списках. */
    @Override
    public String toString() {
        return inventoryNumber + " — " + name;
    }
}
