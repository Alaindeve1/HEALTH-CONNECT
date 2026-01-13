package rw.ac.auca.finalprojectgroupfa.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.ObservationDraft;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.utils.NetworkUtils;
import rw.ac.auca.finalprojectgroupfa.viewmodels.VitalsViewModel;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class VitalsActivity extends AppCompatActivity {
    private VitalsViewModel vitalsViewModel;
    private Patient currentPatient;
    private ObservationDraft editingObservation;
    private boolean isEditMode = false;

    private EditText temperatureInput, systolicBPInput, diastolicBPInput;
    private EditText heartRateInput, respiratoryRateInput, symptomsInput;
    private Button submitButton;
    private ProgressBar loadingProgress;
    private TextView patientInfoText, connectionStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Record Vitals");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get patient data from intent
        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
        if (getIntent().hasExtra("OBSERVATION")) {
            editingObservation = (ObservationDraft) getIntent().getSerializableExtra("OBSERVATION");
            if (editingObservation != null) {
                isEditMode = true;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Edit Vitals");
                }
            }
        }

        if (currentPatient == null) {
            Toast.makeText(this, "Patient data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupViewModel();
        updatePatientInfo();

        if (isEditMode) {
            prefillData();
        }

        if (currentPatient.getFhirPatientId() == null || currentPatient.getFhirPatientId().isEmpty()) {
            if (!isEditMode) { // Allow editing local-only vitals even if patient not synced? Maybe. But submit
                               // warns.
                // Actually relying on isOnline check in viewmodel allows offline save.
            }
        }
    }

    private void initializeViews() {
        temperatureInput = findViewById(R.id.temperatureInput);
        systolicBPInput = findViewById(R.id.systolicBPInput);
        diastolicBPInput = findViewById(R.id.diastolicBPInput);
        heartRateInput = findViewById(R.id.heartRateInput);
        respiratoryRateInput = findViewById(R.id.respiratoryRateInput);
        symptomsInput = findViewById(R.id.symptomsInput);
        submitButton = findViewById(R.id.submitVitalsButton);
        loadingProgress = findViewById(R.id.loadingProgress);
        patientInfoText = findViewById(R.id.patientInfoText);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        submitButton.setOnClickListener(v -> submitVitals());

        if (isEditMode) {
            submitButton.setText("Update Vitals");
        }

        // Setup input validation
        setupInputValidation();
    }

    private void prefillData() {
        temperatureInput.setText(String.valueOf(editingObservation.getTemperature()));
        systolicBPInput.setText(String.valueOf(editingObservation.getSystolicBP()));
        diastolicBPInput.setText(String.valueOf(editingObservation.getDiastolicBP()));
        heartRateInput.setText(String.valueOf(editingObservation.getHeartRate()));
        respiratoryRateInput.setText(String.valueOf(editingObservation.getRespiratoryRate()));
        if (editingObservation.getSymptoms() != null) {
            symptomsInput.setText(editingObservation.getSymptoms());
        }
    }

    private void setupViewModel() {
        vitalsViewModel = new ViewModelProvider(this).get(VitalsViewModel.class);

        vitalsViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            submitButton.setEnabled(!isLoading);
        });

        vitalsViewModel.getOperationStatus().observe(this, status -> {
            if (status != null) {
                if (status.contains("successfully") || status.contains("saved offline") || status.contains("updated")) {
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                    finish(); // Close activity on success
                } else if (status.contains("Error") || status.contains("must be")) {
                    // Show validation errors
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Validation Error")
                            .setMessage(status)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                }
                vitalsViewModel.clearStatus();
            }
        });

        vitalsViewModel.getIsOnline().observe(this, isOnline -> {
            if (isOnline) {
                connectionStatusText.setText("Online - Submitting directly to server");
                connectionStatusText.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                connectionStatusText.setText("Offline - Saving locally for later sync");
                connectionStatusText.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
        });

        // Check network status
        checkNetworkStatus();
    }

    private void updatePatientInfo() {
        if (currentPatient != null) {
            String prefix = isEditMode ? "Editing vitals for:\n" : "Recording vitals for:\n";
            String patientInfo = String.format(Locale.getDefault(), "%s%s\n%d years • %s • %s",
                    prefix,
                    currentPatient.getName(),
                    currentPatient.getAge(),
                    currentPatient.getGender(),
                    currentPatient.getVillage());
            patientInfoText.setText(patientInfo);
        }
    }

    private void setupInputValidation() {
        // Add text watchers for real-time validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };

        temperatureInput.addTextChangedListener(validationWatcher);
        systolicBPInput.addTextChangedListener(validationWatcher);
        diastolicBPInput.addTextChangedListener(validationWatcher);
        heartRateInput.addTextChangedListener(validationWatcher);
        respiratoryRateInput.addTextChangedListener(validationWatcher);
    }

    private void validateInputs() {
        try {
            // Simplified validation check just to enable button
            String tempStr = temperatureInput.getText().toString();
            String sysStr = systolicBPInput.getText().toString();
            String diaStr = diastolicBPInput.getText().toString();
            String hrStr = heartRateInput.getText().toString();

            boolean hasRequiredData = !tempStr.isEmpty() && !sysStr.isEmpty() && !diaStr.isEmpty() && !hrStr.isEmpty();
            submitButton.setEnabled(hasRequiredData);

        } catch (NumberFormatException e) {
            submitButton.setEnabled(false);
        }
    }

    private void checkNetworkStatus() {
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);
        vitalsViewModel.setOnlineStatus(isOnline);
    }

    private void submitVitals() {
        try {
            // Get input values
            double temperature = Double.parseDouble(temperatureInput.getText().toString());
            int systolicBP = Integer.parseInt(systolicBPInput.getText().toString());
            int diastolicBP = Integer.parseInt(diastolicBPInput.getText().toString());
            int heartRate = Integer.parseInt(heartRateInput.getText().toString());
            int respiratoryRate = respiratoryRateInput.getText().toString().isEmpty() ? 0
                    : Integer.parseInt(respiratoryRateInput.getText().toString());
            String symptoms = symptomsInput.getText().toString().trim();

            // Validate vitals
            vitalsViewModel.validateVitals(temperature, systolicBP, diastolicBP, heartRate, respiratoryRate);

            // If no validation errors, proceed with submission
            if (vitalsViewModel.getOperationStatus().getValue() == null) {

                ObservationDraft observation;
                if (isEditMode) {
                    observation = editingObservation;
                } else {
                    observation = new ObservationDraft();
                    observation.setPatientId(currentPatient.getId());
                    observation.setPatientLocalUUID(currentPatient.getLocalUUID());
                }

                observation.setTemperature(temperature);
                observation.setSystolicBP(systolicBP);
                observation.setDiastolicBP(diastolicBP);
                observation.setHeartRate(heartRate);
                observation.setRespiratoryRate(respiratoryRate);
                observation.setSymptoms(symptoms);

                // Get online status
                boolean isOnline = Boolean.TRUE.equals(vitalsViewModel.getIsOnline().getValue());

                // Submit vitals
                if (isEditMode) {
                    vitalsViewModel.updateVitals(observation, currentPatient, isOnline);
                } else {
                    vitalsViewModel.submitVitals(observation, currentPatient, isOnline);
                }
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for all required fields", Toast.LENGTH_SHORT).show();
        }
    }

    // Simple TextWatcher implementation
    private abstract class TextWatcher implements android.text.TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}