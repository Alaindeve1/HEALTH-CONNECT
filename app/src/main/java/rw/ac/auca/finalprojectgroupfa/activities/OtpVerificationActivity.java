package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import rw.ac.auca.finalprojectgroupfa.workers.PatientOwnershipSyncWorker;

import java.util.HashMap;
import java.util.Map;

import rw.ac.auca.finalprojectgroupfa.R;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText otpEditText;
    private Button verifyButton;
    private ProgressBar verificationProgress;
    private TextView resendOtpText;

    private String verificationId;
    private String phoneNumber;
    private FirebaseAnalytics mFirebaseAnalytics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Get the verificationId and phoneNumber from the Intent
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (verificationId == null || phoneNumber == null) {
            // If these are missing, we can't proceed. Go back to Login.
            Toast.makeText(this, "Verification session expired. Please try again.", Toast.LENGTH_LONG).show();
            navigateTo(LoginActivity.class);
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        otpEditText = findViewById(R.id.otpEditText);
        verifyButton = findViewById(R.id.verifyButton);
        verificationProgress = findViewById(R.id.verificationProgress);
        resendOtpText = findViewById(R.id.resendOtpText);

        verifyButton.setOnClickListener(v -> verifyOtp());

        // We will leave resend OTP for now to keep it simple.
        resendOtpText.setOnClickListener(v -> {
            Toast.makeText(this, "Resend OTP functionality not implemented yet.", Toast.LENGTH_SHORT).show();
        });
    }

    private void verifyOtp() {
        String otpCode = otpEditText.getText().toString().trim();

        if (otpCode.length() != 6) {
            Toast.makeText(this, "Please enter the 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Use the verificationId from the intent to create the credential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpCode);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        // OTP Verification Successful!
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            // Now, check the user's role and navigate to the correct dashboard.
                            checkUserRoleAndNavigate(user.getUid());
                        }
                    } else {
                        // Verification failed
                        Toast.makeText(OtpVerificationActivity.this, "OTP Verification Failed. Please check the code and try again.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate(String userId) {
        setLoading(true);
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        // User profile EXISTS. Log them in.
                        String role = documentSnapshot.getString("role");
                        String status = documentSnapshot.getString("status");
                        if (status == null) status = "pending";

                        if (!"approved".equalsIgnoreCase(status)) {
                            // Don't sign out; route to a waiting screen.
                            navigateTo(PendingApprovalActivity.class);
                            return;
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.METHOD, "phone_otp");
                        bundle.putString("user_role", role); // Custom parameter
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

                        enqueuePatientOwnershipSync(userId);

                        if ("doctor".equals(role)) {
                            navigateTo(DoctorDashboardActivity.class);
                        } else if ("admin".equals(role)) {
                            navigateTo(AdminPanelActivity.class);
                        } else {
                            navigateTo(ChwDashboardActivity.class);
                        }
                    } else {
                        // User profile DOES NOT EXIST. This is a new user.
                        // Send them to the CreateProfileActivity.
                        Toast.makeText(this, "Welcome! Please create your profile.", Toast.LENGTH_SHORT).show();
                        navigateTo(CreateProfileActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to check user profile. Please try again.", Toast.LENGTH_SHORT).show();
                    navigateTo(LoginActivity.class); // On failure, go back to login
                });
    }



    private void enqueuePatientOwnershipSync(String userId) {
        try {
            if (userId == null || userId.isEmpty()) return;

            Data input = new Data.Builder()
                    .putString(PatientOwnershipSyncWorker.KEY_USER_ID, userId)
                    .build();

            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(PatientOwnershipSyncWorker.class)
                    .setInputData(input)
                    .build();

            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("patient_ownership_sync_" + userId, androidx.work.ExistingWorkPolicy.KEEP, work);
        } catch (Exception ignored) {
        }
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(OtpVerificationActivity.this, activityClass);
        // Don't clear the task when going to CreateProfileActivity, so the user can't go "back" to the OTP screen.
        // But DO clear it when going to a dashboard.
        if (activityClass == DoctorDashboardActivity.class
                || activityClass == ChwDashboardActivity.class
                || activityClass == AdminPanelActivity.class
                || activityClass == PendingApprovalActivity.class
                || activityClass == LoginActivity.class) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        verificationProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!isLoading);
        otpEditText.setEnabled(!isLoading);
    }
}
