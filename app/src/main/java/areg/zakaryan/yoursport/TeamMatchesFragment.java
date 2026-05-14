package areg.zakaryan.yoursport;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamMatchesFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";

    private SearchItem teamItem;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtNoMatches;
    private TeamMatchesAdapter adapter;

    public static TeamMatchesFragment newInstance(SearchItem teamItem) {
        TeamMatchesFragment fragment = new TeamMatchesFragment();
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
        View view = inflater.inflate(R.layout.fragment_team_matches, container, false);
        initViews(view);
        setupRecyclerView();
        loadTeamMatches();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMatches);
        progressBar = view.findViewById(R.id.progressBar);
        txtNoMatches = view.findViewById(R.id.txtNoMatches);
    }

    private void setupRecyclerView() {
        adapter = new TeamMatchesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnMatchClickListener(item -> {
            if (getContext() == null) return;
            MatchItem match = new MatchItem();
            match.matchId = String.valueOf(item.fixtureId);
            match.homeTeam = item.homeTeamName;
            match.awayTeam = item.awayTeamName;
            match.homeLogo = item.homeTeamLogo;
            match.awayLogo = item.awayTeamLogo;
            match.score = item.homeGoals >= 0 && item.awayGoals >= 0
                    ? item.homeGoals + " - " + item.awayGoals : "vs";
            match.status = item.status;
            match.homeTeamId = item.homeTeamId;
            match.awayTeamId = item.awayTeamId;
            Intent intent = new Intent(getContext(), MatchDetailActivity.class);
            intent.putExtra(MatchDetailActivity.EXTRA_MATCH, match);
            startActivity(intent);
        });
    }

    private int getSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        return month < 6 ? year - 1 : year;
    }

    @SuppressWarnings("unchecked")
    private void loadTeamMatches() {
        if (teamItem == null) { showNoMatches(); return; }

        progressBar.setVisibility(View.VISIBLE);
        txtNoMatches.setVisibility(View.GONE);

        ApiClient.getApiService().getTeamMatches(teamItem.id, getSeason()).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) { showNoMatches(); return; }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> matches = (List<Map<String, Object>>) body.get("response");
                    if (matches == null || matches.isEmpty()) { showNoMatches(); return; }

                    List<TeamMatchItem> items = new ArrayList<>();
                    for (Map<String, Object> m : matches) {
                        Map<String, Object> fixture = (Map<String, Object>) m.get("fixture");
                        Map<String, Object> teams = (Map<String, Object>) m.get("teams");
                        Map<String, Object> goals = (Map<String, Object>) m.get("goals");
                        Map<String, Object> league = (Map<String, Object>) m.get("league");
                        if (fixture == null || teams == null) continue;

                        Number fixtureId = (Number) fixture.get("id");
                        String date = String.valueOf(fixture.get("date"));

                        // Status is a Map, not String
                        Map<String, Object> statusMap = (Map<String, Object>) fixture.get("status");
                        String status = statusMap != null ? String.valueOf(statusMap.get("short")) : "";

                        Map<String, Object> home = (Map<String, Object>) teams.get("home");
                        Map<String, Object> away = (Map<String, Object>) teams.get("away");
                        if (home == null || away == null) continue;

                        String homeName = String.valueOf(home.get("name"));
                        String awayName = String.valueOf(away.get("name"));
                        String homeLogo = String.valueOf(home.get("logo"));
                        String awayLogo = String.valueOf(away.get("logo"));
                        int homeId = numInt(home.get("id"));
                        int awayId = numInt(away.get("id"));

                        int hg = goals != null ? numInt(goals.get("home")) : -1;
                        int ag = goals != null ? numInt(goals.get("away")) : -1;

                        String leagueName = league != null ? String.valueOf(league.get("name")) : "";

                        items.add(new TeamMatchItem(
                                fixtureId != null ? fixtureId.intValue() : 0,
                                homeName, awayName, homeLogo, awayLogo,
                                homeId, awayId, hg, ag, date, status, leagueName
                        ));
                    }

                    // Sort newest first by date string (ISO format sorts lexicographically)
                    Collections.sort(items, (a, b) -> {
                        String da = a.date != null ? a.date : "";
                        String db = b.date != null ? b.date : "";
                        return db.compareTo(da); // descending
                    });

                    if (!items.isEmpty()) {
                        adapter.setMatches(items);
                    } else {
                        showNoMatches();
                    }
                } catch (Exception e) {
                    showNoMatches();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                showNoMatches();
            }
        });
    }

    private int numInt(Object v) {
        if (v == null) return -1;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return (int) Math.round(Double.parseDouble(String.valueOf(v))); }
        catch (Exception e) { return -1; }
    }

    private void showNoMatches() {
        txtNoMatches.setVisibility(View.VISIBLE);
        txtNoMatches.setText("No matches available for " + (teamItem != null ? teamItem.title : "this team"));
    }

    // Match item data class
    public static class TeamMatchItem {
        public int fixtureId;
        public String homeTeamName;
        public String awayTeamName;
        public String homeTeamLogo;
        public String awayTeamLogo;
        public int homeTeamId;
        public int awayTeamId;
        public int homeGoals;
        public int awayGoals;
        public String date;
        public String status;
        public String leagueName;

        public TeamMatchItem(int fixtureId, String homeTeamName, String awayTeamName,
                           String homeTeamLogo, String awayTeamLogo, int homeTeamId, int awayTeamId,
                           int homeGoals, int awayGoals, String date, String status, String leagueName) {
            this.fixtureId = fixtureId;
            this.homeTeamName = homeTeamName;
            this.awayTeamName = awayTeamName;
            this.homeTeamLogo = homeTeamLogo;
            this.awayTeamLogo = awayTeamLogo;
            this.homeTeamId = homeTeamId;
            this.awayTeamId = awayTeamId;
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
            this.date = date;
            this.status = status;
            this.leagueName = leagueName;
        }
    }
}
