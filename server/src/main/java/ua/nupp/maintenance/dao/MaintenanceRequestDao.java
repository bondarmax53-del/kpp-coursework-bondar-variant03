package ua.nupp.maintenance.dao;

import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestDao {
    /** Усі заявки (або лише з указаним статусом), від найвищого пріоритету до найнижчого. */
    List<MaintenanceRequest> findAll(RequestStatus statusOrNull);
    Optional<MaintenanceRequest> findById(long id);
    MaintenanceRequest insert(MaintenanceRequest request);
    boolean update(MaintenanceRequest request);
    boolean delete(long id);
    int countByEquipment(long equipmentId);
    int countBySpecialist(long specialistId);
}
