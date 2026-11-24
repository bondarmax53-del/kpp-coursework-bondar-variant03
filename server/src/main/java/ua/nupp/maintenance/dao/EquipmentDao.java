package ua.nupp.maintenance.dao;

import ua.nupp.maintenance.model.Equipment;

import java.util.List;
import java.util.Optional;

public interface EquipmentDao {
    List<Equipment> findAll();
    Optional<Equipment> findById(long id);
    Optional<Equipment> findByInventoryNumber(String inventoryNumber);
    /** Зберігає новий запис і повертає його з присвоєним id. */
    Equipment insert(Equipment equipment);
    boolean update(Equipment equipment);
    boolean delete(long id);
}
