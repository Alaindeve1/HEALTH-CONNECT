package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.activities.ChwConsultationListActivity;
import rw.ac.auca.finalprojectgroupfa.database.AppDatabase;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.models.KPICard;
import rw.ac.auca.finalprojectgroupfa.repositories.ConsultationRepository;
import rw.ac.auca.finalprojectgroupfa.utils.LocalNotificationUtil;

public class ChwDashboardViewModel extends AndroidViewModel {
    private ConsultationRepository consultationRepository;
    private FirebaseFirestore firestore;

    private MutableLiveData<List<KPICard>> chwKPIs = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> operationStatus = new MutableLiveData<>();

    private ListenerRegistration consultationsListener;
    private boolean consultationsListenerInitialized = false;
    private final Map<String, String> lastKnownStatusByRequestId = new HashMap<>();

    // Executor for database operations
    private Executor executor = Executors.newSingleThreadExecutor();

    public ChwDashboardViewModel(@NonNull Application application) {
        super(application);
        consultationRepository = new ConsultationRepository(application);
        firestore = FirebaseFirestore.getInstance();
    }

    // All consultations for the CHW, regardless of status
    public LiveData<List<ConsultationRequest>> getAllConsultations() {
        return consultationRepository.getAllRequests();
    }

    /**
     * Starts a Firestore listener for this CHW. Keeps Room synced and triggers local notifications
     * on status changes (accepted/declined/completed).
     */
    public void startListeningToConsultations(String chwId) {
        stopListening();
        if (chwId == null || chwId.isEmpty()) return;

        consultationsListenerInitialized = false;
        lastKnownStatusByRequestId.clear();

        consultationsListener = firestore.collection("consultations")
                .whereEqualTo("chwId", chwId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ConsultationRequest req;
                        try {
                            req = doc.toObject(ConsultationRequest.class);
                        } catch (Exception ex) {
                            continue;
                        }
                        if (req == null) continue;

                        if (req.getRequestId() == null || req.getRequestId().isEmpty()) {
                            req.setRequestId(doc.getId());
                        }

                        // Upsert into Room for lists + KPI counts
                        consultationRepository.insert(req);

                        // Notify on status transitions (skip initial snapshot)
                        if (consultationsListenerInitialized) {
                            String newStatus = req.getStatus() != null ? req.getStatus().toLowerCase() : "";
                            String oldStatus = lastKnownStatusByRequestId.get(req.getRequestId());
                            if (oldStatus != null && !oldStatus.equals(newStatus)) {
                                notifyStatusChange(req, oldStatus, newStatus);
                            }
                        }
                        lastKnownStatusByRequestId.put(req.getRequestId(), req.getStatus() != null ? req.getStatus().toLowerCase() : "");
                    }

                    consultationsListenerInitialized = true;
                });
    }

    public void stopListening() {
        if (consultationsListener != null) {
            consultationsListener.remove();
            consultationsListener = null;
        }
    }

    private void notifyStatusChange(ConsultationRequest req, String oldStatus, String newStatus) {
        try {
            String patientName = req.getPatientName() != null ? req.getPatientName() : "Patient";
            String title = "Consultation update";
            String message;

            switch (newStatus) {
                case "accepted":
                    message = "Your request for " + patientName + " was accepted.";
                    break;
                case "revoked":
                    message = "Your request for " + patientName + " was declined.";
                    break;
                case "completed":
                    message = "Consultation for " + patientName + " was completed.";
                    break;
                default:
                    message = "Status changed to " + newStatus;
            }

            Intent intent = new Intent(getApplication(), ChwConsultationListActivity.class);
            LocalNotificationUtil.show(getApplication(), title, message, intent);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
    }
    
    public LiveData<List<KPICard>> getChwKPIs() {
        return chwKPIs;
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
    
    /**
     * Loads KPI data for the CHW dashboard
     * @param chwId The ID of the CHW
     */
    public void loadChwKPIs(String chwId) {
        isLoading.setValue(true);
        
        executor.execute(() -> {
            try {
                // Get counts from the local database
                int totalPatients = AppDatabase.getDatabase(getApplication())
                        .patientDao().getPatientCountForUser(chwId);
                        
                int pendingRequests = AppDatabase.getDatabase(getApplication())
                        .consultationRequestDao().getRequestCountByStatus("pending");
                        
                int activeConsultations = AppDatabase.getDatabase(getApplication())
                        .consultationRequestDao().getRequestCountByStatus("accepted");
                        
                int completedToday = AppDatabase.getDatabase(getApplication())
                        .consultationRequestDao().getCompletedConsultationsCountForToday();
                
                // Create KPI cards specific to CHW
                List<KPICard> kpiCards = new ArrayList<>();
                
                // Total Patients
                kpiCards.add(new KPICard(
                    "1",
                    getApplication().getString(R.string.kpi_total_patients),
                    String.format("%,d", totalPatients),
                    "Under your care",
                    R.drawable.ic_people
                ));
                
                // Pending Requests
                kpiCards.add(new KPICard(
                    "2",
                    getApplication().getString(R.string.kpi_pending_requests),
                    String.format("%,d", pendingRequests),
                    "Awaiting response",
                    R.drawable.ic_pending
                ));
                
                // Active Consultations
                kpiCards.add(new KPICard(
                    "3",
                    getApplication().getString(R.string.kpi_active_consultations),
                    String.format("%,d", activeConsultations),
                    "In progress",
                    R.drawable.ic_consultation
                ));
                
                // Completed Today
                kpiCards.add(new KPICard(
                    "4",
                    getApplication().getString(R.string.kpi_completed_today),
                    String.format("%,d", completedToday),
                    "Consultations completed",
                    R.drawable.ic_check_circle
                ));
                
                // Update LiveData on the main thread
                chwKPIs.postValue(kpiCards);
                
            } catch (Exception e) {
                Log.e("ChwDashboardVM", "Error loading KPIs", e);
                operationStatus.postValue("Error loading dashboard data");
            } finally {
                isLoading.postValue(false);
            }
        });
    }
}
