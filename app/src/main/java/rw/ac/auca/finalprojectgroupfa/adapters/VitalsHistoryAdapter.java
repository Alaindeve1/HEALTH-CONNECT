package rw.ac.auca.finalprojectgroupfa.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.ObservationDraft;

public class VitalsHistoryAdapter extends RecyclerView.Adapter<VitalsHistoryAdapter.VitalsViewHolder> {

    private List<ObservationDraft> vitalsList;
    private final Context context;

    // 1. Define the listener interface
    private final OnVitalsClickListener clickListener;

    public interface OnVitalsClickListener {
        void onVitalClick(ObservationDraft observation);
    }

    // 2. Add a constructor to accept the listener
    public VitalsHistoryAdapter(Context context, OnVitalsClickListener listener) {
        this.context = context;
        this.clickListener = listener; // Set listener via constructor
    }

    public void setVitals(List<ObservationDraft> vitals) {
        this.vitalsList = vitals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VitalsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vitals_history, parent, false);
        return new VitalsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VitalsViewHolder holder, int position) {
        if (vitalsList != null) {
            ObservationDraft currentVitals = vitalsList.get(position);
            holder.bind(currentVitals, clickListener); // Pass the listener to the bind method
        }
    }

    @Override
    public int getItemCount() {
        return vitalsList != null ? vitalsList.size() : 0;
    }

    static class VitalsViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText, vitalsSummaryText, syncStatusText, symptomsText;

        public VitalsViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            vitalsSummaryText = itemView.findViewById(R.id.vitalsSummaryText);
            syncStatusText = itemView.findViewById(R.id.syncStatusText);
            symptomsText = itemView.findViewById(R.id.symptomsText);
        }

        public void bind(final ObservationDraft vitals, final OnVitalsClickListener listener) {
            // Your existing bind logic
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            if (vitals.getRecordedAt() != null) {
                dateText.setText(sdf.format(vitals.getRecordedAt()));
            }

            String summary = String.format(Locale.getDefault(),
                    "Temp: %.1f°C | BP: %d/%d | HR: %d bpm",
                    vitals.getTemperature(),
                    vitals.getSystolicBP(),
                    vitals.getDiastolicBP(),
                    vitals.getHeartRate());
            vitalsSummaryText.setText(summary);

            if (vitals.isSynced()) {
                syncStatusText.setText("Synced");
                syncStatusText.setBackgroundColor(itemView.getContext().getColor(R.color.green));
            } else {
                syncStatusText.setText("Offline");
                syncStatusText.setBackgroundColor(itemView.getContext().getColor(R.color.orange));
            }

            if (vitals.getSymptoms() != null && !vitals.getSymptoms().isEmpty()) {
                symptomsText.setText("Symptoms: " + vitals.getSymptoms());
                symptomsText.setVisibility(View.VISIBLE);
            } else {
                symptomsText.setVisibility(View.GONE);
            }


            // 3. Set the click listener on the entire item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVitalClick(vitals);
                }
            });
        }
    }
}
