package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.repositories.ConsultationRepository;
import java.util.List;

public class ReportsViewModel extends AndroidViewModel {
    private ConsultationRepository repository;
    private LiveData<List<ConsultationRequest>> completedConsultations;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        repository = new ConsultationRepository(application);
        // Assuming "completed" is the status for reports
        completedConsultations = repository.getRequestsByStatus("completed");
    }

    public LiveData<List<ConsultationRequest>> getCompletedConsultations() {
        return completedConsultations;
    }
}
