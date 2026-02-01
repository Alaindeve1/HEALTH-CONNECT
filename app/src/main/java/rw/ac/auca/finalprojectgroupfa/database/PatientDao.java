package rw.ac.auca.finalprojectgroupfa.database;


import androidx.lifecycle.LiveData;
import androidx.room.*;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import java.util.List;

@Dao
public interface PatientDao {
    @Insert
    void insert(Patient patient);

    @Update
    void update(Patient patient);

    @Delete
    void delete(Patient patient);

    @Query("SELECT * FROM patients WHERE createdByUserId = :userId ORDER BY createdAt DESC")
    LiveData<List<Patient>> getPatientsForUser(String userId);


    @Query("SELECT * FROM patients WHERE localUUID = :localUUID")
    Patient getPatientByLocalUUID(String localUUID);

    @Query("SELECT * FROM patients WHERE fhirPatientId = :fhirPatientId LIMIT 1")
    Patient getPatientByFhirId(String fhirPatientId);

    @Query("SELECT * FROM patients WHERE createdByUserId = :userId AND (name LIKE '%' || :query || '%' OR village LIKE '%' || :query || '%')")
    LiveData<List<Patient>> searchPatientsForUser(String userId, String query);

    @Query("SELECT * FROM patients WHERE synced = 0")
    List<Patient> getUnsyncedPatients();
    
    // Get total count of patients for a specific user
    @Query("SELECT COUNT(*) FROM patients WHERE createdByUserId = :userId")
    int getPatientCountForUser(String userId);
}