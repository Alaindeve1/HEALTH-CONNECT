package rw.ac.auca.finalprojectgroupfa.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Date; // Use java.util.Date
import java.util.List;

import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.models.Message; // Use the correct Message model

public class ChatViewModel extends AndroidViewModel {

    private FirebaseFirestore firestore;
    private ListenerRegistration messagesListener;

    // Use the correct Message model for the LiveData
    private MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> operationStatus = new MutableLiveData<>();

    private ConsultationRequest currentConsultation;
    private String currentUserId;
    private String currentUserName;
    private String currentUserRole;

    public ChatViewModel(Application application) {
        super(application);
        firestore = FirebaseFirestore.getInstance();
        isLoading.setValue(false);
    }

    public void initializeChat(ConsultationRequest consultation, String userId) {
        this.currentConsultation = consultation;
        this.currentUserId = userId; // Store the user ID

        // Fetch the user's data from Firestore
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        this.currentUserName = documentSnapshot.getString("name");
                        this.currentUserRole = documentSnapshot.getString("role");
                        // Now that we have the user data, we can start listening for messages
                        listenForMessages();
                    } else {
                        operationStatus.setValue("Error: User profile not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Error fetching user data: " + e.getMessage());
                });
    }

    private void listenForMessages() {
        if (currentConsultation == null || currentConsultation.getRequestId() == null
                || currentConsultation.getRequestId().isEmpty()) {
            operationStatus.setValue("Error: Invalid consultation request ID");
            return;
        }

        isLoading.setValue(true);
        Query query = firestore.collection("consultations")
                .document(currentConsultation.getRequestId())
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        // Make sure to remove any previous listener before attaching a new one
        cleanup();

        messagesListener = query.addSnapshotListener((snapshots, e) -> {
            isLoading.setValue(false);
            if (e != null) {
                operationStatus.setValue("Error loading messages: " + e.getMessage());
                return;
            }

            if (snapshots != null) {
                // Use the correct Message class here
                List<Message> messageList = snapshots.toObjects(Message.class);
                messages.setValue(messageList);
            }
        });
    }

    public void sendMessage(String text) {
        // This is the single, correct implementation for sending a message
        if (text.trim().isEmpty() || currentConsultation == null || currentUserId == null) {
            return;
        }
        sendMessageInternal(text);
    }

    private void sendMessageInternal(String text) {
        if (currentConsultation == null || currentUserId == null)
            return;

        // Validate requestId before sending
        if (currentConsultation.getRequestId() == null || currentConsultation.getRequestId().isEmpty()) {
            operationStatus.setValue("Error: Invalid consultation request ID");
            return;
        }

        // Create a new Message object
        Message message = new Message(
                text.trim(),
                currentUserId, // Use the real user ID
                currentUserName, // Use the fetched user name
                currentUserRole, // Use the fetched user role
                new Date() // Use java.util.Date for the timestamp
        );
        // message.setType(type); // Removed
        // message.setAttachmentUrl(attachmentUrl); // Removed

        // Add the message to the "messages" subcollection in Firestore
        firestore.collection("consultations")
                .document(currentConsultation.getRequestId())
                .collection("messages")
                .add(message)
                .addOnFailureListener(e -> {
                    operationStatus.setValue("Failed to send message: " + e.getMessage());
                });
    }

    // Call this when the ViewModel is no longer needed to prevent memory leaks
    public void cleanup() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    // Getters for LiveData
    public LiveData<List<Message>> getMessages() {
        return messages;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanup(); // Ensure listener is removed when ViewModel is destroyed
    }
}
