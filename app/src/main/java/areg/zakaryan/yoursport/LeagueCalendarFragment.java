package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
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
import areg.zakaryan.yoursport.model.MatchItem;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeagueCalendarFragment extends Fragment {

    private static final String ARG_LEAGUE_ID   = "league_id";
    private static final String ARG_LEAGUE_NAME = "league_name";
    private static final String ARG_LEAGUE_LOGO = "league_logo";

    private int leagueId;
    private String leagueName;
    private String leagueLogo;

    private HorizontalScrollView hsvRounds;
    private LinearLayout llRounds;
    private RecyclerView rvMatches;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private MatchesAdapter adapter;
    private final List<String> roundList = new ArrayList<>();
    private final List<View> roundViews = new ArrayList<>();
    private int selectedRoundIndex = -1;

    public static LeagueCalendarFragment newInstance(int leagueId, String leagueName, String leagueLogo) {
        LeagueCalendarFragment f = new LeagueCalendarFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LEAGUE_ID, leagueId);
        args.putString(ARG_LEAGUE_NAME, leagueName);
        args.putString(ARG_LEAGUE_LOGO, leagueLogo);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            leagueId   = getArguments().getInt(ARG_LEAGUE_ID);
            leagueName = getArguments().getString(ARG_LEAGUE_NAME);
            leagueLogo = getArguments().getString(ARG_LEAGUE_LOGO);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_league_calendar, container, false);

        hsvRounds   = view.findViewById(R.id.hsvRounds);
        llRounds    = view.findViewById(R.id.llRounds);
        rvMatches   = view.findViewById(R.id.rvMatches);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty     = view.findViewById(R.id.tvEmpty);

        rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MatchesAdapter();

        adapter.setOnMatchClickListener(match -> {
            Intent intent = new Intent(requireContext(), MatchDetailActivity.class);
            intent.putExtra(MatchDetailActivity.EXTRA_MATCH, match);
            startActivity(intent);
        });

        adapter.setOnTeamClickListener((teamId, teamName, teamLogo) -> {
            SearchItem teamItem = new SearchItem(
                    SearchItem.TYPE_ITEM, teamName, "", teamLogo, teamId, "team"
            );
            Intent intent = new Intent(requireContext(), TeamDetailActivity.class);
            intent.putExtra("team_item", teamItem);
            startActivity(intent);
        });

        rvMatches.setAdapter(adapter);

        loadRounds();
        return view;
    }

    private int getSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        return cal.get(Calendar.MONTH) < 6 ? year - 1 : year;
    }

    @SuppressWarnings("unchecked")
    private void loadRounds() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        int season = getSeason();
        ApiClient.getApiService().getRounds(leagueId, season).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty("No rounds available");
                    return;
                }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<String> rounds = (List<String>) body.get("response");

                    if (rounds == null || rounds.isEmpty()) {
                        showEmpty("No rounds available");
                        return;
                    }

                    roundList.clear();
                    roundList.addAll(rounds);
                    buildRoundTabs();

                    // Find the latest round that has been played (select last available)
                    // We'll select the last round by default, user can scroll
                    int defaultIndex = findCurrentRound(rounds);
                    selectRound(defaultIndex);

                    // Scroll to selected round
                    hsvRounds.post(() -> {
                        if (!isAdded() || roundViews.isEmpty()) return;
                        View selected = roundViews.get(defaultIndex);
                        int scrollX = selected.getLeft() - hsvRounds.getWidth() / 2 + selected.getWidth() / 2;
                        hsvRounds.smoothScrollTo(Math.max(0, scrollX), 0);
                    });

                } catch (Exception e) {
                    showEmpty("Failed to load rounds");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Failed to load rounds");
                }
            }
        });
    }

    private int findCurrentRound(List<String> rounds) {
        // Default to last round (most recent)
        // A smarter approach would be to check which round has the latest matches
        // For now, pick a middle-to-end round
        if (rounds.size() <= 1) return 0;
        return Math.max(0, rounds.size() - 1);
    }

    private void buildRoundTabs() {
        llRounds.removeAllViews();
        roundViews.clear();

        for (int i = 0; i < roundList.size(); i++) {
            View roundView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_round_tab, llRounds, false);

            String roundName = roundList.get(i);
            // Shorten display: "Regular Season - 1" -> "Round 1"
            String displayName = formatRoundName(roundName);
            ((TextView) roundView.findViewById(R.id.tv_round_name)).setText(displayName);

            final int idx = i;
            roundView.setOnClickListener(v -> selectRound(idx));

            llRounds.addView(roundView);
            roundViews.add(roundView);
        }
    }

    private String formatRoundName(String raw) {
        if (raw == null) return "";
        // API returns e.g. "Regular Season - 17"
        if (raw.contains(" - ")) {
            String[] parts = raw.split(" - ");
            String num = parts[parts.length - 1].trim();
            try {
                Integer.parseInt(num);
                return "Round " + num;
            } catch (NumberFormatException e) {
                return raw;
            }
        }
        return raw;
    }

    private void selectRound(int index) {
        if (index < 0 || index >= roundViews.size()) return;

        // Deselect previous
        if (selectedRoundIndex >= 0 && selectedRoundIndex < roundViews.size()) {
            roundViews.get(selectedRoundIndex).setBackgroundResource(R.drawable.bg_date_tab_normal);
        }

        selectedRoundIndex = index;
        roundViews.get(index).setBackgroundResource(R.drawable.bg_date_tab_selected);

        loadMatchesForRound(roundList.get(index));
    }

    @SuppressWarnings("unchecked")
    private void loadMatchesForRound(String round) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvMatches.setVisibility(View.GONE);

        int season = getSeason();
        ApiClient.getApiService().getFixturesByRound(leagueId, season, round)
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty("No matches for this round");
                    return;
                }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<?> fixtures = (List<?>) body.get("response");

                    if (fixtures == null || fixtures.isEmpty()) {
                        showEmpty("No matches for this round");
                        return;
                    }

                    List<MatchItem> matches = new ArrayList<>();
                    for (Object obj : fixtures) {
                        if (!(obj instanceof Map)) continue;
                        Map<String, Object> m = (Map<String, Object>) obj;
                        MatchItem item = parseFixture(m);
                        if (item != null) matches.add(item);
                    }

                    // Sort by date/time
                    matches.sort((a, b) -> {
                        String ta = a.time != null ? a.time : "";
                        String tb = b.time != null ? b.time : "";
                        return ta.compareTo(tb);
                    });

                    if (matches.isEmpty()) {
                        showEmpty("No matches for this round");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvMatches.setVisibility(View.VISIBLE);
                        adapter.submitList(matches);
                    }

                } catch (Exception e) {
                    showEmpty("Failed to parse matches");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    showEmpty("Failed to load matches");
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private MatchItem parseFixture(Map<String, Object> m) {
        Map<String, Object> fixture = castMap(m.get("fixture"));
        Map<String, Object> teams   = castMap(m.get("teams"));
        Map<String, Object> goals   = castMap(m.get("goals"));
        if (fixture == null || teams == null) return null;

        MatchItem item = new MatchItem();
        item.leagueId   = leagueId;
        item.leagueID   = leagueId;
        item.leagueName = leagueName;
        item.leagueLogo = leagueLogo;

        Map<String, Object> homeObj = castMap(teams.get("home"));
        Map<String, Object> awayObj = castMap(teams.get("away"));
        if (homeObj != null) {
            item.homeTeam   = str(homeObj.get("name"));
            item.homeLogo   = str(homeObj.get("logo"));
            item.homeTeamId = numInt(homeObj.get("id"));
        }
        if (awayObj != null) {
            item.awayTeam   = str(awayObj.get("name"));
            item.awayLogo   = str(awayObj.get("logo"));
            item.awayTeamId = numInt(awayObj.get("id"));
        }

        item.time    = str(fixture.get("date"));
        item.matchId = String.valueOf(numInt(fixture.get("id")));

        // Extract date from ISO format
        if (item.time != null && item.time.contains("T")) {
            item.date = item.time.substring(0, 10);
        }

        String status = str(getNestedValue(fixture, "status", "short"));
        item.status = status;
        item.isLive = "1H".equals(status) || "2H".equals(status) || "HT".equals(status)
                || "ET".equals(status) || "P".equals(status);

        if (goals != null) {
            String h = parseScore(goals.get("home"));
            String a = parseScore(goals.get("away"));
            if (!h.isEmpty() && !a.isEmpty()) {
                item.score = h + " - " + a;
            }
        }

        return (item.homeTeam != null && !item.homeTeam.isEmpty()) ? item : null;
    }

    private void showEmpty(String msg) {
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(View.VISIBLE);
        rvMatches.setVisibility(View.GONE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> root, String k1, String k2) {
        Map<String, Object> nested = castMap(root.get(k1));
        return nested == null ? null : nested.get(k2);
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

    private String parseScore(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        if ("null".equals(s)) return "";
        try { return String.valueOf((int) Double.parseDouble(s)); }
        catch (NumberFormatException e) { return s; }
    }
}
