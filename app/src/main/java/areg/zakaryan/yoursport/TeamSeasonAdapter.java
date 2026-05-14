package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TeamSeasonAdapter extends RecyclerView.Adapter<TeamSeasonAdapter.LeagueTableViewHolder> {

    private List<TeamSeasonFragment.LeagueTableItem> leagueTables = new ArrayList<>();

    public void setLeagueTables(List<TeamSeasonFragment.LeagueTableItem> leagueTables) {
        this.leagueTables = leagueTables != null ? leagueTables : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeagueTableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_league_table, parent, false);
        return new LeagueTableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeagueTableViewHolder holder, int position) {
        TeamSeasonFragment.LeagueTableItem item = leagueTables.get(position);
        
        holder.txtLeagueName.setText(item.leagueName);
        holder.txtSeason.setText(item.season != null ? item.season + "/" + (Integer.parseInt(item.season) + 1) : "");
        
        TablePositionAdapter adapter = new TablePositionAdapter(item.positions);
        holder.recyclerViewPositions.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerViewPositions.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return leagueTables.size();
    }

    static class LeagueTableViewHolder extends RecyclerView.ViewHolder {
        TextView txtLeagueName;
        TextView txtSeason;
        RecyclerView recyclerViewPositions;

        LeagueTableViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLeagueName = itemView.findViewById(R.id.txtLeagueName);
            txtSeason = itemView.findViewById(R.id.txtSeason);
            recyclerViewPositions = itemView.findViewById(R.id.recyclerViewPositions);
        }
    }

    // Adapter for table positions
    static class TablePositionAdapter extends RecyclerView.Adapter<TablePositionAdapter.PositionViewHolder> {

        private List<TeamSeasonFragment.TablePosition> positions;

        public TablePositionAdapter(List<TeamSeasonFragment.TablePosition> positions) {
            this.positions = positions != null ? positions : new ArrayList<>();
        }

        @NonNull
        @Override
        public PositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_table_position, parent, false);
            return new PositionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PositionViewHolder holder, int position) {
            TeamSeasonFragment.TablePosition item = positions.get(position);
            
            holder.txtRank.setText(String.valueOf(item.rank));
            holder.txtTeamName.setText(item.teamName);
            holder.txtPlayed.setText(String.valueOf(item.played));
            holder.txtWon.setText(String.valueOf(item.won));
            holder.txtDraw.setText(String.valueOf(item.draw));
            holder.txtLost.setText(String.valueOf(item.lost));
            holder.txtGoalsFor.setText(String.valueOf(item.goalsFor));
            holder.txtGoalsAgainst.setText(String.valueOf(item.goalsAgainst));
            holder.txtGoalDiff.setText(item.goalDiff >= 0 ? "+" + item.goalDiff : String.valueOf(item.goalDiff));
            holder.txtPoints.setText(String.valueOf(item.points));
            
            // Load team logo
            if (item.teamLogo != null && !item.teamLogo.isEmpty() && !item.teamLogo.equals("null")) {
                Glide.with(holder.itemView.getContext())
                        .load(item.teamLogo)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.imgTeamLogo);
            } else {
                holder.imgTeamLogo.setImageResource(R.drawable.ic_placeholder);
            }
            
            // Highlight current team if needed (you can pass the team ID to highlight)
            // This would require passing the current team ID to the adapter
        }

        @Override
        public int getItemCount() {
            return positions.size();
        }

        static class PositionViewHolder extends RecyclerView.ViewHolder {
            TextView txtRank;
            ImageView imgTeamLogo;
            TextView txtTeamName;
            TextView txtPlayed;
            TextView txtWon;
            TextView txtDraw;
            TextView txtLost;
            TextView txtGoalsFor;
            TextView txtGoalsAgainst;
            TextView txtGoalDiff;
            TextView txtPoints;

            PositionViewHolder(@NonNull View itemView) {
                super(itemView);
                txtRank = itemView.findViewById(R.id.txtRank);
                imgTeamLogo = itemView.findViewById(R.id.imgTeamLogo);
                txtTeamName = itemView.findViewById(R.id.txtTeamName);
                txtPlayed = itemView.findViewById(R.id.txtPlayed);
                txtWon = itemView.findViewById(R.id.txtWon);
                txtDraw = itemView.findViewById(R.id.txtDraw);
                txtLost = itemView.findViewById(R.id.txtLost);
                txtGoalsFor = itemView.findViewById(R.id.txtGoalsFor);
                txtGoalsAgainst = itemView.findViewById(R.id.txtGoalsAgainst);
                txtGoalDiff = itemView.findViewById(R.id.txtGoalDiff);
                txtPoints = itemView.findViewById(R.id.txtPoints);
            }
        }
    }
}
