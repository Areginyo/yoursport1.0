package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TeamMatchesAdapter extends RecyclerView.Adapter<TeamMatchesAdapter.MatchViewHolder> {

    private List<TeamMatchesFragment.TeamMatchItem> matchItems = new ArrayList<>();
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(TeamMatchesFragment.TeamMatchItem item);
    }

    public void setOnMatchClickListener(OnMatchClickListener listener) {
        this.listener = listener;
    }

    public void setMatches(List<TeamMatchesFragment.TeamMatchItem> matchItems) {
        this.matchItems = matchItems != null ? matchItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        TeamMatchesFragment.TeamMatchItem item = matchItems.get(position);
        
        holder.txtHomeTeam.setText(item.homeTeamName);
        holder.txtAwayTeam.setText(item.awayTeamName);
        holder.txtLeague.setText(item.leagueName);
        
        // Load team logos
        loadLogo(holder.imgHomeTeam, item.homeTeamLogo);
        loadLogo(holder.imgAwayTeam, item.awayTeamLogo);

        // Set score or time based on status
        String s = item.status != null ? item.status.toUpperCase() : "";
        switch (s) {
            case "FT": case "AET": case "PEN":
                holder.txtScore.setText(item.homeGoals + " - " + item.awayGoals);
                holder.txtTime.setText(s);
                break;
            case "HT":
                holder.txtScore.setText(item.homeGoals + " - " + item.awayGoals);
                holder.txtTime.setText("HT");
                break;
            case "1H": case "2H": case "ET": case "LIVE":
                holder.txtScore.setText(item.homeGoals >= 0 ? item.homeGoals + " - " + item.awayGoals : "vs");
                holder.txtTime.setText("LIVE");
                break;
            case "NS": case "TBD":
                holder.txtScore.setText("vs");
                holder.txtTime.setText(formatMatchTime(item.date));
                break;
            case "PST": case "CANC": case "ABD":
                holder.txtScore.setText("—");
                holder.txtTime.setText(s);
                break;
            default:
                if (item.homeGoals >= 0 && item.awayGoals >= 0) {
                    holder.txtScore.setText(item.homeGoals + " - " + item.awayGoals);
                } else {
                    holder.txtScore.setText("vs");
                }
                holder.txtTime.setText(s);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMatchClick(item);
        });
    }

    private void loadLogo(ImageView iv, String url) {
        if (url != null && !url.isEmpty() && !url.equals("null")) {
            Glide.with(iv.getContext()).load(url)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(iv);
        } else {
            iv.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private String formatMatchTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH);
            out.setTimeZone(TimeZone.getDefault());
            Date date = in.parse(dateStr.replace("Z", "+00:00"));
            return out.format(date);
        } catch (Exception e) {
            // Try simpler format (date-only string like "2024-03-15")
            try {
                SimpleDateFormat in2 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat out2 = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                return out2.format(in2.parse(dateStr));
            } catch (Exception ex) {
                return dateStr;
            }
        }
    }

    @Override
    public int getItemCount() {
        return matchItems.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        ImageView imgHomeTeam;
        ImageView imgAwayTeam;
        TextView txtHomeTeam;
        TextView txtAwayTeam;
        TextView txtScore;
        TextView txtTime;
        TextView txtLeague;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            imgHomeTeam = itemView.findViewById(R.id.imgHomeTeam);
            imgAwayTeam = itemView.findViewById(R.id.imgAwayTeam);
            txtHomeTeam = itemView.findViewById(R.id.txtHomeTeam);
            txtAwayTeam = itemView.findViewById(R.id.txtAwayTeam);
            txtScore = itemView.findViewById(R.id.txtScore);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtLeague = itemView.findViewById(R.id.txtLeague);
        }
    }
}
