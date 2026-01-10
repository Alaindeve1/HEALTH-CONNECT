package rw.ac.auca.finalprojectgroupfa.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.ConsultationRequestAdapter;
import rw.ac.auca.finalprojectgroupfa.adapters.KPICardAdapter;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.models.KPICard;
import rw.ac.auca.finalprojectgroupfa.utils.HorizontalSpaceItemDecoration;
import rw.ac.auca.finalprojectgroupfa.viewmodels.DoctorDashboardViewModel;

public class DoctorDashboardActivity extends AppCompatActivity
        implements ConsultationRequestAdapter.OnConsultationActionListener {

    private DoctorDashboardViewModel doctorViewModel;
    private ProgressBar loadingProgress;
    private String currentDoctorId;
    private String currentDoctorName;

    // Layout references
    private LinearLayout pendingConsultationsLayout;
    private LinearLayout activeConsultationsLayout;
    private LinearLayout reportsLayout;
    private RecyclerView pendingRecyclerView;
    private RecyclerView activeRecyclerView;
    private TextView emptyPendingText;
    private TextView emptyActiveText;
    private View viewReportsCard;

    // Adapters
    private ConsultationRequestAdapter pendingAdapter;
    private ConsultationRequestAdapter activeAdapter;

    // KPI Views
    private RecyclerView kpiRecyclerView;
    private KPICardAdapter kpiAdapter;
    private TextView welcomeText;
    private TextView dashboardSummaryText;

    // Tab Buttons
    private Button pendingTab;
    private Button activeTab;
    private Button reportsTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Doctor Dashboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupViewModel();
        loadDoctorProfile();

        // Show pending by default
        showPendingConsultations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentDoctorId != null) {
            doctorViewModel.startListeningToConsultations(currentDoctorId);
            doctorViewModel.loadDoctorKPIs(currentDoctorId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        doctorViewModel.stopListening();
    }

    private void initializeViews() {
        loadingProgress = findViewById(R.id.loadingProgress);

        // Tab Layouts & Buttons
        pendingTab = findViewById(R.id.pendingTab);
        activeTab = findViewById(R.id.activeTab);
        reportsTab = findViewById(R.id.reportsTab);

        pendingConsultationsLayout = findViewById(R.id.pendingConsultationsLayout);
        activeConsultationsLayout = findViewById(R.id.activeConsultationsLayout);
        reportsLayout = findViewById(R.id.reportsLayout);

        pendingRecyclerView = findViewById(R.id.pendingRecyclerView);
        activeRecyclerView = findViewById(R.id.activeRecyclerView);

        emptyPendingText = findViewById(R.id.emptyPendingText);
        emptyActiveText = findViewById(R.id.emptyActiveText);
        viewReportsCard = findViewById(R.id.viewReportsCard);

        // Initialize KPI and dashboard views
        welcomeText = findViewById(R.id.welcomeText);
        dashboardSummaryText = findViewById(R.id.dashboardSummaryText);

        setupKPIRecyclerView();
        setupConsultationRecyclerViews();
        setupTabListeners();
    }

    private void setupConsultationRecyclerViews() {
        // Pending
        pendingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Use "doctor" role
        pendingAdapter = new ConsultationRequestAdapter(Collections.emptyList(), this, "doctor");
        pendingRecyclerView.setAdapter(pendingAdapter);

        // Active
        activeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activeAdapter = new ConsultationRequestAdapter(Collections.emptyList(), this, "doctor");
        activeRecyclerView.setAdapter(activeAdapter);
    }

    private void setupTabListeners() {
        pendingTab.setOnClickListener(v -> showPendingConsultations());
        activeTab.setOnClickListener(v -> showActiveConsultations());
        reportsTab.setOnClickListener(v -> showReports());

        if (viewReportsCard != null) {
            viewReportsCard.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportsActivity.class));
            });
        }
    }

    private void showPendingConsultations() {
        pendingConsultationsLayout.setVisibility(View.VISIBLE);
        activeConsultationsLayout.setVisibility(View.GONE);
        reportsLayout.setVisibility(View.GONE);

        updateTabStyle(pendingTab, true);
        updateTabStyle(activeTab, false);
        updateTabStyle(reportsTab, false);
    }

    private void showActiveConsultations() {
        pendingConsultationsLayout.setVisibility(View.GONE);
        activeConsultationsLayout.setVisibility(View.VISIBLE);
        reportsLayout.setVisibility(View.GONE);

        updateTabStyle(pendingTab, false);
        updateTabStyle(activeTab, true);
        updateTabStyle(reportsTab, false);
    }

    private void showReports() {
        pendingConsultationsLayout.setVisibility(View.GONE);
        activeConsultationsLayout.setVisibility(View.GONE);
        reportsLayout.setVisibility(View.VISIBLE);

        updateTabStyle(pendingTab, false);
        updateTabStyle(activeTab, false);
        updateTabStyle(reportsTab, true);
    }

    private void updateTabStyle(Button tab, boolean isActive) {
        if (isActive) {
            tab.setTextColor(getColor(android.R.color.white));
            tab.setBackgroundResource(R.drawable.tab_indicator_active);
        } else {
            tab.setTextColor(getColor(R.color.text_secondary));
            tab.setBackgroundResource(0); // Transparent
        }
    }

    private void setupViewModel() {
        doctorViewModel = new ViewModelProvider(this).get(DoctorDashboardViewModel.class);

        doctorViewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        doctorViewModel.getDoctorKPIs().observe(this, kpiCards -> {
            if (kpiCards != null && !kpiCards.isEmpty()) {
                kpiAdapter.submitList(kpiCards);
                updateDashboardSummary(kpiCards);
            }
        });

        doctorViewModel.getOperationStatus().observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                doctorViewModel.clearStatus();
            }
        });

        // Observe Pending Requests
        doctorViewModel.getPendingRequests().observe(this, requests -> {
            if (requests != null && !requests.isEmpty()) {
                pendingAdapter.setRequests(requests);
                pendingRecyclerView.setVisibility(View.VISIBLE);
                emptyPendingText.setVisibility(View.GONE);
            } else {
                pendingAdapter.setRequests(Collections.emptyList());
                pendingRecyclerView.setVisibility(View.GONE);
                emptyPendingText.setVisibility(View.VISIBLE);
            }
        });

        // Observe Active Consultations
        doctorViewModel.getActiveConsultations().observe(this, consultations -> {
            if (consultations != null && !consultations.isEmpty()) {
                activeAdapter.setRequests(consultations);
                activeRecyclerView.setVisibility(View.VISIBLE);
                emptyActiveText.setVisibility(View.GONE);
            } else {
                activeAdapter.setRequests(Collections.emptyList());
                activeRecyclerView.setVisibility(View.GONE);
                emptyActiveText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadDoctorProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentDoctorId = user.getUid();
            doctorViewModel.startListeningToConsultations(this.currentDoctorId);

            FirebaseFirestore.getInstance().collection("users").document(currentDoctorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            this.currentDoctorName = documentSnapshot.getString("name");
                            if (welcomeText != null) {
                                welcomeText.setText(getString(R.string.welcome_doctor, this.currentDoctorName));
                            }
                            doctorViewModel.loadDoctorKPIs(currentDoctorId);
                        } else {
                            // Warn but don't fail, maybe name is not set
                            this.currentDoctorName = "Doctor";
                            if (welcomeText != null) {
                                welcomeText.setText(getString(R.string.welcome_doctor, this.currentDoctorName));
                            }
                            doctorViewModel.loadDoctorKPIs(currentDoctorId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load doctor name.", Toast.LENGTH_SHORT).show();
                        this.currentDoctorName = "Doctor";
                        doctorViewModel.loadDoctorKPIs(currentDoctorId);
                    });
        } else {
            Toast.makeText(this, "Error: No authenticated user found.", Toast.LENGTH_SHORT).show();
            logoutUser();
        }
    }

    private void setupKPIRecyclerView() {
        kpiRecyclerView = findViewById(R.id.kpiRecyclerView);
        kpiRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        kpiAdapter = new KPICardAdapter();
        kpiRecyclerView.setAdapter(kpiAdapter);

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.kpi_card_spacing);
        kpiRecyclerView.addItemDecoration(new HorizontalSpaceItemDecoration(spacingInPixels));
    }

    private void updateDashboardSummary(List<KPICard> kpiCards) {
        if (dashboardSummaryText == null)
            return;

        int pendingRequests = 0;
        int activeConsultations = 0;

        for (KPICard card : kpiCards) {
            if (card.getTitle().equals(getString(R.string.kpi_pending_requests))) {
                try {
                    pendingRequests = Integer.parseInt(card.getValue().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    Log.e("DoctorDashboard", "Error parsing pending requests", e);
                }
            } else if (card.getTitle().equals(getString(R.string.kpi_active_consultations))) {
                try {
                    activeConsultations = Integer.parseInt(card.getValue().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    Log.e("DoctorDashboard", "Error parsing active consultations", e);
                }
            }
        }

        String summary = getString(R.string.doctor_dashboard_summary, pendingRequests, activeConsultations);
        dashboardSummaryText.setText(summary);
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
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onAcceptRequest(ConsultationRequest request) {
        if (currentDoctorId == null || currentDoctorName == null) {
            Toast.makeText(this, "Cannot accept request: doctor data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }
        doctorViewModel.acceptConsultationRequest(request, currentDoctorId, currentDoctorName);
    }

    @Override
    public void onDeclineRequest(ConsultationRequest request) {
        showDeclineReasonDialog(request);
    }

    @Override
    public void onStartConsultation(ConsultationRequest request) {
        Intent intent = new Intent(this, ConsultationDetailActivity.class);
        intent.putExtra("CONSULTATION_REQUEST", request);
        startActivity(intent);
    }

    private void showDeclineReasonDialog(ConsultationRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Decline Consultation");
        builder.setMessage("Please provide a reason for declining this consultation:");

        final EditText input = new EditText(this);
        input.setHint("Reason for declining...");
        builder.setView(input);

        builder.setPositiveButton("Decline", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                reason = "No reason provided";
            }
            doctorViewModel.declineConsultationRequest(request, reason);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onCompleteConsultation(ConsultationRequest request) {
        doctorViewModel.completeConsultation(request);
    }

    @Override
    public void onViewDetails(ConsultationRequest request) {
        rw.ac.auca.finalprojectgroupfa.dialogs.ConsultationDetailDialogFragment dialog = rw.ac.auca.finalprojectgroupfa.dialogs.ConsultationDetailDialogFragment
                .newInstance(request);
        dialog.show(getSupportFragmentManager(), "ConsultationDetailDialog");
    }
}
