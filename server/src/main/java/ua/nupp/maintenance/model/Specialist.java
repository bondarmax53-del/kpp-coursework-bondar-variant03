package ua.nupp.maintenance.model;

/** Фахівець (інженер), який діагностує й обслуговує обладнання. */
public class Specialist {
    private Long id;
    private String fullName;
    private String specialization;
    private String phone;
    private String email;
    private boolean active = true;

    public Specialist() {
    }

    public Specialist(Long id, String fullName, String specialization,
                      String phone, String email, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
