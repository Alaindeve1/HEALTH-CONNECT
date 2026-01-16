package rw.ac.auca.finalprojectgroupfa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.AdminUser;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder> {
    private List<AdminUser> users;
    private OnUserActionListener listener;
    private static SimpleDateFormat dateFormat;

    public interface OnUserActionListener {
        void onApproveUser(AdminUser user);
        void onRejectUser(AdminUser user);
        void onDisableUser(AdminUser user);
        void onActivateUser(AdminUser user);
        void onRemoveUser(AdminUser user);
        void onViewUserDetails(AdminUser user);
    }

    public AdminUsersAdapter(List<AdminUser> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    public void setUsers(List<AdminUser> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUser user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView userName;
        private TextView userDetails;
        private TextView userRole;
        private TextView requestDate;
        private Button approveButton;
        private Button rejectButton;
        private Button detailsButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userDetails = itemView.findViewById(R.id.userDetails);
            userRole = itemView.findViewById(R.id.userRole);
            requestDate = itemView.findViewById(R.id.requestDate);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
        }

        public void bind(AdminUser user, OnUserActionListener listener) {
            userName.setText(user.getName());
            userDetails.setText(user.getEmail());
            userRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : "USER");
            if (user.getCreatedAt() != null) {
                requestDate.setText(dateFormat.format(user.getCreatedAt()));
            } else {
                requestDate.setText("-");
            }

            // Set role-specific background color
            String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
            switch (role) {
                case "doctor":
                    userRole.setBackgroundColor(itemView.getContext().getColor(R.color.blue));
                    break;
                case "chw":
                    userRole.setBackgroundColor(itemView.getContext().getColor(R.color.green));
                    break;
                case "admin":
                    userRole.setBackgroundColor(itemView.getContext().getColor(R.color.orange));
                    break;
                default:
                    userRole.setBackgroundColor(itemView.getContext().getColor(R.color.gray));
            }

            // Configure actions based on user status
            String status = user.getStatus() != null ? user.getStatus().toLowerCase() : "pending";

            // Default visibility
            approveButton.setVisibility(View.VISIBLE);
            rejectButton.setVisibility(View.VISIBLE);

            if ("pending".equals(status)) {
                approveButton.setText("Approve");
                approveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.green)
                ));
                rejectButton.setText("Reject");
                rejectButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.red)
                ));

                approveButton.setOnClickListener(v -> {
                    if (listener != null) listener.onApproveUser(user);
                });
                rejectButton.setOnClickListener(v -> {
                    if (listener != null) listener.onRejectUser(user);
                });
            } else if ("approved".equals(status)) {
                approveButton.setText("Disable");
                approveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.orange)
                ));
                rejectButton.setText("Remove");
                rejectButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.red)
                ));

                approveButton.setOnClickListener(v -> {
                    if (listener != null) listener.onDisableUser(user);
                });
                rejectButton.setOnClickListener(v -> {
                    if (listener != null) listener.onRemoveUser(user);
                });
            } else if ("disabled".equals(status) || "rejected".equals(status)) {
                approveButton.setText("Activate");
                approveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.green)
                ));
                rejectButton.setText("Remove");
                rejectButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.red)
                ));

                approveButton.setOnClickListener(v -> {
                    if (listener != null) listener.onActivateUser(user);
                });
                rejectButton.setOnClickListener(v -> {
                    if (listener != null) listener.onRemoveUser(user);
                });
            } else if ("deleted".equals(status)) {
                // Deleted users are informational only.
                approveButton.setVisibility(View.GONE);
                rejectButton.setVisibility(View.GONE);
            } else {
                // Unknown status: default to Details-only
                approveButton.setVisibility(View.GONE);
                rejectButton.setVisibility(View.GONE);
            }

            detailsButton.setOnClickListener(v -> {
                if (listener != null) listener.onViewUserDetails(user);
            });

            // Whole item click for details
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onViewUserDetails(user);
            });
        }
    }
}