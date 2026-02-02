package rw.ac.auca.finalprojectgroupfa.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.activities.VitalsActivity;
import rw.ac.auca.finalprojectgroupfa.activities.VitalsHistoryActivity;
import rw.ac.auca.finalprojectgroupfa.models.Patient;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<Patient> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    public PatientAdapter(List<Patient> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.bind(patient, listener);
    }

    @Override
    public int getItemCount() {
        return patients != null ? patients.size() : 0;
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        private TextView patientName;
        private TextView patientDetails;
        private TextView syncStatus;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.patientName);
            patientDetails = itemView.findViewById(R.id.patientDetails);
            syncStatus = itemView.findViewById(R.id.syncStatus);
        }

        public void bind(Patient patient, OnPatientClickListener listener) {
            patientName.setText(patient.getName());
            patientDetails.setText(String.format("%d years • %s • %s",
                    patient.getAge(), patient.getGender(), patient.getVillage()));

            // Sync status
            if (patient.isSynced()) {
                syncStatus.setText("Synced");
                syncStatus.setBackgroundColor(itemView.getContext().getColor(R.color.green));
            } else {
                syncStatus.setText("Offline");
                syncStatus.setBackgroundColor(itemView.getContext().getColor(R.color.orange));
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatientClick(patient);
                }
            });
        }
    }
}