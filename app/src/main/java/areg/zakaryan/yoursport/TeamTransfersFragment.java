package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamTransfersFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";

    private SearchItem teamItem;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtNoTransfers;
    private TeamTransfersAdapter adapter;

    public static TeamTransfersFragment newInstance(SearchItem teamItem) {
        TeamTransfersFragment fragment = new TeamTransfersFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TEAM_ITEM, teamItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamItem = getArguments().getParcelable(ARG_TEAM_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_team_transfers, container, false);
        initViews(view);
        setupRecyclerView();
        loadTeamTransfers();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewTransfers);
        progressBar = view.findViewById(R.id.progressBar);
        txtNoTransfers = view.findViewById(R.id.txtNoTransfers);
    }

    private void setupRecyclerView() {
        adapter = new TeamTransfersAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @SuppressWarnings("unchecked")
    private void loadTeamTransfers() {
        if (teamItem == null) { showNoTransfers(); return; }

        progressBar.setVisibility(View.VISIBLE);
        txtNoTransfers.setVisibility(View.GONE);

        // Use direct team transfers endpoint (no league/season needed)
        ApiClient.getApiService().getTransfersByTeam(teamItem.id).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) { showNoTransfers(); return; }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> resp = (List<Map<String, Object>>) body.get("response");
                    if (resp == null || resp.isEmpty()) { showNoTransfers(); return; }

                    List<TransferItem> transferItems = new ArrayList<>();

                    /*
                     * API-Football transfers response format:
                     * response[]: { player: {id, name}, update: "...",
                     *   transfers: [{ date, type, teams: { in: {id,name,logo}, out: {id,name,logo} } }]
                     * }
                     */
                    for (Map<String, Object> playerEntry : resp) {
                        Map<String, Object> player = (Map<String, Object>) playerEntry.get("player");
                        if (player == null) continue;

                        int playerId = numInt(player.get("id"));
                        String playerName = str(player.get("name"));

                        List<Map<String, Object>> transfers = (List<Map<String, Object>>) playerEntry.get("transfers");
                        if (transfers == null) continue;

                        for (Map<String, Object> t : transfers) {
                            String date = str(t.get("date"));
                            String type = str(t.get("type"));

                            Map<String, Object> teams = (Map<String, Object>) t.get("teams");
                            if (teams == null) continue;

                            Map<String, Object> teamIn = (Map<String, Object>) teams.get("in");
                            Map<String, Object> teamOut = (Map<String, Object>) teams.get("out");

                            String toTeamName = teamIn != null ? str(teamIn.get("name")) : "";
                            String toTeamLogo = teamIn != null ? str(teamIn.get("logo")) : "";
                            String fromTeamName = teamOut != null ? str(teamOut.get("name")) : "";
                            String fromTeamLogo = teamOut != null ? str(teamOut.get("logo")) : "";

                            // Determine direction relative to our team
                            int toId = teamIn != null ? numInt(teamIn.get("id")) : 0;
                            boolean isIncoming = (toId == teamItem.id);
                            String direction = isIncoming ? "in" : "out";
                            if (type.toLowerCase().contains("loan")) direction = type;

                            // Only include recent transfers (last 2 years)
                            if (isRecent(date, 24)) {
                                transferItems.add(new TransferItem(
                                        playerId, playerName, "",
                                        fromTeamName, fromTeamLogo,
                                        toTeamName, toTeamLogo,
                                        date, direction, type
                                ));
                            }
                        }
                    }

                    // Sort by date descending (newest first)
                    Collections.sort(transferItems, (a, b) -> b.transferDate.compareTo(a.transferDate));

                    // Limit to 30 most recent
                    if (transferItems.size() > 30) {
                        transferItems = new ArrayList<>(transferItems.subList(0, 30));
                    }

                    if (!transferItems.isEmpty()) {
                        adapter.setTransfers(transferItems);
                    } else {
                        showNoTransfers();
                    }
                } catch (Exception e) {
                    showNoTransfers();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                showNoTransfers();
            }
        });
    }

    private boolean isRecent(String dateStr, int months) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(dateStr);
            long cutoff = System.currentTimeMillis() - (long) months * 30L * 24 * 60 * 60 * 1000;
            return d != null && d.getTime() > cutoff;
        } catch (Exception e) { return false; }
    }

    private int numInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return (int) Math.round(Double.parseDouble(String.valueOf(v))); }
        catch (Exception e) { return 0; }
    }

    private String str(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equals(s) ? "" : s;
    }

    private void showNoTransfers() {
        txtNoTransfers.setVisibility(View.VISIBLE);
        txtNoTransfers.setText("No recent transfers for " + (teamItem != null ? teamItem.title : "this team"));
    }

    // Transfer item data class
    public static class TransferItem {
        public int playerId;
        public String playerName;
        public String playerPhoto;
        public String fromTeamName;
        public String fromTeamLogo;
        public String toTeamName;
        public String toTeamLogo;
        public String transferDate;
        public String transferType;
        public String leagueName;

        public TransferItem(int playerId, String playerName, String playerPhoto,
                          String fromTeamName, String fromTeamLogo, String toTeamName, String toTeamLogo,
                          String transferDate, String transferType, String leagueName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.playerPhoto = playerPhoto;
            this.fromTeamName = fromTeamName;
            this.fromTeamLogo = fromTeamLogo;
            this.toTeamName = toTeamName;
            this.toTeamLogo = toTeamLogo;
            this.transferDate = transferDate;
            this.transferType = transferType;
            this.leagueName = leagueName;
        }

        public boolean isIncoming() {
            return "in".equalsIgnoreCase(transferType);
        }

        public boolean isOutgoing() {
            return "out".equalsIgnoreCase(transferType);
        }

        public boolean isLoan() {
            return transferType != null && transferType.toLowerCase().contains("loan");
        }
    }
}
