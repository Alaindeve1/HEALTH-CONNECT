package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.*;
import rw.ac.auca.finalprojectgroupfa.models.AdminUser;
import rw.ac.auca.finalprojectgroupfa.models.SystemAnalytics;
import java.util.*;

public class AdminViewModel extends AndroidViewModel {
    private FirebaseFirestore firestore;
    private MutableLiveData<List<AdminUser>> pendingUsers = new MutableLiveData<>();
    private MutableLiveData<List<AdminUser>> allUsers = new MutableLiveData<>();
    private MutableLiveData<SystemAnalytics> systemAnalytics = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> operationStatus = new MutableLiveData<>();

    public AdminViewModel(Application application) {
        super(application);
        firestore = FirebaseFirestore.getInstance();
        isLoading.setValue(false);

        loadPendingUsers();
        loadAllUsers();
        loadSystemAnalytics();
    }

    private void loadPendingUsers() {
        firestore.collection("users")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        operationStatus.setValue("Error loading pending users: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<AdminUser> users = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AdminUser user = doc.toObject(AdminUser.class);
                            if (user != null) {
                                user.setUserId(doc.getId());
                                users.add(user);
                            }
                        }
                        pendingUsers.setValue(users);
                    }
                });
    }

    private void loadAllUsers() {
        firestore.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        operationStatus.setValue("Error loading users: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<AdminUser> users = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AdminUser user = doc.toObject(AdminUser.class);
                            if (user != null) {
                                user.setUserId(doc.getId());
                                users.add(user);
                            }
                        }
                        allUsers.setValue(users);
                    }
                });
    }

    private void loadSystemAnalytics() {
        // This would typically aggregate data from multiple collections
        // For now, we'll create mock analytics
        SystemAnalytics analytics = new SystemAnalytics();
        analytics.setTotalUsers(45);
        analytics.setTotalPatients(128);
        analytics.setTotalConsultations(67);
        analytics.setActiveConsultations(8);
        analytics.setCompletedConsultations(59);
        analytics.setSyncQueueSize(3);
        analytics.setFailedSyncs(2);
        analytics.setLastUpdated(new Date());

        // Mock data for charts
        Map<String, Integer> consultationsByDistrict = new HashMap<>();
        consultationsByDistrict.put("Kigali", 25);
        consultationsByDistrict.put("Northern", 15);
        consultationsByDistrict.put("Southern", 12);
        consultationsByDistrict.put("Eastern", 8);
        consultationsByDistrict.put("Western", 7);
        analytics.setConsultationsByDistrict(consultationsByDistrict);

        Map<String, Integer> usersByRole = new HashMap<>();
        usersByRole.put("CHW", 25);
        usersByRole.put("Doctor", 15);
        usersByRole.put("Nurse", 5);
        usersByRole.put("Admin", 2);
        analytics.setUsersByRole(usersByRole);

        systemAnalytics.setValue(analytics);
    }

    public void approveUser(String userId, String approvedBy) {
        isLoading.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("approvedAt", new Date());
        updates.put("approvedBy", approvedBy);

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    operationStatus.setValue("User approved successfully");
                    isLoading.setValue(false);

                    // Send notification to user
                    sendUserApprovalNotification(userId);
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error approving user: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void disableUser(String userId, String updatedBy) {
        isLoading.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "disabled");
        updates.put("disabledAt", new Date());
        updates.put("disabledBy", updatedBy);

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    operationStatus.setValue("User disabled");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error disabling user: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void activateUser(String userId, String updatedBy) {
        // Reactivate by setting approved.
        approveUser(userId, updatedBy);
    }

    public void removeUser(String userId, String updatedBy) {
        isLoading.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "deleted");
        updates.put("deletedAt", new Date());
        updates.put("deletedBy", updatedBy);

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    operationStatus.setValue("User removed");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error removing user: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void rejectUser(String userId, String rejectedBy, String reason) {
        isLoading.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("approvedBy", rejectedBy);
        updates.put("rejectionReason", reason);

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    operationStatus.setValue("User rejected");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error rejecting user: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void createUser(AdminUser user) {
        isLoading.setValue(true);

        String userId = firestore.collection("users").document().getId();
        user.setUserId(userId);

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    operationStatus.setValue("User created successfully");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error creating user: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    private void sendUserApprovalNotification(String userId) {
        // In production, this would send a push notification to the user
        // For now, we'll just log it
        Log.d("AdminViewModel", "Sending approval notification to user: " + userId);

        // You would typically use FCM to send a notification like:
        // "Your account has been approved! You can now access the Telehealth app."
    }

    public void forceSync() {
        isLoading.setValue(true);

        // Trigger manual sync
        // This would typically trigger your SyncWorker
        operationStatus.setValue("Manual sync triggered");
        isLoading.setValue(false);
    }

    // Getters for LiveData
    public LiveData<List<AdminUser>> getPendingUsers() {
        return pendingUsers;
    }

    public LiveData<List<AdminUser>> getAllUsers() {
        return allUsers;
    }

    public LiveData<SystemAnalytics> getSystemAnalytics() {
        return systemAnalytics;
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