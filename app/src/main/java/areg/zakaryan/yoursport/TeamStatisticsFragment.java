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

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamStatisticsFragment extends Fragment {

    private static final String ARG_TEAM_ITEM = "team_item";
    private SearchItem teamItem;

    private ProgressBar progressBar;
    private TextView txtNoStatistics;

    // Results
    private TextView valPlayed, valWon, valDraw, valLost, valWinRate, valForm;
    // Attack
    private TextView valGoalsFor, valGoalsPerGame, valBiggestWin, valPenalty, valFailedScore;
    // Defense
    private TextView valGoalsAgainst, valGoalsAgainstAvg, valCleanSheets, valBiggestLoss;
    // Discipline
    private TextView valYellow, valRed;

    public static TeamStatisticsFragment newInstance(SearchItem teamItem) {
        TeamStatisticsFragment f = new TeamStatisticsFragment();
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
        View v = inflater.inflate(R.layout.fragment_team_statistics, container, false);

        progressBar    = v.findViewById(R.id.progressBar);
        txtNoStatistics = v.findViewById(R.id.txtNoStatistics);

        valPlayed  = v.findViewById(R.id.valPlayed);
        valWon     = v.findViewById(R.id.valWon);
        valDraw    = v.findViewById(R.id.valDraw);
        valLost    = v.findViewById(R.id.valLost);
        valWinRate = v.findViewById(R.id.valWinRate);
        valForm    = v.findViewById(R.id.valForm);

        valGoalsFor    = v.findViewById(R.id.valGoalsFor);
        valGoalsPerGame = v.findViewById(R.id.valGoalsPerGame);
        valBiggestWin  = v.findViewById(R.id.valBiggestWin);
        valPenalty     = v.findViewById(R.id.valPenalty);
        valFailedScore = v.findViewById(R.id.valFailedScore);

        valGoalsAgainst    = v.findViewById(R.id.valGoalsAgainst);
        valGoalsAgainstAvg = v.findViewById(R.id.valGoalsAgainstAvg);
        valCleanSheets     = v.findViewById(R.id.valCleanSheets);
        valBiggestLoss     = v.findViewById(R.id.valBiggestLoss);

        valYellow = v.findViewById(R.id.valYellow);
        valRed    = v.findViewById(R.id.valRed);

        loadTeamStatistics();
        return v;
    }

    private int getSeason() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        return c.get(Calendar.MONTH) < 6 ? year - 1 : year;
    }

    @SuppressWarnings("unchecked")
    private void loadTeamStatistics() {
        if (teamItem == null) { showError(); return; }
        progressBar.setVisibility(View.VISIBLE);

        int season = getSeason();

        // Step 1: find a league ID from team fixtures
        ApiClient.getApiService().getTeamMatches(teamItem.id, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) { showError(); return; }
                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> fixtures = (List<Map<String, Object>>) body.get("response");
                    int leagueId = 0;
                    if (fixtures != null) {
                        for (Map<String, Object> fix : fixtures) {
                            Map<String, Object> lg = castMap(fix.get("league"));
                            if (lg != null) { leagueId = numInt(lg.get("id")); if (leagueId > 0) break; }
                        }
                    }
                    if (leagueId <= 0) { showError(); return; }
                    fetchStats(leagueId, season);
                } catch (Exception e) { showError(); }
            }
            @Override public void onFailure(Call<Object> call, Throwable t) { if (isAdded()) showError(); }
        });
    }

    @SuppressWarnings("unchecked")
    private void fetchStats(int leagueId, int season) {
        ApiClient.getApiService().getTeamStatistics(leagueId, season, teamItem.id)
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) { showError(); return; }
                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    Object raw = body.get("response");
                    Map<String, Object> stats;
                    if (raw instanceof Map)       stats = (Map<String, Object>) raw;
                    else if (raw instanceof List) { List<?> l = (List<?>) raw; if (l.isEmpty()) { showError(); return; } stats = (Map<String, Object>) l.get(0); }
                    else { showError(); return; }
                    populate(stats);
                } catch (Exception e) { showError(); }
            }
            @Override public void onFailure(Call<Object> call, Throwable t) { if (isAdded()) { progressBar.setVisibility(View.GONE); showError(); } }
        });
    }

    @SuppressWarnings("unchecked")
    private void populate(Map<String, Object> s) {
        // ── Results ──────────────────────────────────────────────────────────
        Map<String, Object> fix = castMap(s.get("fixtures"));
        int played = 0, wins = 0, draws = 0, loses = 0;
        if (fix != null) {
            played = numInt(castMap(fix.get("played")), "total");
            wins   = numInt(castMap(fix.get("wins")),   "total");
            draws  = numInt(castMap(fix.get("draws")),  "total");
            loses  = numInt(castMap(fix.get("loses")),  "total");
        }
        set(valPlayed,  String.valueOf(played));
        set(valWon,     String.valueOf(wins));
        set(valDraw,    String.valueOf(draws));
        set(valLost,    String.valueOf(loses));
        set(valWinRate, played > 0 ? String.format("%.1f%%", (float) wins / played * 100) : "0%");
        String form = str(s.get("form"));
        set(valForm, form.isEmpty() ? "—" : form);

        // ── Attack ───────────────────────────────────────────────────────────
        Map<String, Object> goals = castMap(s.get("goals"));
        String goalsFor = "0", avgFor = "0";
        if (goals != null) {
            Map<String, Object> gf = castMap(goals.get("for"));
            if (gf != null) {
                goalsFor = String.valueOf(numInt(castMap(gf.get("total")), "total"));
                avgFor   = str(castMap(gf.get("average")), "total");
            }
        }
        set(valGoalsFor,    goalsFor);
        set(valGoalsPerGame, avgFor.isEmpty() ? "0" : avgFor);

        Map<String, Object> biggest = castMap(s.get("biggest"));
        String bigWin = "—";
        if (biggest != null) {
            Map<String, Object> bw = castMap(biggest.get("wins"));
            if (bw != null) bigWin = combine(str(bw.get("home")), str(bw.get("away")));
        }
        set(valBiggestWin, bigWin);

        Map<String, Object> penalty = castMap(s.get("penalty"));
        String penPct = "—";
        if (penalty != null) {
            Map<String, Object> scored = castMap(penalty.get("scored"));
            if (scored != null) { String p = str(scored.get("percentage")); if (!p.isEmpty()) penPct = p; }
        }
        set(valPenalty, penPct);

        Map<String, Object> fts = castMap(s.get("failed_to_score"));
        set(valFailedScore, fts != null ? numInt(fts.get("total")) + " games" : "0 games");

        // ── Defense ──────────────────────────────────────────────────────────
        String goalsAgainst = "0", avgAgainst = "0";
        if (goals != null) {
            Map<String, Object> ga = castMap(goals.get("against"));
            if (ga != null) {
                goalsAgainst = String.valueOf(numInt(castMap(ga.get("total")), "total"));
                avgAgainst   = str(castMap(ga.get("average")), "total");
            }
        }
        set(valGoalsAgainst,    goalsAgainst);
        set(valGoalsAgainstAvg, avgAgainst.isEmpty() ? "0" : avgAgainst);

        Map<String, Object> cs = castMap(s.get("clean_sheet"));
        set(valCleanSheets, cs != null ? String.valueOf(numInt(cs.get("total"))) : "0");

        String bigLoss = "—";
        if (biggest != null) {
            Map<String, Object> bl = castMap(biggest.get("loses"));
            if (bl != null) bigLoss = combine(str(bl.get("home")), str(bl.get("away")));
        }
        set(valBiggestLoss, bigLoss);

        // ── Discipline ───────────────────────────────────────────────────────
        Map<String, Object> cards = castMap(s.get("cards"));
        int yellow = 0, red = 0;
        if (cards != null) {
            yellow = sumCards(castMap(cards.get("yellow")));
            red    = sumCards(castMap(cards.get("red")));
        }
        set(valYellow, String.valueOf(yellow));
        set(valRed,    String.valueOf(red));

        txtNoStatistics.setVisibility(View.GONE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private void set(TextView tv, String val) { if (tv != null) tv.setText(val); }

    private String combine(String a, String b) {
        boolean aEmpty = a == null || a.isEmpty() || "null".equals(a);
        boolean bEmpty = b == null || b.isEmpty() || "null".equals(b);
        if (aEmpty && bEmpty) return "—";
        if (aEmpty) return b;
        if (bEmpty) return a;
        return a + " / " + b;
    }

    @SuppressWarnings("unchecked")
    private int sumCards(Map<String, Object> m) {
        if (m == null) return 0;
        int t = 0;
        for (Object v : m.values()) { Map<String, Object> e = castMap(v); if (e != null) t += numInt(e.get("total")); }
        return t;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object v) { return v instanceof Map ? (Map<String, Object>) v : null; }

    private int numInt(Map<String, Object> m, String key) { return m == null ? 0 : numInt(m.get(key)); }
    private String str(Map<String, Object> m, String key) { return m == null ? "" : str(m.get(key)); }
    private int numInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return (int) Math.round(Double.parseDouble(String.valueOf(v))); } catch (Exception e) { return 0; }
    }
    private String str(Object v) { if (v == null) return ""; String s = String.valueOf(v); return "null".equals(s) ? "" : s; }

    private void showError() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.GONE);
        txtNoStatistics.setVisibility(View.VISIBLE);
        txtNoStatistics.setText("No statistics available for " + (teamItem != null ? teamItem.title : "this team"));
    }
}
