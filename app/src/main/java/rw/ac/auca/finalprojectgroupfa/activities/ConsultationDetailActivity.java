package rw.ac.auca.finalprojectgroupfa.activities;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.ChatAdapter;
import rw.ac.auca.finalprojectgroupfa.database.AppDatabase;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.repositories.ConsultationRepository;
import rw.ac.auca.finalprojectgroupfa.viewmodels.ChatViewModel;
import rw.ac.auca.finalprojectgroupfa.viewmodels.DoctorDashboardViewModel;

public class ConsultationDetailActivity extends AppCompatActivity {
    private ChatViewModel chatViewModel;
    private ChatAdapter chatAdapter;
    private ConsultationRequest currentConsultation;
    private Patient currentPatient;
    private DoctorDashboardViewModel doctorDashboardViewModel;

    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private FloatingActionButton sendButton;
    private TextView patientNameText;
    private TextView consultationStatusText;
    private com.google.android.material.chip.Chip priorityChip;
    private ProgressBar loadingProgress;

    private TextView emptyChatText;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_detail);

        // Get consultation data from intent
        currentConsultation = (ConsultationRequest) getIntent().getSerializableExtra("CONSULTATION_REQUEST");
        if (currentConsultation == null) {
            Toast.makeText(this, "Consultation data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validate that the consultation has a requestId (required for chat)
        if (currentConsultation.getRequestId() == null || currentConsultation.getRequestId().isEmpty()) {
            Toast.makeText(this, "Invalid consultation: missing request ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        loadPatientData();
        setupViewModel();
        setupRecyclerView();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String patientName = currentConsultation.getPatientName() != null ? currentConsultation.getPatientName()
                    : "Consultation";
            getSupportActionBar().setTitle(patientName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadPatientData() {
        // Load patient data from database using patientLocalUUID
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String patientLocalUUID = currentConsultation.getPatientLocalUUID();
                if (patientLocalUUID != null && !patientLocalUUID.isEmpty()) {
                    Patient patient = AppDatabase.getDatabase(getApplicationContext())
                            .patientDao()
                            .getPatientByLocalUUID(patientLocalUUID);
                    if (patient != null) {
                        runOnUiThread(() -> {
                            currentPatient = patient;
                        });
                    }
                }
            } catch (Exception e) {
                // Patient lookup failed, but we can still use the consultation
                e.printStackTrace();
            }
        });
    }

    private void initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        loadingProgress = findViewById(R.id.loadingProgress);
        emptyChatText = findViewById(R.id.emptyChatText);

        patientNameText = findViewById(R.id.patientNameText);
        consultationStatusText = findViewById(R.id.consultationStatusText);
        priorityChip = findViewById(R.id.priorityChip);

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Enable/disable send button based on input
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(!s.toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Send on enter key
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void setPriorityColor(String priority) {
        int colorRes;
        switch (priority.toLowerCase()) {
            case "emergency":
            case "stat":
                colorRes = android.R.color.holo_red_dark;
                break;
            case "urgent":
            case "asap":
                colorRes = android.R.color.holo_orange_dark;
                break;
            case "routine":
            default:
                colorRes = android.R.color.darker_gray; // Fallback
                break;
        }
        // chipBackgroundColor expects a ColorStateList, but for simplicity here we
        // assume standard colored chips style or
        // we can set chipBackgroundColor programmatically if needed.
        // For now, let's just leave the default color or set text color.
        // The XML set it to ?attr/colorPrimary.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_video_call) {
            startVideoCall();
            return true;
        } else if (itemId == R.id.action_view_patient_file) {
            viewPatientFile();
            return true;
        } else if (itemId == R.id.action_end_consultation) {
            endConsultation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startVideoCall() {
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra("CONSULTATION_REQUEST", currentConsultation);
        startActivity(intent);
    }

    private void viewPatientFile() {
        if (currentPatient == null) {
            // Try to load patient synchronously if not already loaded
            String patientLocalUUID = currentConsultation.getPatientLocalUUID();
            if (patientLocalUUID != null && !patientLocalUUID.isEmpty()) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        Patient patient = AppDatabase.getDatabase(getApplicationContext())
                                .patientDao()
                                .getPatientByLocalUUID(patientLocalUUID);
                        if (patient != null) {
                            runOnUiThread(() -> {
                                navigateToPatientDetails(patient);
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Patient information not found.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error loading patient information.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Patient information not available.", Toast.LENGTH_SHORT).show();
            }
        } else {
            navigateToPatientDetails(currentPatient);
        }
    }

    private void navigateToPatientDetails(Patient patient) {
        Intent intent = new Intent(this, PatientDetailActivity.class);
        intent.putExtra("PATIENT", patient);
        startActivity(intent);
    }

    private void endConsultation() {
        new AlertDialog.Builder(this)
                .setTitle("End Consultation")
                .setMessage("Are you sure you want to end this consultation? This action cannot be undone.")
                .setPositiveButton("End", (dialog, which) -> {
                    // Update consultation status to completed
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            currentConsultation.setStatus("completed");
                            currentConsultation.setCompletedAt(new java.util.Date());

                            ConsultationRepository repository = new ConsultationRepository(
                                    (Application) getApplicationContext());
                            repository.update(currentConsultation);

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Consultation ended successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error ending consultation: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // This will finish the current activity and take the user back to the previous
        // one (the dashboard)
        finish();
        return true;
    }

    private void setupViewModel() {
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = firebaseUser.getUid();
        chatViewModel.initializeChat(currentConsultation, currentUserId);

        // Observe messages from the ViewModel
        // The LiveData now correctly contains a List<Message>
        chatViewModel.getMessages().observe(this, messages -> {
            if (messages != null && !messages.isEmpty()) {
                chatAdapter.setMessages(messages);
                emptyChatText.setVisibility(View.GONE);
                messagesRecyclerView.setVisibility(View.VISIBLE);
                scrollToBottom();
            } else {
                emptyChatText.setVisibility(View.VISIBLE);
                messagesRecyclerView.setVisibility(View.GONE);
            }
        });

        // Observe loading state
        chatViewModel.getIsLoading().observe(this, isLoading -> {
            // You can update a progress bar here if you want
            // loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe operation status
        chatViewModel.getOperationStatus().observe(this, status -> {
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                chatViewModel.clearStatus();
            }
        });
    }

    private void setupRecyclerView() {
        // Get the real current user ID from Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // Safeguard, should already be handled in setupViewModel
            return;
        }
        String currentUserId = firebaseUser.getUid();

        // Pass the real user ID to the adapter so it can correctly distinguish
        // between "sent" and "received" messages.
        chatAdapter = new ChatAdapter(null, currentUserId);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(chatAdapter);

        // Scroll to bottom when new messages arrive
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                scrollToBottom();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            chatViewModel.sendMessage(messageText);
            messageInput.setText("");
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            messagesRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatViewModel != null) {
            chatViewModel.cleanup();
        }
    }
}