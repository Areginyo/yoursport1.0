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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamSeasonFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";

    private SearchItem teamItem;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtNoTables;
    private TeamSeasonAdapter adapter;

    public static TeamSeasonFragment newInstance(SearchItem teamItem) {
        TeamSeasonFragment f = new TeamSeasonFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TEAM_ITEM, teamItem);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) teamItem = getArguments().getParcelable(ARG_TEAM_ITEM);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_team_season, container, false);
        recyclerView  = view.findViewById(R.id.recyclerViewTables);
        progressBar   = view.findViewById(R.id.progressBar);
        txtNoTables   = view.findViewById(R.id.txtNoTables);

        adapter = new TeamSeasonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (teamItem == null) { showNoTables(); return view; }
        progressBar.setVisibility(View.VISIBLE);
        discoverLeagues();
        return view;
    }

    private int getSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        return cal.get(Calendar.MONTH) < 6 ? year - 1 : year;
    }

    // ── Step 1: Discover all leagues the team plays in this season ─────────
    @SuppressWarnings("unchecked")
    private void discoverLeagues() {
        int season = getSeason();
        ApiClient.getApiService().getLeaguesByTeam(teamItem.id, season)
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showNoTables(); return;
                }
                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> resp = (List<Map<String, Object>>) body.get("response");
                    if (resp == null || resp.isEmpty()) { showNoTables(); return; }

                    // Collect all league ids + names
                    List<Integer> ids   = new ArrayList<>();
                    List<String>  names = new ArrayList<>();

                    for (Map<String, Object> entry : resp) {
                        Map<String, Object> lg = (Map<String, Object>) entry.get("league");
                        if (lg == null) continue;
                        int lid = numInt(lg.get("id"));
                        if (lid <= 0) continue;
                        ids.add(lid);
                        names.add(str(lg.get("name")));
                    }

                    if (ids.isEmpty()) { showNoTables(); return; }

                    // ── Step 2: Load standings for all leagues in parallel ──
                    List<LeagueTableItem> results = new ArrayList<>();
                    AtomicInteger pending = new AtomicInteger(ids.size());

                    for (int i = 0; i < ids.size(); i++) {
                        loadStandings(ids.get(i), names.get(i), season, results, pending);
                    }

                } catch (Exception e) { showNoTables(); }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) showNoTables();
            }
        });
    }

    // ── Step 2: Load standings for one league, accumulate into results ─────
    @SuppressWarnings("unchecked")
    private void loadStandings(int leagueId, String leagueName, int season,
                               List<LeagueTableItem> results, AtomicInteger pending) {
        ApiClient.getApiService().getStandings(leagueId, season)
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> resp =
                                (List<Map<String, Object>>) body.get("response");

                        if (resp != null && !resp.isEmpty()) {
                            Map<String, Object> first = resp.get(0);
                            Map<String, Object> league = (Map<String, Object>) first.get("league");

                            if (league != null) {
                                String name = str(league.get("name"));
                                if (name.isEmpty()) name = leagueName;

                                // standings is List<List<Map>>  (groups)
                                List<?> outer = (List<?>) league.get("standings");
                                if (outer != null && !outer.isEmpty()) {
                                    // iterate ALL groups (e.g. Champions League has 8 groups)
                                    for (Object groupObj : outer) {
                                        List<Map<String, Object>> group =
                                                (List<Map<String, Object>>) groupObj;
                                        if (group == null || group.isEmpty()) continue;

                                        List<TablePosition> positions = new ArrayList<>();
                                        String groupName = name; // default

                                        for (Map<String, Object> row : group) {
                                            // Extract group name from first row description
                                            String desc = str(row.get("description"));
                                            if (!desc.isEmpty() && outer.size() > 1) {
                                                // multi-group competition
                                                String groupStr = str(row.get("group"));
                                                if (!groupStr.isEmpty()) groupName = name + " · " + groupStr;
                                            }

                                            Map<String, Object> team = (Map<String, Object>) row.get("team");
                                            int teamId   = team != null ? numInt(team.get("id"))   : 0;
                                            String tName = team != null ? str(team.get("name"))    : "";
                                            String tLogo = team != null ? str(team.get("logo"))    : "";

                                            Map<String, Object> all = (Map<String, Object>) row.get("all");
                                            int played = 0, won = 0, draw = 0, lost = 0, gf = 0, ga = 0;
                                            if (all != null) {
                                                played = numInt(all.get("played"));
                                                won    = numInt(all.get("win"));
                                                draw   = numInt(all.get("draw"));
                                                lost   = numInt(all.get("lose"));
                                                Map<String, Object> goals = (Map<String, Object>) all.get("goals");
                                                if (goals != null) {
                                                    gf = numInt(goals.get("for"));
                                                    ga = numInt(goals.get("against"));
                                                }
                                            }
                                            positions.add(new TablePosition(
                                                    numInt(row.get("rank")), teamId, tName, tLogo,
                                                    played, won, draw, lost, gf, ga, gf - ga,
                                                    numInt(row.get("points"))
                                            ));
                                        }

                                        if (!positions.isEmpty()) {
                                            results.add(new LeagueTableItem(
                                                    groupName, String.valueOf(season), positions));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // When all leagues are done, show results
                if (pending.decrementAndGet() == 0) {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    if (!results.isEmpty()) {
                        adapter.setLeagueTables(results);
                    } else {
                        showNoTables();
                    }
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded() && pending.decrementAndGet() == 0) {
                    progressBar.setVisibility(View.GONE);
                    if (!results.isEmpty()) adapter.setLeagueTables(results);
                    else showNoTables();
                }
            }
        });
    }

    private void showNoTables() {
        progressBar.setVisibility(View.GONE);
        txtNoTables.setVisibility(View.VISIBLE);
        txtNoTables.setText("No league tables available for " +
                (teamItem != null ? teamItem.title : "this team"));
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

    // ── Data classes ───────────────────────────────────────────────────────
    public static class LeagueTableItem {
        public String leagueName, season;
        public List<TablePosition> positions;

        public LeagueTableItem(String leagueName, String season, List<TablePosition> positions) {
            this.leagueName = leagueName; this.season = season; this.positions = positions;
        }
    }

    public static class TablePosition {
        public int rank, teamId, played, won, draw, lost, goalsFor, goalsAgainst, goalDiff, points;
        public String teamName, teamLogo;

        public TablePosition(int rank, int teamId, String teamName, String teamLogo,
                            int played, int won, int draw, int lost,
                            int goalsFor, int goalsAgainst, int goalDiff, int points) {
            this.rank = rank; this.teamId = teamId; this.teamName = teamName;
            this.teamLogo = teamLogo; this.played = played; this.won = won;
            this.draw = draw; this.lost = lost; this.goalsFor = goalsFor;
            this.goalsAgainst = goalsAgainst; this.goalDiff = goalDiff; this.points = points;
        }
    }
}
