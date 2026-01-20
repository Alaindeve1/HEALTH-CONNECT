package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import java.util.List;

public class ChwConsultationViewModel extends AndroidViewModel {

    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<ConsultationRequest>> consultations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public ChwConsultationViewModel(@NonNull Application application) {
        super(application);
        firestore = FirebaseFirestore.getInstance();
        loadAllConsultationsForChw();
    }

    private void loadAllConsultationsForChw() {
        isLoading.setValue(true);
        String chwId = FirebaseAuth.getInstance().getUid();

        if (chwId == null) {
            isLoading.setValue(false);
            return;
        }

        firestore.collection("consultations")
                .whereEqualTo("chwId", chwId) // Find requests created by this CHW
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        return; // Handle error appropriately
                    }
                    if (value != null) {
                        consultations.setValue(value.toObjects(ConsultationRequest.class));
                    }
                });
    }

    public LiveData<List<ConsultationRequest>> getConsultations() {
        return consultations;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
