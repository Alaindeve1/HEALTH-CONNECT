package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.repositories.PatientRepository;
import java.util.List;
import java.util.UUID;

public class PatientViewModel extends AndroidViewModel {
    private PatientRepository patientRepository;
    private LiveData<List<Patient>> filteredPatients;
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> operationStatus = new MutableLiveData<>();

    public PatientViewModel(Application application) {
        super(application);
        patientRepository = new PatientRepository(application);

        filteredPatients = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return patientRepository.getAllPatients();
            } else {
                return patientRepository.searchPatients(query);
            }
        });

        isLoading.setValue(false);
    }

    public LiveData<List<Patient>> getFilteredPatients() {
        return filteredPatients;
    }

    // kept for backward compatibility if needed, but should act same as filtered
    public LiveData<List<Patient>> getAllPatients() {
        return filteredPatients;
    }

    public void insert(Patient patient) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                patientRepository.insert(patient);
                operationStatus.postValue("Patient added successfully!");
            } catch (Exception e) {
                operationStatus.postValue("Error adding patient: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void update(Patient patient) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                patientRepository.update(patient);
                operationStatus.postValue("Patient updated successfully!");
            } catch (Exception e) {
                operationStatus.postValue("Error updating patient: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void delete(Patient patient) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                patientRepository.delete(patient);
                operationStatus.postValue("Patient deleted successfully!");
            } catch (Exception e) {
                operationStatus.postValue("Error deleting patient: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getOperationStatus() {
        return operationStatus;
    }

    public void clearStatus() {
        operationStatus.setValue(null);
    }
}