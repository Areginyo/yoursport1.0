package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SportTabAdapter extends RecyclerView.Adapter<SportTabAdapter.ViewHolder> {

    private final List<String> sports;
    private int selectedPosition = 0;
    private final OnSportSelectedListener listener;

    public interface OnSportSelectedListener {
        void onSportSelected(String sport);
    }

    public SportTabAdapter(List<String> sports, OnSportSelectedListener listener) {
        this.sports = sports;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(position);
        listener.onSportSelected(sports.get(position));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sport_tab, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String sport = sports.get(position);
        holder.tvSport.setText(sport);

        // Устанавливаем иконку (используй свои иконки)
        int iconRes = getSportIcon(sport);
        holder.ivIcon.setImageResource(iconRes);

        // Выделение выбранного
        holder.itemView.setBackgroundResource(
                position == selectedPosition ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg
        );

        holder.itemView.setOnClickListener(v -> setSelectedPosition(position));

        holder.tvSport.setTypeface(ResourcesCompat.getFont(holder.tvSport.getContext(), R.font.font_oswald_bold));
    }

    private int getSportIcon(String sport) {
        switch (sport) {
            case "Football":   return R.drawable.ic_football;
            case "UFC":        return R.drawable.ic_ufc_1;
            case "Basketball": return R.drawable.ic_basketball;
            case "Formula 1":  return R.drawable.ic_formula1_1;
            case "Tennis":     return R.drawable.ic_tennis;
            default:           return R.drawable.ic_sport_default;
        }
    }

    @Override
    public int getItemCount() {
        return sports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvSport;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_sport_icon);
            tvSport = itemView.findViewById(R.id.tv_sport_name);
        }
    }
}