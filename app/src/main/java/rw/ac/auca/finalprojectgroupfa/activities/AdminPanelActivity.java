package rw.ac.auca.finalprojectgroupfa.activities;

import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.AdminUsersAdapter;
import rw.ac.auca.finalprojectgroupfa.models.AdminUser;
import rw.ac.auca.finalprojectgroupfa.viewmodels.AdminViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminPanelActivity extends AppCompatActivity implements AdminUsersAdapter.OnUserActionListener {
    private AdminViewModel adminViewModel;
    private AdminUsersAdapter pendingUsersAdapter;
    private AdminUsersAdapter allUsersAdapter;

    // Layout references
    private LinearLayout userManagementLayout, analyticsLayout, systemLogsLayout;
    private RecyclerView pendingUsersRecyclerView;
    private RecyclerView allUsersRecyclerView;
    private TextView emptyPendingText;
    private TextView emptyAllUsersText;
    private ProgressBar loadingProgress;
    private Button forceSyncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Panel");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupViewModel();
        setupRecyclerView();
        showUserManagement(); // Show user management by default
    }

    private void initializeViews() {
        // Initialize layout sections
        userManagementLayout = findViewById(R.id.userManagementLayout);
        analyticsLayout = findViewById(R.id.analyticsLayout);
        systemLogsLayout = findViewById(R.id.systemLogsLayout);

        // Initialize user management views
        pendingUsersRecyclerView = findViewById(R.id.pendingUsersRecyclerView);
        allUsersRecyclerView = findViewById(R.id.allUsersRecyclerView);
        emptyPendingText = findViewById(R.id.emptyPendingText);
        emptyAllUsersText = findViewById(R.id.emptyAllUsersText);
        loadingProgress = findViewById(R.id.loadingProgress);

        // Initialize analytics views
        forceSyncButton = findViewById(R.id.forceSyncButton);
        forceSyncButton.setOnClickListener(v -> forceSync());

        // Setup tab functionality
        setupTabs();
    }

    private void setupTabs() {
        // Simple tab implementation without ViewPager
        Button userManagementTab = findViewById(R.id.userManagementTab);
        Button analyticsTab = findViewById(R.id.analyticsTab);
        Button systemLogsTab = findViewById(R.id.systemLogsTab);

        userManagementTab.setOnClickListener(v -> showUserManagement());
        analyticsTab.setOnClickListener(v -> showAnalytics());
        systemLogsTab.setOnClickListener(v -> showSystemLogs());
    }

    private void setupViewModel() {
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Observe pending users
        adminViewModel.getPendingUsers().observe(this, users -> {
            if (users != null && !users.isEmpty()) {
                pendingUsersAdapter.setUsers(users);
                emptyPendingText.setVisibility(View.GONE);
                pendingUsersRecyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyPendingText.setVisibility(View.VISIBLE);
                pendingUsersRecyclerView.setVisibility(View.GONE);
            }
        });

        // Observe all users (exclude pending and deleted in the "All Users" section to avoid duplication)
        adminViewModel.getAllUsers().observe(this, users -> {
            List<AdminUser> filtered = new ArrayList<>();
            if (users != null) {
                for (AdminUser u : users) {
                    String status = u.getStatus() != null ? u.getStatus().toLowerCase() : "pending";
                    if ("pending".equals(status)) continue;
                    if ("deleted".equals(status)) continue;
                    filtered.add(u);
                }
            }

            if (!filtered.isEmpty()) {
                allUsersAdapter.setUsers(filtered);
                emptyAllUsersText.setVisibility(View.GONE);
                allUsersRecyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyAllUsersText.setVisibility(View.VISIBLE);
                allUsersRecyclerView.setVisibility(View.GONE);
            }
        });

        // Observe loading state
        adminViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe operation status
        adminViewModel.getOperationStatus().observe(this, status -> {
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                adminViewModel.clearStatus();
            }
        });
    }

    private void setupRecyclerView() {
        pendingUsersAdapter = new AdminUsersAdapter(null, this);
        pendingUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pendingUsersRecyclerView.setAdapter(pendingUsersAdapter);

        allUsersAdapter = new AdminUsersAdapter(null, this);
        allUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allUsersRecyclerView.setAdapter(allUsersAdapter);
    }

    private void showUserManagement() {
        userManagementLayout.setVisibility(View.VISIBLE);
        analyticsLayout.setVisibility(View.GONE);
        systemLogsLayout.setVisibility(View.GONE);

        // Update tab states
        updateTabStates(R.id.userManagementTab);
    }

    private void showAnalytics() {
        userManagementLayout.setVisibility(View.GONE);
        analyticsLayout.setVisibility(View.VISIBLE);
        systemLogsLayout.setVisibility(View.GONE);

        displayAnalytics();
        updateTabStates(R.id.analyticsTab);
    }

    private void showSystemLogs() {
        userManagementLayout.setVisibility(View.GONE);
        analyticsLayout.setVisibility(View.GONE);
        systemLogsLayout.setVisibility(View.VISIBLE);
        updateTabStates(R.id.systemLogsTab);
    }

    private void updateTabStates(int selectedTabId) {
        // Reset all tabs
        Button userManagementTab = findViewById(R.id.userManagementTab);
        Button analyticsTab = findViewById(R.id.analyticsTab);
        Button systemLogsTab = findViewById(R.id.systemLogsTab);

        int defaultText = getColor(R.color.text_primary);
        int selectedText = getColor(android.R.color.white);

        userManagementTab.setBackgroundColor(getColor(android.R.color.transparent));
        analyticsTab.setBackgroundColor(getColor(android.R.color.transparent));
        systemLogsTab.setBackgroundColor(getColor(android.R.color.transparent));

        userManagementTab.setTextColor(defaultText);
        analyticsTab.setTextColor(defaultText);
        systemLogsTab.setTextColor(defaultText);

        // Highlight selected tab (blue background + white text)
        Button selectedTab = findViewById(selectedTabId);
        selectedTab.setBackgroundColor(getColor(R.color.blue));
        selectedTab.setTextColor(selectedText);
    }

    private void displayAnalytics() {
        adminViewModel.getSystemAnalytics().observe(this, analytics -> {
            if (analytics != null) {
                TextView totalUsersText = findViewById(R.id.totalUsersText);
                TextView totalPatientsText = findViewById(R.id.totalPatientsText);
                TextView totalConsultationsText = findViewById(R.id.totalConsultationsText);
                TextView activeConsultationsText = findViewById(R.id.activeConsultationsText);
                TextView syncQueueText = findViewById(R.id.syncQueueText);

                totalUsersText.setText(String.valueOf(analytics.getTotalUsers()));
                totalPatientsText.setText(String.valueOf(analytics.getTotalPatients()));
                totalConsultationsText.setText(String.valueOf(analytics.getTotalConsultations()));
                activeConsultationsText.setText(String.valueOf(analytics.getActiveConsultations()));
                syncQueueText.setText(String.valueOf(analytics.getSyncQueueSize()));
            }
        });
    }

    private void forceSync() {
        adminViewModel.forceSync();
    }

    @Override
    public void onApproveUser(AdminUser user) {
        adminViewModel.approveUser(user.getUserId(), getCurrentAdminId());
    }

    @Override
    public void onRejectUser(AdminUser user) {
        showRejectReasonDialog(user);
    }

    @Override
    public void onDisableUser(AdminUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Disable User")
                .setMessage("Disable this user? They will not be able to log in.")
                .setPositiveButton("Disable", (d, w) -> adminViewModel.disableUser(user.getUserId(), getCurrentAdminId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onActivateUser(AdminUser user) {
        adminViewModel.activateUser(user.getUserId(), getCurrentAdminId());
    }

    @Override
    public void onRemoveUser(AdminUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Remove User")
                .setMessage("Remove this user from the app? (status will be set to deleted)")
                .setPositiveButton("Remove", (d, w) -> adminViewModel.removeUser(user.getUserId(), getCurrentAdminId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewUserDetails(AdminUser user) {
        showUserDetails(user);
    }

    private String getCurrentAdminId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "admin";
    }

    private void showRejectReasonDialog(AdminUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reject User");
        builder.setMessage("Please provide a reason for rejecting this user:");

        final EditText input = new EditText(this);
        input.setHint("Reason for rejection...");
        builder.setView(input);

        builder.setPositiveButton("Reject", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                reason = "No reason provided";
            }
            adminViewModel.rejectUser(user.getUserId(), getCurrentAdminId(), reason);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showUserDetails(AdminUser user) {
        String details = String.format(
                "User Details\n\n" +
                        "Name: %s\n" +
                        "Email: %s\n" +
                        "Role: %s\n" +
                        "Status: %s\n" +
                        "Phone: %s\n" +
                        "Created: %s\n" +
                        "Qualification: %s\n" +
                        "License: %s\n" +
                        "District: %s",
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "Not provided",
                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(user.getCreatedAt()),
                user.getQualification() != null ? user.getQualification() : "N/A",
                user.getLicenseNumber() != null ? user.getLicenseNumber() : "N/A",
                user.getDistrict() != null ? user.getDistrict() : "N/A"
        );

        new AlertDialog.Builder(this)
                .setTitle("User Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }
}