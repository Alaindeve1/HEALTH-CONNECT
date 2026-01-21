package rw.ac.auca.finalprojectgroupfa.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rw.ac.auca.finalprojectgroupfa.database.AppDatabase;
import rw.ac.auca.finalprojectgroupfa.database.PatientDao;
import rw.ac.auca.finalprojectgroupfa.models.Patient;

public class PatientRepository {
    private PatientDao patientDao;
    private LiveData<List<Patient>> allPatients;
    private ExecutorService executor;

    private final String currentUserId;

    public PatientRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        patientDao = database.patientDao();
        currentUserId = FirebaseAuth.getInstance().getUid() != null ? FirebaseAuth.getInstance().getUid() : "";
        allPatients = patientDao.getPatientsForUser(currentUserId);
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Patient>> getAllPatients() {
        return allPatients;
    }

    public void insert(Patient patient) {
        executor.execute(() -> {
            patientDao.insert(patient);
        });
    }

    public void update(Patient patient) {
        executor.execute(() -> {
            patientDao.update(patient);
        });
    }

    public void delete(Patient patient) {
        executor.execute(() -> {
            patientDao.delete(patient);
        });
    }

    public LiveData<List<Patient>> searchPatients(String query) {
        return patientDao.searchPatientsForUser(currentUserId, query);
    }

    public Patient getPatientById(String uuid) {
        // Note: This runs on background thread
        final Patient[] patient = new Patient[1];
        executor.execute(() -> {
            patient[0] = patientDao.getPatientByLocalUUID( uuid);
        });
        try {
            Thread.sleep(200); // Small delay to wait for result
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            e.printStackTrace();
        }
        return patient[0];
    }
}