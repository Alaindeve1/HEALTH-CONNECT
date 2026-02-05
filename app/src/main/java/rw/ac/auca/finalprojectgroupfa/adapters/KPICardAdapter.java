package rw.ac.auca.finalprojectgroupfa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.KPICard;
import com.google.android.material.card.MaterialCardView;

/**
 * RecyclerView adapter for KPI cards displayed on the dashboards.
 */
public class KPICardAdapter extends RecyclerView.Adapter<KPICardAdapter.KPIViewHolder> {

    private final List<KPICard> items = new ArrayList<>();

    public void submitList(List<KPICard> kpiCards) {
        items.clear();
        if (kpiCards != null) {
            items.addAll(kpiCards);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KPIViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kpi_card, parent, false);
        return new KPIViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KPIViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class KPIViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView value;
        private final TextView subtitle;
        private final ImageView icon;
        private final MaterialCardView cardView;

        KPIViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            title = itemView.findViewById(R.id.kpiTitle);
            value = itemView.findViewById(R.id.kpiValue);
            subtitle = itemView.findViewById(R.id.kpiSubtitle);
            icon = itemView.findViewById(R.id.kpiIcon);
        }

        void bind(KPICard card) {
            title.setText(card.getTitle());
            value.setText(card.getValue());
            subtitle.setText(card.getSubtitle());
            icon.setImageResource(card.getIconResId());

            if (card.getBackgroundColorRes() != null) {
                int color = ContextCompat.getColor(cardView.getContext(), card.getBackgroundColorRes());
                cardView.setCardBackgroundColor(color);
            } else {
                cardView.setCardBackgroundColor(
                        ContextCompat.getColor(cardView.getContext(), R.color.surface)
                );
            }

            if (card.getTextColorRes() != null) {
                int color = ContextCompat.getColor(cardView.getContext(), card.getTextColorRes());
                title.setTextColor(color);
                value.setTextColor(color);
                subtitle.setTextColor(color);
                icon.setColorFilter(color);
            } else {
                // Reset colors to defaults from XML when not specified
                int titleColor = ContextCompat.getColor(cardView.getContext(), R.color.blue_800);
                int valueColor = ContextCompat.getColor(cardView.getContext(), R.color.blue_700);
                int subtitleColor = ContextCompat.getColor(cardView.getContext(), R.color.blue_600);
                int iconColor = ContextCompat.getColor(cardView.getContext(), R.color.blue_500);

                title.setTextColor(titleColor);
                value.setTextColor(valueColor);
                subtitle.setTextColor(subtitleColor);
                icon.setColorFilter(iconColor);
            }
        }
    }
}

