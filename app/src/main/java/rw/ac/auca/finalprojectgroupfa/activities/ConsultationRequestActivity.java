package rw.ac.auca.finalprojectgroupfa.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.models.SyncItem;
import rw.ac.auca.finalprojectgroupfa.utils.LocalNotificationUtil;
import rw.ac.auca.finalprojectgroupfa.utils.NetworkUtils;
import rw.ac.auca.finalprojectgroupfa.viewmodels.ConsultationViewModel;
import rw.ac.auca.finalprojectgroupfa.workers.SyncWorker;

public class ConsultationRequestActivity extends AppCompatActivity {
    private ConsultationViewModel consultationViewModel;
    private Patient currentPatient;

    private Spinner prioritySpinner;
    private EditText reasonEditText;
    private Button submitButton;
    private ProgressBar loadingProgress;
    private TextView patientInfoText, connectionStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_request);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Request Consultation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get patient data from intent
        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
        if (currentPatient == null) {
            Toast.makeText(this, "Patient data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupViewModel();
        updatePatientInfo();
        checkNetworkStatus();

        if (currentPatient.getFhirPatientId() == null || currentPatient.getFhirPatientId().isEmpty()) {
            submitButton.setEnabled(false);
            Toast.makeText(this, "Patient has not been synced to the server yet. Please try again later.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        prioritySpinner = findViewById(R.id.prioritySpinner);
        reasonEditText = findViewById(R.id.reasonEditText);
        submitButton = findViewById(R.id.submitRequestButton);
        loadingProgress = findViewById(R.id.loadingProgress);
        patientInfoText = findViewById(R.id.patientInfoText);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        // Setup priority spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.consultation_priorities, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        submitButton.setOnClickListener(v -> submitConsultationRequest());
    }

    private void setupViewModel() {
        consultationViewModel = new ViewModelProvider(this).get(ConsultationViewModel.class);

        consultationViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            submitButton.setEnabled(!isLoading);
        });

        consultationViewModel.getOperationStatus().observe(this, status -> {
            if (status != null) {
                if (status.contains("successfully") || status.contains("saved offline")) {
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();

                    // Local confirmation notification (shippable, no backend)
                    LocalNotificationUtil.show(
                            this,
                            "Consultation request sent",
                            "Waiting for doctor response.",
                            new android.content.Intent(this, ChwConsultationListActivity.class)
                    );

                    finish(); // Close activity on success
                } else {
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                }
                consultationViewModel.clearStatus();
            }
        });
    }

    private void updatePatientInfo() {
        if (currentPatient != null) {
            String patientInfo = String.format("Request consultation for:\n%s\n%d years • %s • %s",
                    currentPatient.getName(),
                    currentPatient.getAge(),
                    currentPatient.getGender(),
                    currentPatient.getVillage());
            patientInfoText.setText(patientInfo);
        }
    }

    private void checkNetworkStatus() {
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);
        if (isOnline) {
            connectionStatusText.setText("Online - Submitting directly to server");
            connectionStatusText.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            connectionStatusText.setText("Offline - Saving locally for later sync");
            connectionStatusText.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    private void submitConsultationRequest() {
        String priority = prioritySpinner.getSelectedItem().toString();
        String reason = reasonEditText.getText().toString().trim();

        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter reason for consultation", Toast.LENGTH_SHORT).show();
            return;
        }

        String fhirPriority = mapPriorityToFhir(priority);
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);

        com.google.gson.Gson gson = new com.google.gson.Gson();
        rw.ac.auca.finalprojectgroupfa.models.fhir.FhirServiceRequest serviceRequest = new rw.ac.auca.finalprojectgroupfa.models.fhir.FhirServiceRequest();

        // --- THIS IS THE FIX ---

        // 2. Initialize the 'subject' object, then set its properties.
        // Use the actual FHIR Patient ID so the server accepts the reference.
        serviceRequest.subject = new rw.ac.auca.finalprojectgroupfa.models.fhir.CommonTypes.Reference();
        serviceRequest.subject.reference = "Patient/" + currentPatient.getFhirPatientId();
        serviceRequest.subject.display = currentPatient.getName();

        // 3. Set the reason (this part was already correct)
        rw.ac.auca.finalprojectgroupfa.models.fhir.CommonTypes.CodeableConcept reasonConcept = new rw.ac.auca.finalprojectgroupfa.models.fhir.CommonTypes.CodeableConcept();
        reasonConcept.text = reason;
        serviceRequest.reasonCode.add(reasonConcept);

        // --- END OF FIX ---

        // 4. Set the priority
        serviceRequest.priority = fhirPriority;

        // 5. Set the authoredOn date with the CORRECT ISO 8601 FORMAT
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        serviceRequest.authoredOn = sdf.format(new Date());

        // 6. Create the SyncItem to be saved locally
        SyncItem syncItem = new SyncItem();
        syncItem.setResourceType("ServiceRequest");
        syncItem.setLocalId(java.util.UUID.randomUUID().toString());
        syncItem.setJsonData(gson.toJson(serviceRequest));

        // 7. Pass the fully-formed SyncItem to the ViewModel along with patient information
        consultationViewModel.createConsultationRequest(
            syncItem, 
            isOnline, 
            currentPatient.getLocalUUID(), 
            currentPatient.getFhirPatientId()
        );

        // 8. If we are online, immediately trigger a sync so the ServiceRequest is pushed to FHIR
        if (isOnline) {
            triggerSync();
        }
    }

    private String mapPriorityToFhir(String priority) {
        switch (priority) {
            case "Emergency":
                return "stat";
            case "Urgent":
                return "urgent";
            case "High":
                return "asap";
            case "Routine":
            default:
                return "routine";
        }
    }

    /**
     * Triggers the background SyncWorker similarly to AddPatientActivity,
     * so queued ServiceRequest items are pushed to the FHIR server as soon as possible.
     */
    private void triggerSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(syncWorkRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
