package rw.ac.auca.finalprojectgroupfa.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.database.AppDatabase;
import rw.ac.auca.finalprojectgroupfa.database.PatientDao;
import rw.ac.auca.finalprojectgroupfa.database.SyncItemDao;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.models.SyncItem;
import rw.ac.auca.finalprojectgroupfa.models.fhir.FhirPatient;
import rw.ac.auca.finalprojectgroupfa.viewmodels.PatientViewModel;
import rw.ac.auca.finalprojectgroupfa.workers.SyncWorker;

public class AddPatientActivity extends AppCompatActivity {
    private PatientViewModel patientViewModel;
    private EditText nameEditText, ageEditText, villageEditText, contactEditText;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadio, femaleRadio, otherRadio;
    private Button saveButton;
    private ProgressBar loadingProgress;
    private Gson gson = new Gson();

    private Patient editingPatient;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupViewModel();

        if (getIntent().hasExtra("PATIENT")) {
            editingPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
            if (editingPatient != null) {
                isEditMode = true;
                setupEditMode();
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Patient" : "Add New Patient");
        }
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        ageEditText = findViewById(R.id.ageEditText);
        villageEditText = findViewById(R.id.villageEditText);
        contactEditText = findViewById(R.id.contactEditText);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        maleRadio = findViewById(R.id.maleRadio);
        femaleRadio = findViewById(R.id.femaleRadio);
        otherRadio = findViewById(R.id.otherRadio);
        saveButton = findViewById(R.id.savePatientButton);
        loadingProgress = findViewById(R.id.loadingProgress);

        saveButton.setOnClickListener(v -> savePatient());
    }

    private void setupEditMode() {
        nameEditText.setText(editingPatient.getName());
        ageEditText.setText(String.valueOf(editingPatient.getAge()));
        villageEditText.setText(editingPatient.getVillage());
        contactEditText.setText(editingPatient.getContactNumber());
        saveButton.setText("Update Patient");

        String gender = editingPatient.getGender();
        if ("Male".equalsIgnoreCase(gender)) {
            maleRadio.setChecked(true);
        } else if ("Female".equalsIgnoreCase(gender)) {
            femaleRadio.setChecked(true);
        } else {
            otherRadio.setChecked(true);
        }
    }

    private void setupViewModel() {
        patientViewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        patientViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isLoading);
        });

        // Note: We are using custom saving logic below to ensure SyncItems are created,
        // so we might not use the ViewModel's operationStatus for the save action
        // itself,
        // but we keep it observing just in case.
        patientViewModel.getOperationStatus().observe(this, status -> {
            // Optional: Handle ViewModel status if we switch to using it
        });
    }

    private void savePatient() {
        String name = nameEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();
        String contact = contactEditText.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Name and Age are required", Toast.LENGTH_SHORT).show();
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedId);
        String gender = selectedRadioButton.getText().toString();

        loadingProgress.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        // Prepare the object
        final Patient patientToSave;
        if (isEditMode) {
            patientToSave = editingPatient;
            // Keep existing ID and UUID
            patientToSave.setSynced(false); // Mark as unsynced so it uploads again
        } else {
            patientToSave = new Patient();
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId != null) {
                patientToSave.setCreatedByUserId(userId);
            }
        }

        patientToSave.setName(name);
        patientToSave.setAge(age);
        patientToSave.setGender(gender);
        patientToSave.setVillage(village);
        patientToSave.setContactNumber(contact);

        // database operations on background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            PatientDao patientDao = AppDatabase.getDatabase(getApplicationContext()).patientDao();
            if (isEditMode) {
                patientDao.update(patientToSave);
            } else {
                patientDao.insert(patientToSave);
            }

            // Create SyncItem for both Insert and Update to ensure server gets the changes
            createSyncItemForPatient(patientToSave);

            // Best-effort Firestore index update
            writePatientIndexToFirestore(patientToSave);

            runOnUiThread(() -> {
                loadingProgress.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                String msg = isEditMode ? "Patient updated." : "Patient saved locally.";
                Toast.makeText(this, msg + " Queued for sync.", Toast.LENGTH_LONG).show();
                triggerSync();
                finish();
            });
        });
    }

    private void writePatientIndexToFirestore(Patient patient) {
        try {
            if (patient == null)
                return;
            String userId = patient.getCreatedByUserId();
            if (userId == null || userId.isEmpty())
                return;
            if (patient.getLocalUUID() == null || patient.getLocalUUID().isEmpty())
                return;

            // Store a minimal patient record for cross-device discovery.
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("localUUID", patient.getLocalUUID());
            data.put("createdByUserId", userId);
            data.put("name", patient.getName());
            data.put("age", patient.getAge());
            data.put("gender", patient.getGender());
            data.put("village", patient.getVillage());
            data.put("contactNumber", patient.getContactNumber());
            data.put("createdAt", patient.getCreatedAt());
            data.put("synced", patient.isSynced());
            data.put("fhirPatientId", patient.getFhirPatientId());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("patients")
                    .document(patient.getLocalUUID())
                    .set(data);
        } catch (Exception ignored) {
        }
    }

    private void createSyncItemForPatient(Patient localPatient) {
        FhirPatient fhirPatient = new FhirPatient();
        FhirPatient.HumanName humanName = new FhirPatient.HumanName();
        humanName.given.add(localPatient.getName());

        List<FhirPatient.HumanName> nameList = new ArrayList<>();
        nameList.add(humanName);
        fhirPatient.setName(nameList);
        fhirPatient.setGender(localPatient.getGender().toLowerCase());

        // If we have a remote ID, set it so the server knows which to update
        if (localPatient.getFhirPatientId() != null) {
            fhirPatient.setId(localPatient.getFhirPatientId());
        }

        SyncItem syncItem = new SyncItem();
        syncItem.setResourceType("Patient");
        syncItem.setLocalId(localPatient.getLocalUUID());
        syncItem.setJsonData(gson.toJson(fhirPatient));

        // Insert SyncItem
        SyncItemDao dao = AppDatabase.getDatabase(getApplicationContext()).syncItemDao();
        dao.insert(syncItem);
    }

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
