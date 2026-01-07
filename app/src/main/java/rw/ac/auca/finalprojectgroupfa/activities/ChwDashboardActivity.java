package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.KPICardAdapter;
import rw.ac.auca.finalprojectgroupfa.models.KPICard;
import rw.ac.auca.finalprojectgroupfa.utils.HorizontalSpaceItemDecoration;
import rw.ac.auca.finalprojectgroupfa.viewmodels.ChwDashboardViewModel;

public class ChwDashboardActivity extends AppCompatActivity {
    private ChwDashboardViewModel viewModel;
    private ProgressBar loadingProgress;
    private RecyclerView kpiRecyclerView;
    private KPICardAdapter kpiAdapter;
    private TextView welcomeText;
    private TextView dashboardSummaryText;
    private MaterialButton addPatientCard;

    private MaterialButton patientsListCard;
    private MaterialButton consultationsListCard;
    private String currentChwId;
    private String currentChwName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chw_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.chw_dashboard_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        initializeViews();

        // Setup ViewModel
        setupViewModel();

        // Load CHW profile and KPI data
        loadChwProfile();
    }

    private void initializeViews() {
        // Initialize UI elements
        loadingProgress = findViewById(R.id.loadingProgress);
        welcomeText = findViewById(R.id.welcomeText);
        dashboardSummaryText = findViewById(R.id.dashboardSummaryText);

        // Initialize action cards
        addPatientCard = findViewById(R.id.addPatientCard);

        patientsListCard = findViewById(R.id.patientsListCard);
        consultationsListCard = findViewById(R.id.consultationsListCard);

        // Setup KPI RecyclerView
        setupKPIRecyclerView();

        // Set click listeners
        setupClickListeners();
    }

    private void setupKPIRecyclerView() {
        kpiRecyclerView = findViewById(R.id.kpiRecyclerView);
        kpiRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        kpiAdapter = new KPICardAdapter();
        kpiRecyclerView.setAdapter(kpiAdapter);

        // Add spacing between KPI cards
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.kpi_card_spacing);
        kpiRecyclerView.addItemDecoration(new HorizontalSpaceItemDecoration(spacingInPixels));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChwDashboardViewModel.class);

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe KPI data
        viewModel.getChwKPIs().observe(this, kpiCards -> {
            if (kpiCards != null && !kpiCards.isEmpty()) {
                kpiAdapter.submitList(kpiCards);
                updateDashboardSummary(kpiCards);
            }
        });

        // Observe operation status for errors
        viewModel.getOperationStatus().observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                viewModel.clearStatus();
            }
        });
    }

    private void loadChwProfile() {
        // Get current user ID from Firebase Auth
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.currentChwId = userId;

        // In a real app, you would fetch the CHW's name from Firestore
        // For now, we'll use a placeholder
        this.currentChwName = "CHW User";
        welcomeText.setText(getString(R.string.welcome_chw, currentChwName));

        viewModel.startListeningToConsultations(currentChwId);

        // Load KPI data
        viewModel.loadChwKPIs(currentChwId);
    }

    private void updateDashboardSummary(List<KPICard> kpiCards) {
        if (dashboardSummaryText == null)
            return;

        // Find the relevant KPIs
        int pendingRequests = 0;
        int activeConsultations = 0;
        int completedToday = 0;

        for (KPICard card : kpiCards) {
            if (card.getTitle().equals(getString(R.string.kpi_pending_requests))) {
                try {
                    pendingRequests = Integer.parseInt(card.getValue().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    Log.e("ChwDashboard", "Error parsing pending requests", e);
                }
            } else if (card.getTitle().equals(getString(R.string.kpi_active_consultations))) {
                try {
                    activeConsultations = Integer.parseInt(card.getValue().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    Log.e("ChwDashboard", "Error parsing active consultations", e);
                }
            } else if (card.getTitle().equals(getString(R.string.kpi_completed_today))) {
                try {
                    completedToday = Integer.parseInt(card.getValue().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    Log.e("ChwDashboard", "Error parsing completed today", e);
                }
            }
        }

        // Build the summary text
        StringBuilder summary = new StringBuilder();

        if (pendingRequests > 0) {
            summary.append(getResources().getQuantityString(
                    R.plurals.pending_requests_summary,
                    pendingRequests,
                    pendingRequests));
        }

        if (activeConsultations > 0) {
            if (summary.length() > 0)
                summary.append("\n");
            summary.append(getResources().getQuantityString(
                    R.plurals.active_consultations_summary,
                    activeConsultations,
                    activeConsultations));
        }

        if (completedToday > 0) {
            if (summary.length() > 0)
                summary.append("\n");
            summary.append(getResources().getQuantityString(
                    R.plurals.completed_today_summary,
                    completedToday,
                    completedToday));
        }

        // If no data yet, show a default message
        if (summary.length() == 0) {
            summary.append(getString(R.string.no_activity_summary));
        }

        dashboardSummaryText.setText(summary.toString());
    }

    private void setupClickListeners() {
        if (patientsListCard != null) {
            patientsListCard.setOnClickListener(v -> startActivity(new Intent(this, PatientListActivity.class)));
        }

        if (consultationsListCard != null) {
            consultationsListCard
                    .setOnClickListener(v -> startActivity(new Intent(this, ChwConsultationListActivity.class)));
        }

        if (addPatientCard != null) {
            addPatientCard.setOnClickListener(v -> startActivity(new Intent(this, AddPatientActivity.class)));
        }

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
        Intent intent = new Intent(ChwDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
