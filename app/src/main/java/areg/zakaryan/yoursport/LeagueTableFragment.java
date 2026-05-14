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

import areg.zakaryan.yoursport.api.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeagueTableFragment extends Fragment {

    private static final String ARG_LEAGUE_ID = "league_id";

    private int leagueId;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TeamSeasonAdapter adapter;

    public static LeagueTableFragment newInstance(int leagueId) {
        LeagueTableFragment f = new LeagueTableFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LEAGUE_ID, leagueId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) leagueId = getArguments().getInt(ARG_LEAGUE_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_league_table, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTable);
        progressBar  = view.findViewById(R.id.progressBar);
        tvEmpty      = view.findViewById(R.id.tvEmpty);

        adapter = new TeamSeasonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadStandings();
        return view;
    }

    private int getSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        return cal.get(Calendar.MONTH) < 6 ? year - 1 : year;
    }

    @SuppressWarnings("unchecked")
    private void loadStandings() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        int season = getSeason();
        ApiClient.getApiService().getStandings(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty("No standings available");
                    return;
                }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> resp = (List<Map<String, Object>>) body.get("response");

                    if (resp == null || resp.isEmpty()) {
                        showEmpty("No standings available");
                        return;
                    }

                    List<TeamSeasonFragment.LeagueTableItem> tables = new ArrayList<>();

                    Map<String, Object> first = resp.get(0);
                    Map<String, Object> league = (Map<String, Object>) first.get("league");

                    if (league != null) {
                        String name = str(league.get("name"));
                        List<?> outer = (List<?>) league.get("standings");

                        if (outer != null && !outer.isEmpty()) {
                            for (Object groupObj : outer) {
                                List<Map<String, Object>> group = (List<Map<String, Object>>) groupObj;
                                if (group == null || group.isEmpty()) continue;

                                List<TeamSeasonFragment.TablePosition> positions = new ArrayList<>();
                                String groupName = name;

                                for (Map<String, Object> row : group) {
                                    String groupStr = str(row.get("group"));
                                    if (!groupStr.isEmpty() && outer.size() > 1) {
                                        groupName = name + " · " + groupStr;
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

                                    positions.add(new TeamSeasonFragment.TablePosition(
                                            numInt(row.get("rank")), teamId, tName, tLogo,
                                            played, won, draw, lost, gf, ga, gf - ga,
                                            numInt(row.get("points"))
                                    ));
                                }

                                if (!positions.isEmpty()) {
                                    tables.add(new TeamSeasonFragment.LeagueTableItem(
                                            groupName, String.valueOf(season), positions));
                                }
                            }
                        }
                    }

                    if (tables.isEmpty()) {
                        showEmpty("No standings available");
                    } else {
                        adapter.setLeagueTables(tables);
                    }

                } catch (Exception e) {
                    showEmpty("Failed to parse standings");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Failed to load standings");
                }
            }
        });
    }

    private void showEmpty(String msg) {
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(View.VISIBLE);
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
}
