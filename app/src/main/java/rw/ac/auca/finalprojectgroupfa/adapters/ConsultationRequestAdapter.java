package rw.ac.auca.finalprojectgroupfa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ConsultationRequestAdapter extends RecyclerView.Adapter<ConsultationRequestAdapter.ConsultationViewHolder> {
    private List<ConsultationRequest> requests;
    private OnConsultationActionListener listener;
    private static SimpleDateFormat dateFormat;
    private String userRole; // "doctor" or "chw"

    public interface OnConsultationActionListener {
        void onAcceptRequest(ConsultationRequest request);
        void onDeclineRequest(ConsultationRequest request);
        void onStartConsultation(ConsultationRequest request);
        void onCompleteConsultation(ConsultationRequest request);
        void onViewDetails(ConsultationRequest request);
    }

    public ConsultationRequestAdapter(List<ConsultationRequest> requests, OnConsultationActionListener listener, String userRole) {
        this.requests = requests;
        this.listener = listener;
        this.userRole = userRole;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    public void setRequests(List<ConsultationRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConsultationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_consultation_request, parent, false);
        return new ConsultationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConsultationViewHolder holder, int position) {
        ConsultationRequest request = requests.get(position);
        holder.bind(request, listener, userRole);
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class ConsultationViewHolder extends RecyclerView.ViewHolder {
        private TextView patientName;
        private TextView requestDetails;
        private TextView priorityBadge;
        private TextView statusBadge;
        private TextView requestTime;
        private Button acceptButton;
        private Button declineButton;
        private Button startButton;
        private Button completeButton;
        private Button detailsButton;

        public ConsultationViewHolder(@NonNull View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.patientName);
            requestDetails = itemView.findViewById(R.id.requestDetails);
            priorityBadge = itemView.findViewById(R.id.priorityBadge);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            requestTime = itemView.findViewById(R.id.requestTime);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
            startButton = itemView.findViewById(R.id.startButton);
            completeButton = itemView.findViewById(R.id.completeButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
        }

        public void bind(ConsultationRequest request, OnConsultationActionListener listener, String userRole) {
            // Basic info
            patientName.setText(request.getPatientName());
            requestDetails.setText(request.getReason());
            requestTime.setText(dateFormat.format(request.getRequestedAt()));

            // Priority badge
            priorityBadge.setText(request.getPriority().toUpperCase());
            switch (request.getPriority()) {
                case "stat":
                    priorityBadge.setBackgroundColor(itemView.getContext().getColor(R.color.red));
                    break;
                case "urgent":
                    priorityBadge.setBackgroundColor(itemView.getContext().getColor(R.color.orange));
                    break;
                case "asap":
                    priorityBadge.setBackgroundColor(itemView.getContext().getColor(R.color.yellow));
                    priorityBadge.setTextColor(itemView.getContext().getColor(android.R.color.black));
                    break;
                default:
                    priorityBadge.setBackgroundColor(itemView.getContext().getColor(R.color.green));
            }

            // Status badge
            statusBadge.setText(request.getStatus().toUpperCase());
            switch (request.getStatus()) {
                case "active":
                    statusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.blue));
                    break;
                case "accepted":
                    statusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.green));
                    break;
                case "completed":
                    statusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.gray));
                    break;
                case "revoked":
                    statusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.red));
                    break;
                default:
                    statusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.gray));
            }

            // Show/hide buttons based on user role and status
            if ("doctor".equals(userRole)) {
                setupDoctorView(request, listener);
            } else {
                setupChwView(request, listener);
            }

            // Details button always visible
            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetails(request);
                }
            });
        }

        private void setupDoctorView(ConsultationRequest request, OnConsultationActionListener listener) {
            switch (request.getStatus()) {
                case "pending":
                    // Pending request - show accept/decline
                    acceptButton.setVisibility(View.VISIBLE);
                    declineButton.setVisibility(View.VISIBLE);
                    startButton.setVisibility(View.GONE);
                    completeButton.setVisibility(View.GONE);

                    acceptButton.setOnClickListener(v -> {
                        if (listener != null) listener.onAcceptRequest(request);
                    });
                    declineButton.setOnClickListener(v -> {
                        if (listener != null) listener.onDeclineRequest(request);
                    });
                    break;

                case "accepted":
                    // Accepted but not started - show start button
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    startButton.setVisibility(View.VISIBLE);
                    completeButton.setVisibility(View.GONE);

                    startButton.setOnClickListener(v -> {
                        if (listener != null) listener.onStartConsultation(request);
                    });
                    break;

                case "in-progress":
                    // In progress - show complete button
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    startButton.setVisibility(View.GONE);
                    completeButton.setVisibility(View.VISIBLE);

                    completeButton.setOnClickListener(v -> {
                        if (listener != null) listener.onCompleteConsultation(request);
                    });
                    break;

                default:
                    // Completed or revoked - hide all action buttons
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    startButton.setVisibility(View.GONE);
                    completeButton.setVisibility(View.GONE);
            }
        }

        private void setupChwView(ConsultationRequest request, OnConsultationActionListener listener) {
            // CHW can only view details
            acceptButton.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);
            startButton.setVisibility(View.GONE);
            completeButton.setVisibility(View.GONE);
        }
    }
}