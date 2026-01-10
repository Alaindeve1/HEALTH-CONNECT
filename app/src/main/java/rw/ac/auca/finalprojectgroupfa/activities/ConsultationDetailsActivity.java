package rw.ac.auca.finalprojectgroupfa.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;

public class ConsultationDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_details);

        ConsultationRequest request = (ConsultationRequest) getIntent().getSerializableExtra("CONSULTATION_REQUEST");
        if (request == null) {
            Toast.makeText(this, "Consultation data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView patientName = findViewById(R.id.patientName);
        TextView reason = findViewById(R.id.reason);
        TextView status = findViewById(R.id.status);
        TextView date = findViewById(R.id.date);
        Button closeButton = findViewById(R.id.closeButton);

        patientName.setText("Patient: " + request.getPatientName());
        reason.setText("Reason: " + request.getReason());
        status.setText(request.getStatus() != null ? request.getStatus().toUpperCase() : "PENDING");

        if (request.getRequestedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            date.setText("Date: " + dateFormat.format(request.getRequestedAt()));
        }

        closeButton.setOnClickListener(v -> finish());
    }
}
