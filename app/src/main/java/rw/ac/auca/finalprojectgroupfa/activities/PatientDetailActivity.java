package rw.ac.auca.finalprojectgroupfa.activities;

import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.view.MenuItem;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.ChwConsultationAdapter;
import rw.ac.auca.finalprojectgroupfa.adapters.VitalsHistoryAdapter;
import rw.ac.auca.finalprojectgroupfa.models.ObservationDraft;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.viewmodels.PatientDetailViewModel;
import rw.ac.auca.finalprojectgroupfa.viewmodels.PatientViewModel;

public class PatientDetailActivity extends AppCompatActivity
        implements VitalsHistoryAdapter.OnVitalsClickListener, ChwConsultationAdapter.OnChwConsultationListener {

    private PatientDetailViewModel viewModel;
    private PatientViewModel patientViewModel; // For delete operation
    private Patient currentPatient;

    private TextView patientName, patientDemographics, patientContact;
    private RecyclerView vitalsRecyclerView, consultationsRecyclerView;
    private VitalsHistoryAdapter vitalsAdapter;
    private ChwConsultationAdapter consultationAdapter;
    private TextView noVitalsText, noConsultationsText;
    private FloatingActionButton fab;
    private String userRole = "chw"; // Default to CHW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Patient Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
        if (currentPatient == null) {
            Toast.makeText(this, "Patient data not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupViewModel();
        setupRecyclerViews();

        checkUserRole();
    }

    private void checkUserRole() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if (role != null) {
                                this.userRole = role;
                                updateUIForRole();
                            }
                        }
                    });
        }
    }

    private void updateUIForRole() {
        if ("doctor".equalsIgnoreCase(userRole)) {
            if (fab != null)
                fab.setVisibility(View.GONE);
        } else {
            if (fab != null)
                fab.setVisibility(View.VISIBLE);
        }

        // Re-initialize adapter with correct role
        if (consultationAdapter != null) {
            consultationAdapter = new ChwConsultationAdapter(consultationAdapter.getConsultations(), this, userRole);
            consultationsRecyclerView.setAdapter(consultationAdapter);
        }
    }

    private void initializeViews() {
        patientName = findViewById(R.id.patientName);
        patientDemographics = findViewById(R.id.patientDemographics);
        patientContact = findViewById(R.id.patientContact);
        vitalsRecyclerView = findViewById(R.id.vitalsRecyclerView);
        consultationsRecyclerView = findViewById(R.id.consultationsRecyclerView);
        noVitalsText = findViewById(R.id.noVitalsText);
        noConsultationsText = findViewById(R.id.noConsultationsText);
        fab = findViewById(R.id.fab);

        updatePatientInfoUI();

        fab.setOnClickListener(view -> showFabMenu(view));
    }

    private void updatePatientInfoUI() {
        if (currentPatient != null) {
            patientName.setText(currentPatient.getName());
            patientDemographics.setText(String.format("%d years • %s • %s",
                    currentPatient.getAge(), currentPatient.getGender(), currentPatient.getVillage()));
            patientContact.setText("Contact: " + currentPatient.getContactNumber());
        }
    }

    private void showFabMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.patient_detail_fab_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_vitals) {
                navigateToAddVitals();
                return true;
            } else if (itemId == R.id.action_request_consultation) {
                navigateToRequestConsultation();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void navigateToAddVitals() {
        Intent intent = new Intent(this, VitalsActivity.class);
        intent.putExtra("PATIENT", currentPatient);
        startActivity(intent);
    }

    private void navigateToRequestConsultation() {
        Intent intent = new Intent(this, ConsultationRequestActivity.class);
        intent.putExtra("PATIENT", currentPatient);
        startActivity(intent);
    }

    // ... (updatePatientInfoUI and showFabMenu remain same)

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PatientDetailViewModel.class);
        patientViewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        // Load data initially
        refreshData();

        // Observe Vitals
        viewModel.getVitals().observe(this, vitals -> {
            if (vitals != null && !vitals.isEmpty()) {
                vitalsAdapter.setVitals(vitals);
                vitalsRecyclerView.setVisibility(View.VISIBLE);
                noVitalsText.setVisibility(View.GONE);
            } else {
                vitalsRecyclerView.setVisibility(View.GONE);
                noVitalsText.setVisibility(View.VISIBLE);
            }
        });

        // Observe Consultations
        viewModel.getConsultations().observe(this, consultations -> {
            if (consultations != null && !consultations.isEmpty()) {
                consultationAdapter.setConsultations(consultations);
                consultationsRecyclerView.setVisibility(View.VISIBLE);
                noConsultationsText.setVisibility(View.GONE);
            } else {
                consultationsRecyclerView.setVisibility(View.GONE);
                noConsultationsText.setVisibility(View.VISIBLE);
            }
        });

        // Listen for delete status
        patientViewModel.getOperationStatus().observe(this, status -> {
            if (status != null && status.contains("deleted")) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        if (currentPatient != null) {
            viewModel.loadPatientData(currentPatient.getId(), currentPatient.getLocalUUID());
        }
    }

    // ...

    private void setupRecyclerViews() {
        // Vitals RecyclerView
        vitalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vitalsAdapter = new VitalsHistoryAdapter(null, this);
        vitalsRecyclerView.setAdapter(vitalsAdapter);

        // Consultations RecyclerView
        consultationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize with default role, will be updated in updateUIForRole
        consultationAdapter = new ChwConsultationAdapter(null, this, userRole);
        consultationsRecyclerView.setAdapter(consultationAdapter);
    }

    @Override
    public void onStartChat(rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest request) {
        Intent intent = new Intent(this, ConsultationDetailActivity.class);
        intent.putExtra("CONSULTATION_REQUEST", request);
        startActivity(intent);
    }

    @Override
    public void onViewDetails(rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest request) {
        rw.ac.auca.finalprojectgroupfa.dialogs.ConsultationDetailDialogFragment dialog = rw.ac.auca.finalprojectgroupfa.dialogs.ConsultationDetailDialogFragment
                .newInstance(request);
        dialog.show(getSupportFragmentManager(), "ConsultationDetailDialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.patient_detail_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if ("doctor".equalsIgnoreCase(userRole)) {
            menu.findItem(R.id.action_edit_patient).setVisible(false);
            menu.findItem(R.id.action_delete_patient).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_edit_patient) {
            editPatient();
            return true;
        }
        if (item.getItemId() == R.id.action_delete_patient) {
            confirmDeletePatient();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editPatient() {
        Intent intent = new Intent(this, AddPatientActivity.class);
        intent.putExtra("PATIENT", currentPatient);
        startActivity(intent);
        // Note: We should probably reload currentPatient in onResume or observe it
    }

    private void confirmDeletePatient() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Patient")
                .setMessage("Are you sure you want to delete this patient? This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deletePatient())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePatient() {
        // Delete locally
        patientViewModel.delete(currentPatient);

        // Best-effort remote delete (simplified)
        if (currentPatient.getLocalUUID() != null) {
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("patients")
                        .document(currentPatient.getLocalUUID())
                        .delete();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onVitalClick(ObservationDraft observation) {
        Intent intent = new Intent(this, VitalsDetailActivity.class);
        intent.putExtra("OBSERVATION", observation);
        intent.putExtra("PATIENT", currentPatient);
        startActivity(intent);
    }
}
