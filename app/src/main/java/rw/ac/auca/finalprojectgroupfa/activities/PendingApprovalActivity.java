package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import rw.ac.auca.finalprojectgroupfa.R;

public class PendingApprovalActivity extends AppCompatActivity {

    private TextView statusText;
    private ProgressBar progress;
    private Button refreshButton;
    private Button signOutButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        statusText = findViewById(R.id.pendingStatusText);
        progress = findViewById(R.id.pendingProgress);
        refreshButton = findViewById(R.id.refreshStatusButton);
        signOutButton = findViewById(R.id.signOutButton);

        refreshButton.setOnClickListener(v -> checkAndRoute());
        signOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            navigateTo(LoginActivity.class);
        });

        checkAndRoute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the admin approved the user while the app was backgrounded, a quick refresh should route them.
        checkAndRoute();
    }

    private void checkAndRoute() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            navigateTo(LoginActivity.class);
            return;
        }

        setLoading(true);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(this::handleUserDoc)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to load account status. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    private void handleUserDoc(DocumentSnapshot doc) {
        setLoading(false);

        if (!doc.exists()) {
            // Brand new user: they must create a profile first.
            navigateTo(CreateProfileActivity.class);
            return;
        }

        String role = doc.getString("role");
        String status = doc.getString("status");
        if (status == null) status = "pending";

        if (!"approved".equalsIgnoreCase(status)) {
            statusText.setText("Account status: " + status + "\n\nPlease wait for admin approval.");
            return;
        }

        // Approved: route to the correct dashboard.
        if ("doctor".equals(role)) {
            navigateTo(DoctorDashboardActivity.class);
        } else if ("chw".equals(role)) {
            navigateTo(ChwDashboardActivity.class);
        } else if ("admin".equals(role)) {
            navigateTo(AdminPanelActivity.class);
        } else {
            navigateTo(LoginActivity.class);
        }
    }

    private void setLoading(boolean isLoading) {
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        refreshButton.setEnabled(!isLoading);
        signOutButton.setEnabled(!isLoading);
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
