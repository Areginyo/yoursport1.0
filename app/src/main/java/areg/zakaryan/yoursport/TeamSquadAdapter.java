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
import java.util.Map;
import java.util.TreeMap;

public class TeamSquadAdapter extends RecyclerView.Adapter<TeamSquadAdapter.SquadGroupViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(TeamSquadFragment.SquadItem item);
    }

    private List<SquadGroup> squadGroups = new ArrayList<>();
    private OnPlayerClickListener playerClickListener;

    public void setOnPlayerClickListener(OnPlayerClickListener l) {
        this.playerClickListener = l;
    }

    public void setSquad(List<TeamSquadFragment.SquadItem> squadItems) {
        this.squadGroups = organizeSquadByPosition(squadItems);
        notifyDataSetChanged();
    }

    private List<SquadGroup> organizeSquadByPosition(List<TeamSquadFragment.SquadItem> squadItems) {
        // Use a defined order for position groups
        String[] positionOrder = {"Goalkeepers", "Defenders", "Midfielders", "Forwards", "Other"};
        Map<String, List<TeamSquadFragment.SquadItem>> grouped = new TreeMap<>();
        for (String pos : positionOrder) grouped.put(pos, new ArrayList<>());

        for (TeamSquadFragment.SquadItem item : squadItems) {
            String cat = item.getPositionCategory();
            if (!grouped.containsKey(cat)) grouped.put(cat, new ArrayList<>());
            grouped.get(cat).add(item);
        }

        List<SquadGroup> groups = new ArrayList<>();
        for (String pos : positionOrder) {
            List<TeamSquadFragment.SquadItem> players = grouped.get(pos);
            if (players != null && !players.isEmpty()) {
                groups.add(new SquadGroup(pos, players));
            }
        }
        return groups;
    }

    @NonNull
    @Override
    public SquadGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_squad_group, parent, false);
        return new SquadGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SquadGroupViewHolder holder, int position) {
        SquadGroup group = squadGroups.get(position);
        holder.txtGroupName.setText(group.groupName);
        SquadPlayerAdapter adapter = new SquadPlayerAdapter(group.players, playerClickListener);
        holder.recyclerViewPlayers.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerViewPlayers.setAdapter(adapter);
    }

    @Override
    public int getItemCount() { return squadGroups.size(); }

    static class SquadGroupViewHolder extends RecyclerView.ViewHolder {
        TextView txtGroupName;
        RecyclerView recyclerViewPlayers;

        SquadGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGroupName = itemView.findViewById(R.id.txtGroupName);
            recyclerViewPlayers = itemView.findViewById(R.id.recyclerViewPlayers);
        }
    }

    // Inner adapter for individual players
    static class SquadPlayerAdapter extends RecyclerView.Adapter<SquadPlayerAdapter.PlayerViewHolder> {

        private final List<TeamSquadFragment.SquadItem> players;
        private final OnPlayerClickListener listener;

        public SquadPlayerAdapter(List<TeamSquadFragment.SquadItem> players, OnPlayerClickListener listener) {
            this.players  = players  != null ? players  : new ArrayList<>();
            this.listener = listener;
        }

        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_squad_player, parent, false);
            return new PlayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            TeamSquadFragment.SquadItem player = players.get(position);

            holder.txtPlayerName.setText(player.name);
            holder.txtPlayerNumber.setText(player.number != null && !player.number.isEmpty()
                    ? player.number : "--");
            holder.txtPlayerPosition.setText(player.position != null ? player.position : "");
            holder.txtPlayerNationality.setText(
                    player.nationality != null && !player.nationality.isEmpty()
                            ? player.nationality : "");

            // Use age directly from API (API-Football provides age as int)
            if (player.age > 0) {
                holder.txtPlayerAge.setText(player.age + " y.o.");
            } else if (player.birthDate != null && !player.birthDate.isEmpty()) {
                holder.txtPlayerAge.setText(player.birthDate);
            } else {
                holder.txtPlayerAge.setText("--");
            }

            // Height / weight
            String hw = "";
            if (player.height != null && !player.height.isEmpty()) hw += player.height;
            if (player.weight != null && !player.weight.isEmpty()) {
                if (!hw.isEmpty()) hw += " / ";
                hw += player.weight;
            }
            holder.txtPlayerPhysical.setText(hw.isEmpty() ? "--" : hw);

            // Photo
            if (player.photo != null && !player.photo.isEmpty() && !player.photo.equals("null")) {
                Glide.with(holder.itemView.getContext())
                        .load(player.photo)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .circleCrop()
                        .into(holder.imgPlayerPhoto);
            } else {
                holder.imgPlayerPhoto.setImageResource(R.drawable.ic_placeholder);
            }

            // Click → open player detail
            if (listener != null) {
                holder.itemView.setOnClickListener(v -> listener.onPlayerClick(player));
            }
        }

        @Override
        public int getItemCount() { return players.size(); }

        static class PlayerViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPlayerPhoto;
            TextView txtPlayerNumber;
            TextView txtPlayerName;
            TextView txtPlayerPosition;
            TextView txtPlayerNationality;
            TextView txtPlayerAge;
            TextView txtPlayerPhysical;

            PlayerViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPlayerPhoto = itemView.findViewById(R.id.imgPlayerPhoto);
                txtPlayerNumber = itemView.findViewById(R.id.txtPlayerNumber);
                txtPlayerName = itemView.findViewById(R.id.txtPlayerName);
                txtPlayerPosition = itemView.findViewById(R.id.txtPlayerPosition);
                txtPlayerNationality = itemView.findViewById(R.id.txtPlayerNationality);
                txtPlayerAge = itemView.findViewById(R.id.txtPlayerAge);
                txtPlayerPhysical = itemView.findViewById(R.id.txtPlayerPhysical);
            }
        }
    }

    static class SquadGroup {
        public String groupName;
        public List<TeamSquadFragment.SquadItem> players;

        public SquadGroup(String groupName, List<TeamSquadFragment.SquadItem> players) {
            this.groupName = groupName;
            this.players = players;
        }
    }
}
