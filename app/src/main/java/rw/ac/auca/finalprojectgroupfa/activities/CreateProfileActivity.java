package rw.ac.auca.finalprojectgroupfa.activities;

import com.google.firebase.analytics.FirebaseAnalytics;
import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import rw.ac.auca.finalprojectgroupfa.R;

public class CreateProfileActivity extends AppCompatActivity {
    private EditText fullNameEditText;
    private RadioGroup roleRadioGroup;
    private Button saveProfileButton;
    private ProgressBar createProfileProgress;

    private FirebaseFirestore firestore;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // If for some reason the user is null, they can't create a profile.
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            // Send back to Login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        fullNameEditText = findViewById(R.id.fullNameEditText);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        createProfileProgress = findViewById(R.id.createProfileProgress);

        saveProfileButton.setOnClickListener(v -> saveUserProfile());
    }

    private void saveUserProfile() {
        String fullName = fullNameEditText.getText().toString().trim();
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();

        if (fullName.isEmpty()) {
            fullNameEditText.setError("Full name is required");
            fullNameEditText.requestFocus();
            return;
        }

        setLoading(true);

        RadioButton selectedRadioButton = findViewById(selectedRoleId);
        String role;
        if (selectedRadioButton.getId() == R.id.radioDoctor) { // Corrected from doctorRadioButton
            role = "doctor";
        } else {
            role = "chw";
        }

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("uid", currentUser.getUid());
        newUser.put("name", fullName);
        newUser.put("role", role);
        newUser.put("phoneNumber", currentUser.getPhoneNumber());
        newUser.put("status", "pending");
        newUser.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("users").document(currentUser.getUid()).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Profile created. Waiting for admin approval.", Toast.LENGTH_LONG).show();
                    // --- NEW: Log the sign_up event ---
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.METHOD, "phone_otp");
                    bundle.putString("user_role", role); // Custom parameter
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
                    // --- END OF NEW CODE ---

                    // Keep the user signed in and show a "pending approval" screen.
                    Intent intent = new Intent(this, PendingApprovalActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard(String role) {
        Class<?> destinationActivity = "doctor".equals(role) ? DoctorDashboardActivity.class : ChwDashboardActivity.class;
        Intent intent = new Intent(this, destinationActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        createProfileProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveProfileButton.setEnabled(!isLoading);
        fullNameEditText.setEnabled(!isLoading);
        for (int i = 0; i < roleRadioGroup.getChildCount(); i++) {
            roleRadioGroup.getChildAt(i).setEnabled(!isLoading);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

