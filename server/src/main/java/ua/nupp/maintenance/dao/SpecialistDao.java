package ua.nupp.maintenance.dao;

import ua.nupp.maintenance.model.Specialist;

import java.util.List;
import java.util.Optional;

public interface SpecialistDao {
    List<Specialist> findAll();
    Optional<Specialist> findById(long id);
    Specialist insert(Specialist specialist);
    boolean update(Specialist specialist);
    boolean delete(long id);
}
