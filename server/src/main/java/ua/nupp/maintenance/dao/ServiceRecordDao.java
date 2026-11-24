package ua.nupp.maintenance.dao;

import ua.nupp.maintenance.model.ServiceRecord;

import java.util.List;
import java.util.Optional;

public interface ServiceRecordDao {
    List<ServiceRecord> findByRequest(long requestId);
    Optional<ServiceRecord> findById(long id);
    ServiceRecord insert(ServiceRecord record);
    boolean update(ServiceRecord record);
    boolean delete(long id);
    int countByRequest(long requestId);
}
