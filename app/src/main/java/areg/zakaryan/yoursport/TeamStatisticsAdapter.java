package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter kept for source compatibility.
 * TeamStatisticsFragment populates views directly; this adapter is unused at runtime.
 */
public class TeamStatisticsAdapter extends RecyclerView.Adapter<TeamStatisticsAdapter.StatisticsViewHolder> {

    // Self-contained data class – no longer nested in fragment
    public static class StatItem {
        public String label;
        public String value;
        public StatItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<StatItem> items = new ArrayList<>();

    public void setItems(List<StatItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatisticsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new StatisticsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatisticsViewHolder holder, int position) {
        StatItem item = items.get(position);
        holder.tvLabel.setText(item.label);
        holder.tvValue.setText(item.value);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class StatisticsViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel;
        TextView tvValue;

        StatisticsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(android.R.id.text1);
            tvValue = itemView.findViewById(android.R.id.text2);
        }
    }
}
