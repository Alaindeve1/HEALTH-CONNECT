package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.ChwConsultationAdapter;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.viewmodels.ChwDashboardViewModel;

public class ChwConsultationListActivity extends AppCompatActivity
        implements ChwConsultationAdapter.OnChwConsultationListener {

    private ChwDashboardViewModel viewModel;
    private RecyclerView recyclerView;
    private ChwConsultationAdapter adapter;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chw_consultation_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Consultations");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.chwConsultationsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        adapter = new ChwConsultationAdapter(null, this, "chw");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChwDashboardViewModel.class);

        // For the CHW view, show all consultation requests they have created
        viewModel.getAllConsultations().observe(this, consultations -> {
            if (consultations == null || consultations.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                adapter.setConsultations(consultations);
                emptyStateText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStartChat(ConsultationRequest request) {
        Intent intent = new Intent(this, ConsultationDetailActivity.class);
        intent.putExtra("CONSULTATION_REQUEST", request);
        startActivity(intent);
    }

    @Override
    public void onViewDetails(ConsultationRequest request) {
        // For now, we just open the chat. Details view can be added later.
        onStartChat(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
