package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchStatsFragment extends Fragment {

    private static final String ARG_MATCH = "match";
    private static final int STAT_NOT_AVAILABLE = Integer.MIN_VALUE;

    public static MatchStatsFragment newInstance(MatchItem match) {
        MatchStatsFragment fragment = new MatchStatsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MATCH, match);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match_stats, container, false);
        MatchItem match = getArguments() != null ? getArguments().getParcelable(ARG_MATCH) : null;
        TextView tvStats = view.findViewById(R.id.tv_stats_content);
        LinearLayout graphsLayout = view.findViewById(R.id.layout_stats_graphs);

        tvStats.setText(buildFallbackStats(match));
        graphsLayout.setVisibility(View.GONE);

        if (match == null || match.matchId == null || match.matchId.trim().isEmpty()) {
            tvStats.setText("No statistics available.");
            return view;
        }

        ApiClient.getApiService().getFixtureStatistics(match.matchId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                boolean hasDetailedStats = bindGraphStats(view, response.body());
                if (hasDetailedStats) {
                    graphsLayout.setVisibility(View.VISIBLE);
                    tvStats.setText("Match analytics");
                } else {
                    graphsLayout.setVisibility(View.GONE);
                    tvStats.setText("Detailed statistics are not available for this match yet.");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Keep fallback stats when detailed endpoint is unavailable.
            }
        });

        return view;
    }

    private String valueOrDash(Object val) {
        if (val == null) return "—";
        String s = String.valueOf(val);
        if (s.trim().isEmpty() || "null".equalsIgnoreCase(s)) return "—";
        return s;
    }

    private String buildFallbackStats(MatchItem match) {
        if (match == null) return "No statistics available.";
        StringBuilder sb = new StringBuilder();
        sb.append("Match status: ").append(valueOrDash(match.status)).append("\n");
        sb.append("Score: ").append(valueOrDash(match.score)).append("\n");
        sb.append("Kick-off (UTC): ").append(valueOrDash(match.time)).append("\n");
        sb.append("Live: ").append(match.isLive ? "Yes" : "No");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean bindGraphStats(View root, Object raw) {
        if (!(raw instanceof Map)) return false;
        Map<String, Object> body = (Map<String, Object>) raw;
        Object responseObj = body.get("response");
        if (!(responseObj instanceof java.util.List) || ((java.util.List<?>) responseObj).size() < 2) return false;

        Map<String, Object> home = (Map<String, Object>) ((java.util.List<?>) responseObj).get(0);
        Map<String, Object> away = (Map<String, Object>) ((java.util.List<?>) responseObj).get(1);
        Map<String, Integer> homeStats = parseStatsMap(home);
        Map<String, Integer> awayStats = parseStatsMap(away);
        if (homeStats.isEmpty() && awayStats.isEmpty()) return false;

        setDualStatOrHide(
                root,
                R.id.row_possession,
                R.id.tv_possession_values,
                R.id.progress_possession_home,
                R.id.progress_possession_away,
                stat(homeStats, "Ball Possession"),
                stat(awayStats, "Ball Possession"),
                true
        );
        setDualStatOrHide(
                root,
                R.id.row_total_passes,
                R.id.tv_total_passes_values,
                R.id.progress_total_passes_home,
                R.id.progress_total_passes_away,
                stat(homeStats, "Total passes"),
                stat(awayStats, "Total passes"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_pass_accuracy,
                R.id.tv_pass_accuracy_values,
                R.id.progress_pass_accuracy_home,
                R.id.progress_pass_accuracy_away,
                stat(homeStats, "Passes %"),
                stat(awayStats, "Passes %"),
                true
        );
        setDualStatOrHide(
                root,
                R.id.row_shots,
                R.id.tv_shots_values,
                R.id.progress_shots_home,
                R.id.progress_shots_away,
                stat(homeStats, "Total Shots"),
                stat(awayStats, "Total Shots"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_on_target,
                R.id.tv_on_target_values,
                R.id.progress_on_target_home,
                R.id.progress_on_target_away,
                stat(homeStats, "Shots on Goal"),
                stat(awayStats, "Shots on Goal"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_corners,
                R.id.tv_corners_values,
                R.id.progress_corners_home,
                R.id.progress_corners_away,
                stat(homeStats, "Corner Kicks"),
                stat(awayStats, "Corner Kicks"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_offsides,
                R.id.tv_offsides_values,
                R.id.progress_offsides_home,
                R.id.progress_offsides_away,
                stat(homeStats, "Offsides"),
                stat(awayStats, "Offsides"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_fouls,
                R.id.tv_fouls_values,
                R.id.progress_fouls_home,
                R.id.progress_fouls_away,
                stat(homeStats, "Fouls"),
                stat(awayStats, "Fouls"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_goalkeeper_saves,
                R.id.tv_goalkeeper_saves_values,
                R.id.progress_goalkeeper_saves_home,
                R.id.progress_goalkeeper_saves_away,
                stat(homeStats, "Goalkeeper Saves"),
                stat(awayStats, "Goalkeeper Saves"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_yellow,
                R.id.tv_yellow_values,
                R.id.progress_yellow_home,
                R.id.progress_yellow_away,
                stat(homeStats, "Yellow Cards"),
                stat(awayStats, "Yellow Cards"),
                false
        );
        setDualStatOrHide(
                root,
                R.id.row_red,
                R.id.tv_red_values,
                R.id.progress_red_home,
                R.id.progress_red_away,
                stat(homeStats, "Red Cards"),
                stat(awayStats, "Red Cards"),
                false
        );
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseStatsMap(Map<String, Object> teamStats) {
        HashMap<String, Integer> map = new HashMap<>();
        if (teamStats == null) return map;
        Object statsObj = teamStats.get("statistics");
        if (!(statsObj instanceof java.util.List)) return map;
        for (Object statObj : (java.util.List<?>) statsObj) {
            if (!(statObj instanceof Map)) continue;
            Map<String, Object> stat = (Map<String, Object>) statObj;
            String type = String.valueOf(stat.get("type"));
            int value = toInt(stat.get("value"));
            map.put(type, value);
        }
        return map;
    }

    private int stat(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value == null ? STAT_NOT_AVAILABLE : value;
    }

    private void setDualStatOrHide(
            View root,
            int rowId,
            int labelId,
            int homeProgressId,
            int awayProgressId,
            int home,
            int away,
            boolean showAsPercent
    ) {
        View row = root.findViewById(rowId);
        TextView tv = root.findViewById(labelId);
        LinearProgressIndicator homeBar = root.findViewById(homeProgressId);
        LinearProgressIndicator awayBar = root.findViewById(awayProgressId);

        if (home == STAT_NOT_AVAILABLE && away == STAT_NOT_AVAILABLE) {
            if (row != null) row.setVisibility(View.GONE);
            return;
        }
        if (row != null) row.setVisibility(View.VISIBLE);

        int safeHome = home == STAT_NOT_AVAILABLE ? 0 : home;
        int safeAway = away == STAT_NOT_AVAILABLE ? 0 : away;
        int total = Math.max(1, safeHome + safeAway);
        int homePercent = Math.round((safeHome * 100f) / total);
        int awayPercent = Math.round((safeAway * 100f) / total);

        String homeText = showAsPercent ? safeHome + "%" : String.valueOf(safeHome);
        String awayText = showAsPercent ? safeAway + "%" : String.valueOf(safeAway);
        tv.setText(homeText + " - " + awayText);
        homeBar.setProgressCompat(homePercent, true);
        awayBar.setProgressCompat(awayPercent, true);
    }

    private int toInt(Object value) {
        try {
            if (value == null) return 0;
            String s = String.valueOf(value).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0;
            s = s.replace("%", "").trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

}