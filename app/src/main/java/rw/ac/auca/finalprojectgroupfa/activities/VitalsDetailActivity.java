package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.database.AppDatabase;
import rw.ac.auca.finalprojectgroupfa.database.ObservationDraftDao;
import rw.ac.auca.finalprojectgroupfa.models.ObservationDraft;
import rw.ac.auca.finalprojectgroupfa.models.Patient;

public class VitalsDetailActivity extends AppCompatActivity {

    private ObservationDraftDao observationDao;
    private ObservationDraft observation;
    private Patient patient;

    private TextView titleText;
    private TextView dateText;
    private TextView temperatureText;
    private TextView bpText;
    private TextView heartRateText;
    private TextView respiratoryRateText;
    private TextView symptomsText;
    private TextView syncStatusText;

    private MaterialButton editButton;
    private MaterialButton deleteButton;
    private View loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Vitals Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        observationDao = AppDatabase.getDatabase(getApplicationContext()).observationDraftDao();

        observation = (ObservationDraft) getIntent().getSerializableExtra("OBSERVATION");
        patient = (Patient) getIntent().getSerializableExtra("PATIENT");

        if (observation == null) {
            finish();
            return;
        }

        bindViews();
        renderObservation(observation);

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, VitalsActivity.class);
            intent.putExtra("PATIENT", patient);
            intent.putExtra("OBSERVATION", observation);
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromDb();
    }

    private void bindViews() {
        titleText = findViewById(R.id.vitalsDetailTitle);
        dateText = findViewById(R.id.vitalsDetailDate);
        temperatureText = findViewById(R.id.vitalsDetailTemperature);
        bpText = findViewById(R.id.vitalsDetailBloodPressure);
        heartRateText = findViewById(R.id.vitalsDetailHeartRate);
        respiratoryRateText = findViewById(R.id.vitalsDetailRespiratoryRate);
        symptomsText = findViewById(R.id.vitalsDetailSymptoms);
        syncStatusText = findViewById(R.id.vitalsDetailSyncStatus);

        editButton = findViewById(R.id.editVitalsButton);
        deleteButton = findViewById(R.id.deleteVitalsButton);
        loadingProgress = findViewById(R.id.loadingProgress);

        if (patient != null && titleText != null) {
            titleText.setText("Vitals for " + patient.getName());
        }
    }

    private void renderObservation(ObservationDraft obs) {
        if (obs == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        if (obs.getRecordedAt() != null) {
            dateText.setText(sdf.format(obs.getRecordedAt()));
        }

        temperatureText.setText(String.format(Locale.getDefault(), "%.1f °C", obs.getTemperature()));
        bpText.setText(String.format(Locale.getDefault(), "%d / %d mmHg", obs.getSystolicBP(), obs.getDiastolicBP()));
        heartRateText.setText(String.format(Locale.getDefault(), "%d bpm", obs.getHeartRate()));
        respiratoryRateText.setText(String.format(Locale.getDefault(), "%d breaths/min", obs.getRespiratoryRate()));

        if (obs.getSymptoms() != null && !obs.getSymptoms().isEmpty()) {
            symptomsText.setText(obs.getSymptoms());
        } else {
            symptomsText.setText("None");
        }

        syncStatusText.setText(obs.isSynced() ? "Synced" : "Local only");
    }

    private void refreshFromDb() {
        if (observation == null || observation.getObservationId() == null) return;

        setLoading(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ObservationDraft latest = observationDao.getById(observation.getObservationId());
            runOnUiThread(() -> {
                setLoading(false);
                if (latest == null) {
                    // Deleted from another screen.
                    finish();
                    return;
                }
                observation = latest;
                renderObservation(observation);
            });
        });
    }

    private void confirmDelete() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete vitals")
                .setMessage("Delete this vitals record? This only removes it locally.")
                .setPositiveButton("Delete", (d, w) -> deleteVitals())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVitals() {
        if (observation == null) return;

        setLoading(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            observationDao.delete(observation);
            runOnUiThread(() -> {
                setLoading(false);
                finish();
            });
        });
    }

    private void setLoading(boolean isLoading) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (editButton != null) editButton.setEnabled(!isLoading);
        if (deleteButton != null) deleteButton.setEnabled(!isLoading);
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
