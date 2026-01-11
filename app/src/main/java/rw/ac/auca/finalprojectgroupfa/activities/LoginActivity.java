package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.utils.NotificationHelper;
import rw.ac.auca.finalprojectgroupfa.viewmodels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private AutoCompleteTextView roleSpinner;
    private TextInputLayout roleSpinnerLayout; // Add this

    private LinearLayout phoneAuthLayout, emailAuthLayout;
    private EditText phoneNumberEditText, emailEditText, passwordEditText;
    private Button sendOtpButton, loginButton;
    private ProgressBar loadingProgress;

    // This variable will hold the phone number to pass to the next screen
    private String phoneNumberForVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupViewModel();
//        setupRoleSpinner();
    }

    private void initializeViews() {
        // Correctly find views
        roleSpinnerLayout = findViewById(R.id.roleSpinnerLayout); // Get the layout
        roleSpinner = findViewById(R.id.roleSpinner);
        phoneAuthLayout = findViewById(R.id.phoneAuthLayout);
        emailAuthLayout = findViewById(R.id.emailAuthLayout);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        loginButton = findViewById(R.id.loginButton);
        loadingProgress = findViewById(R.id.loadingProgress);

        // --- Setup Role Spinner ---
        String[] roles = getResources().getStringArray(R.array.user_roles);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        roleSpinner.setAdapter(adapter);
        // --- Add Listener to show/hide login fields ---

        roleSpinner.setOnItemClickListener((parent, view, position, id) -> {
            // This listener fires ONLY when a user makes a definitive selection.
            String selectedRole = (String) parent.getItemAtPosition(position);
            updateAuthUI(selectedRole);
        });

        // --- Setup Button Listeners ---
        sendOtpButton.setOnClickListener(v -> handlePhoneLogin());
        loginButton.setOnClickListener(v -> handleEmailLogin());

        // Set the initial state of the UI based on whatever is pre-filled (if anything)
        updateAuthUI(roleSpinner.getText().toString());
    }

    private void setupViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // For email/password login (Admin), route through SplashActivity so the same status/role checks apply.
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) return;
            Intent intent = new Intent(this, SplashActivity.class);
            intent.putExtra("skipDelay", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        authViewModel.getAuthError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            sendOtpButton.setEnabled(!isLoading);
            loginButton.setEnabled(!isLoading);
        });

        // This is the key observer. When the OTP is sent by the ViewModel, we navigate.
        authViewModel.getIsOtpSent().observe(this, isOtpSent -> {
            if (isOtpSent != null && isOtpSent) {
                authViewModel.resetOtpSentState(); // Reset state to prevent re-triggering
                navigateToOtpVerification();
            }
        });

        // --- REMOVED THE CONFLICTING PhoneAuthProvider.OnVerificationStateChangedCallbacks BLOCK ---
    }

    private void setupRoleSpinner() {
        // Create an ArrayAdapter
        String[] roles = getResources().getStringArray(R.array.user_roles);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );

        // Set the adapter to the AutoCompleteTextView
        roleSpinner.setAdapter(adapter);
    }

    private void updateAuthUI(String role) {
        if ("Community Health Worker (CHW)".equals(role) || "Doctor/Nurse".equals(role)) {
            phoneAuthLayout.setVisibility(View.VISIBLE);
            emailAuthLayout.setVisibility(View.GONE);
        } else if ("Admin".equals(role)) {
            phoneAuthLayout.setVisibility(View.GONE);
            emailAuthLayout.setVisibility(View.VISIBLE);
        } else {
            phoneAuthLayout.setVisibility(View.GONE);
            emailAuthLayout.setVisibility(View.GONE);
        }
    }


    private void handlePhoneLogin() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Standardize the phone number format
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+250" + phoneNumber;
        }

        phoneNumberForVerification = phoneNumber;
        authViewModel.sendOtpToPhone(this, phoneNumberForVerification);
    }


    private void handleEmailLogin() {
        // This functionality seems secondary, keeping it as is.
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String role = roleSpinner.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.loginWithEmail(email, password, role);
    }

    private void navigateToOtpVerification() {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        // Pass the phone number and verification ID from the ViewModel
        intent.putExtra("verificationId", authViewModel.getVerificationId().getValue());
        intent.putExtra("phoneNumber", phoneNumberForVerification);
        startActivity(intent);
    }

    private void navigateToDashboard(String role) {
        // This method is no longer the primary navigation path, but we keep it for email login.
        Intent intent;
        if (role.contains("CHW")) {
            intent = new Intent(this, ChwDashboardActivity.class);
        } else if (role.contains("Doctor")) {
            intent = new Intent(this, DoctorDashboardActivity.class);
        } else {
            // Default case, maybe go to a generic screen or back to login
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
