package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.PatientAdapter;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import rw.ac.auca.finalprojectgroupfa.viewmodels.PatientViewModel;

public class PatientListActivity extends AppCompatActivity implements PatientAdapter.OnPatientClickListener {
    private PatientViewModel patientViewModel;
    private RecyclerView patientsRecyclerView;
    private PatientAdapter patientAdapter;
    private androidx.appcompat.widget.SearchView patientSearchView;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Patients");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupViewModel();
        setupRecyclerView();
    }

    private void initializeViews() {
        patientsRecyclerView = findViewById(R.id.patientsRecyclerView);
        patientSearchView = findViewById(R.id.patientSearchView);
        emptyStateText = findViewById(R.id.emptyStateText);
        loadingProgress = findViewById(R.id.loadingProgress);

        com.google.android.material.floatingactionbutton.FloatingActionButton addPatientFab = findViewById(
                R.id.addPatientFab);
        addPatientFab.setOnClickListener(v -> {
            Intent intent = new Intent(PatientListActivity.this, AddPatientActivity.class);
            startActivity(intent);
        });

        // Setup search functionality
        patientSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                patientViewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    private void setupViewModel() {
        patientViewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        // Observe all patients
        // Observe filtered patients
        patientViewModel.getFilteredPatients().observe(this, patients -> {
            if (patients != null && !patients.isEmpty()) {
                patientAdapter.setPatients(patients);
                emptyStateText.setVisibility(View.GONE);
                patientsRecyclerView.setVisibility(View.VISIBLE);
            } else {
                // Only show empty state if we really have no patients matching
                patientAdapter.setPatients(new java.util.ArrayList<>());
                emptyStateText.setVisibility(View.VISIBLE);
                // Don't hide recycler view completely, just show empty
                // patientsRecyclerView.setVisibility(View.GONE);
            }
        });

        patientViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void setupRecyclerView() {
        patientAdapter = new PatientAdapter(null, this);
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientsRecyclerView.setAdapter(patientAdapter);
    }

    // Implement the listener methods
    @Override
    public void onPatientClick(Patient patient) {
        // Show vitals details
        Intent intent = new Intent(this, PatientDetailActivity.class);
        intent.putExtra("PATIENT", patient);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}