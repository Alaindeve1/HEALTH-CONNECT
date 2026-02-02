package rw.ac.auca.finalprojectgroupfa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<ConsultationRequest> reports = new ArrayList<>();
    private OnReportActionListener listener;

    public interface OnReportActionListener {
        void onDownloadReport(ConsultationRequest report);
    }

    public ReportAdapter(OnReportActionListener listener) {
        this.listener = listener;
    }

    public void setReports(List<ConsultationRequest> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ConsultationRequest report = reports.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private TextView patientNameText;
        private TextView dateText;
        private TextView diagnosisText;
        private ImageButton downloadButton;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            patientNameText = itemView.findViewById(R.id.patientNameText);
            dateText = itemView.findViewById(R.id.dateText);
            diagnosisText = itemView.findViewById(R.id.diagnosisText);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }

        public void bind(ConsultationRequest report) {
            patientNameText.setText(report.getPatientName() != null ? report.getPatientName() : "Unknown Patient");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = report.getCompletedAt() != null ? sdf.format(report.getCompletedAt()) : "N/A";
            dateText.setText("Date: " + dateStr);

            diagnosisText.setText("Reason: " + (report.getReason() != null ? report.getReason() : "N/A"));

            downloadButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadReport(report);
                }
            });
        }
    }
}
