package areg.zakaryan.yoursport;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchLineupsFragment extends Fragment {

    private static final String ARG_MATCH = "match";

    private MatchItem mMatch;

    public static MatchLineupsFragment newInstance(MatchItem match) {
        MatchLineupsFragment fragment = new MatchLineupsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MATCH, match);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match_lineups, container, false);
        mMatch = getArguments() != null ? getArguments().getParcelable(ARG_MATCH) : null;

        TextView tvHomeFormation = view.findViewById(R.id.tv_home_formation);
        TextView tvAwayFormation = view.findViewById(R.id.tv_away_formation);
        TextView tvHomeBench = view.findViewById(R.id.tv_home_bench);
        TextView tvAwayBench = view.findViewById(R.id.tv_away_bench);
        LinearLayout homePitchRows = view.findViewById(R.id.home_pitch_rows);
        LinearLayout awayPitchRows = view.findViewById(R.id.away_pitch_rows);

        tvHomeFormation.setText("Home formation: —");
        tvAwayFormation.setText("Away formation: —");
        tvHomeBench.setText("no data");
        tvAwayBench.setText("no data");
        renderMessageRow(homePitchRows, "Lineups are loading...");
        renderMessageRow(awayPitchRows, "Lineups are loading...");

        if (mMatch == null || mMatch.matchId == null || mMatch.matchId.trim().isEmpty()) {
            renderMessageRow(homePitchRows, "No lineup data.");
            renderMessageRow(awayPitchRows, "No lineup data.");
            return view;
        }

        ApiClient.getApiService().getLineups(mMatch.matchId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                ApiClient.getApiService().getFixtureEvents(mMatch.matchId).enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> c2, Response<Object> r2) {
                        Object eventsBody = r2.isSuccessful() ? r2.body() : null;
                        bindLineups(
                                response.body(),
                                eventsBody,
                                tvHomeFormation,
                                tvAwayFormation,
                                tvHomeBench,
                                tvAwayBench,
                                homePitchRows,
                                awayPitchRows
                        );
                    }

                    @Override
                    public void onFailure(Call<Object> c2, Throwable t) {
                        bindLineups(
                                response.body(),
                                null,
                                tvHomeFormation,
                                tvAwayFormation,
                                tvHomeBench,
                                tvAwayBench,
                                homePitchRows,
                                awayPitchRows
                        );
                    }
                });
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                
            }
        });

        return view;
    }

    @SuppressWarnings("unchecked")
    private void bindLineups(
            Object raw,
            Object rawEvents,
            TextView tvHomeFormation,
            TextView tvAwayFormation,
            TextView tvHomeBench,
            TextView tvAwayBench,
            LinearLayout homePitchRows,
            LinearLayout awayPitchRows
    ) {
        if (!(raw instanceof Map)) return;
        Map<String, Object> root = (Map<String, Object>) raw;
        Object responseObj = root.get("response");
        if (!(responseObj instanceof List)) return;

        List<?> lineups = (List<?>) responseObj;
        if (lineups.isEmpty()) return;

        Map<String, Object> homeLineupMap = resolveLineupForSide(lineups, true);
        Map<String, Object> awayLineupMap = resolveLineupForSide(lineups, false);
        List<Map<String, Object>> substitutionEvents = parseSubstitutionEvents(rawEvents);

        bindTeamInternal(homeLineupMap, tvHomeFormation, tvHomeBench, homePitchRows, false, substitutionEvents);
        bindTeamInternal(awayLineupMap, tvAwayFormation, tvAwayBench, awayPitchRows, true, substitutionEvents);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveLineupForSide(List<?> lineups, boolean homeSide) {
        int wantId = homeSide ? (mMatch != null ? mMatch.homeTeamId : 0)
                : (mMatch != null ? mMatch.awayTeamId : 0);

        Map<String, Object> byId = null;
        Map<String, Object> other = null;
        for (Object o : lineups) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> lm = (Map<String, Object>) o;
            Map<String, Object> team = castMap(lm.get("team"));
            if (team != null && wantId > 0 && asInt(team.get("id")) == wantId) {
                byId = lm;
                break;
            }
        }

        Object firstEl = lineups.get(0);
        Object secondEl = lineups.size() > 1 ? lineups.get(1) : null;
        Map<String, Object> lineup0 = firstEl instanceof Map ? (Map<String, Object>) firstEl : null;
        Map<String, Object> lineup1 = secondEl instanceof Map ? (Map<String, Object>) secondEl : null;

        if (byId != null) return byId;
        
        return homeSide ? lineup0 : (lineup1 != null ? lineup1 : lineup0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSubstitutionEvents(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(raw instanceof Map)) return out;
        Object resp = ((Map<String, Object>) raw).get("response");
        if (!(resp instanceof List)) return out;
        for (Object o : (List<?>) resp) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> ev = (Map<String, Object>) o;
            String type = safe(ev.get("type")).toLowerCase(Locale.ROOT);
            if (type.contains("subst")) out.add(ev);
        }
        Collections.sort(out, Comparator.comparingInt(this::eventMinute));
        return out;
    }

    private int eventMinute(Map<String, Object> ev) {
        Map<String, Object> time = castMap(ev.get("time"));
        if (time != null) {
            return asInt(time.get("elapsed")) * 100 + asInt(time.get("extra"));
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private void bindTeamInternal(
            Map<String, Object> lineupMap,
            TextView tvFormation,
            TextView tvBench,
            LinearLayout pitchRows,
            boolean isAwayTeamPitch,
            List<Map<String, Object>> substitutionEvents
    ) {
        if (lineupMap == null) {
            tvFormation.setText((isAwayTeamPitch ? "Away" : "Home") + " formation: —");
            tvBench.setText("no data");
            renderMessageRow(pitchRows, "No lineup available.");
            return;
        }

        Map<String, Object> lineup = lineupMap;

        String formation = safe(lineup.get("formation"));
        tvFormation.setText((isAwayTeamPitch ? "Away" : "Home") + " formation: "
                + (formation.isEmpty() ? "—" : formation));

        List<PlayerViewData> starting = parsePlayers(lineup.get("startXI"));
        int tid = lineupTeamId(lineup);

        applySubstitutions(starting, tid, substitutionEvents);

        sortRowsByGrid(starting);
        renderFormationPitch(pitchRows, starting, isAwayTeamPitch);

        List<PlayerViewData> bench = parsePlayers(lineup.get("substitutes"));
        if (bench.isEmpty()) {
            tvBench.setText("no data");
        } else {
            List<String> benchLines = new ArrayList<>();
            for (PlayerViewData p : bench) {
                benchLines.add(formatBenchLine(p));
            }
            tvBench.setText(joinLines(benchLines));
        }
    }

    private String joinLines(List<String> benchLines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < benchLines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(benchLines.get(i));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private int lineupTeamId(Map<String, Object> lineup) {
        Map<String, Object> t = castMap(lineup.get("team"));
        return t != null ? asInt(t.get("id")) : 0;
    }

    private void applySubstitutions(List<PlayerViewData> starters, int teamId,
                                    List<Map<String, Object>> substitutionEvents) {
        if (teamId <= 0 || substitutionEvents.isEmpty()) return;

        for (Map<String, Object> ev : substitutionEvents) {
            Map<String, Object> tm = castMap(ev.get("team"));
            if (tm == null || asInt(tm.get("id")) != teamId) continue;

            Map<String, Object> assist = castMap(ev.get("assist"));
            Map<String, Object> playerIn = castMap(ev.get("player"));
            if (assist == null || playerIn == null) continue;

            int outId = asInt(assist.get("id"));
            for (PlayerViewData p : starters) {
                if (p.playerId == outId) {
                    p.playerId = asInt(playerIn.get("id"));
                    p.name = safe(playerIn.get("name"));
                    if (p.name.isEmpty()) p.name = safe(assist.get("name"));
                    p.number = normalizeNumber(playerIn.get("number"));
                    p.cameViaSubstitution = true;
                    break;
                }
            }
        }
    }

    private void sortRowsByGrid(List<PlayerViewData> list) {
        list.sort((a, b) -> {
            String ga = normalizeGrid(a.grid);
            String gb = normalizeGrid(b.grid);
            return ga.compareTo(gb);
        });
    }

    private String normalizeGrid(String grid) {
        return grid != null ? grid : "";
    }

    @SuppressWarnings("unchecked")
    private List<PlayerViewData> parsePlayers(Object playersObj) {
        List<PlayerViewData> out = new ArrayList<>();
        if (!(playersObj instanceof List)) return out;
        for (Object playerObj : (List<?>) playersObj) {
            if (!(playerObj instanceof Map)) continue;
            Map<String, Object> playerWrap = (Map<String, Object>) playerObj;
            Map<String, Object> playerData = castMap(playerWrap.get("player"));
            if (playerData == null) continue;

            String name = safe(playerData.get("name"));
            if (name.isEmpty()) continue;
            PlayerViewData player = new PlayerViewData();
            player.playerId = asInt(playerData.get("id"));
            player.name = name;
            player.number = normalizeNumber(playerData.get("number"));
            player.position = safe(playerData.get("pos"));
            player.grid = safe(playerData.get("grid"));
            out.add(player);
        }
        return out;
    }

    private void renderFormationPitch(LinearLayout container, List<PlayerViewData> starting, boolean reverseRows) {
        container.removeAllViews();
        if (starting.isEmpty()) {
            renderMessageRow(container, "Starting XI not available.");
            return;
        }

        LinkedHashMap<Integer, List<PlayerViewData>> rows = groupByGridRow(starting);
        List<Integer> keys = new ArrayList<>(rows.keySet());
        Collections.sort(keys);
        if (reverseRows) Collections.reverse(keys);

        for (Integer key : keys) {
            List<PlayerViewData> rowPlayers = rows.get(key);
            if (rowPlayers == null) continue;
            sortRowByGridColumn(rowPlayers);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            for (PlayerViewData player : rowPlayers) {
                row.addView(buildPlayerMarker(player));
            }
            container.addView(row);
        }
    }

    private void sortRowByGridColumn(List<PlayerViewData> rowPlayers) {
        rowPlayers.sort((a, b) -> Integer.compare(parseGridColumn(a.grid), parseGridColumn(b.grid)));
    }

    private int parseGridColumn(String grid) {
        if (grid != null && grid.contains(":")) {
            String[] parts = grid.split(":");
            if (parts.length >= 2) {
                try {
                    return Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) { }
            }
        }
        return 0;
    }

    private LinkedHashMap<Integer, List<PlayerViewData>> groupByGridRow(List<PlayerViewData> players) {
        LinkedHashMap<Integer, List<PlayerViewData>> rows = new LinkedHashMap<>();
        for (PlayerViewData p : players) {
            int rowIndex = parseGridRow(p.grid, p.position);
            if (!rows.containsKey(rowIndex)) {
                rows.put(rowIndex, new ArrayList<>());
            }
            rows.get(rowIndex).add(p);
        }
        return rows;
    }

    private int parseGridRow(String grid, String position) {
        if (grid != null && grid.contains(":")) {
            try {
                return Integer.parseInt(grid.split(":")[0]);
            } catch (Exception ignored) {
            }
        }
        String pos = position == null ? "" : position.toUpperCase(Locale.ROOT);
        if ("G".equals(pos)) return 1;
        if ("D".equals(pos)) return 2;
        if ("M".equals(pos)) return 3;
        if ("F".equals(pos)) return 4;
        return 5;
    }

    private View buildPlayerMarker(PlayerViewData player) {
        LinearLayout outer = new LinearLayout(requireContext());
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        int margin = dp(2);
        lp.setMargins(margin, dp(6), margin, dp(6));
        outer.setLayoutParams(lp);

        LinearLayout iconRow = new LinearLayout(requireContext());
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iconRow.setLayoutParams(iconRowLp);

        FrameLayout fl = new FrameLayout(requireContext());
        LinearLayout.LayoutParams flLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        fl.setLayoutParams(flLp);

        ImageView marker = new ImageView(requireContext());
        FrameLayout.LayoutParams markerLp = new FrameLayout.LayoutParams(dp(32), dp(32));
        markerLp.gravity = Gravity.CENTER;
        marker.setLayoutParams(markerLp);
        
        com.bumptech.glide.Glide.with(outer.getContext())
                .load("https://media.api-sports.io/football/players/" + player.playerId + ".png")
                .placeholder(R.drawable.ic_player_marker)
                .error(R.drawable.ic_player_marker)
                .circleCrop()
                .into(marker);
                
        marker.setContentDescription(player.name);
        fl.addView(marker);
        iconRow.addView(fl);

        if (player.cameViaSubstitution) {
            ImageView sub = new ImageView(requireContext());
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(dp(16), dp(16));
            subLp.leftMargin = dp(2);
            sub.setLayoutParams(subLp);
            sub.setImageResource(R.drawable.ic_substitution);
            sub.setContentDescription("Substitution");
            iconRow.addView(sub);
        }

        outer.addView(iconRow);

        String displayName = player.name.trim();
        if (!player.number.isEmpty()) {
            displayName = player.number + ". " + displayName;
        }

        TextView tvName = labeledText(displayName, 9, 0xFFFFFFFF);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvName.setLayoutParams(textLp);
        outer.addView(tvName);

if (player.playerId > 0) {
            final int pid = player.playerId;
            final String pname = player.name;
            final String ppos = player.position;
            outer.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(
                        requireContext(), PlayerDetailActivity.class);
                intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_ID, pid);
                intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_NAME, pname);
                intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_POSITION, ppos);
                startActivity(intent);
            });
        }

        return outer;
    }

    private TextView labeledText(String text, int sp, int color) {
        TextView tv = new TextView(requireContext());
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setPadding(dp(1), dp(1), dp(1), 0);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setText(text);
        return tv;
    }

    private String formatBenchLine(PlayerViewData p) {
        StringBuilder row = new StringBuilder();
        if (!p.number.isEmpty()) row.append(p.number).append(" ");
        row.append(p.name);
        if (!p.position.isEmpty()) row.append(" (").append(p.position).append(")");
        return row.toString();
    }

    private void renderMessageRow(LinearLayout container, String text) {
        container.removeAllViews();
        TextView msg = new TextView(requireContext());
        msg.setText(text);
        msg.setTextColor(0xFFEAEAEA);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(dp(8), dp(16), dp(8), dp(16));
        container.addView(msg);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private int asInt(Object val) {
        try {
            if (val == null) return 0;
            if (val instanceof Number) return ((Number) val).intValue();
            String s = String.valueOf(val).trim();
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safe(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }

    private String normalizeNumber(Object numberObj) {
        String raw = safe(numberObj).trim();
        if (raw.isEmpty()) return "";
        try {
            if (raw.contains(".")) return String.valueOf((int) Math.round(Double.parseDouble(raw)));
            return String.valueOf(Integer.parseInt(raw));
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static class PlayerViewData {
        int playerId;
        boolean cameViaSubstitution;
        String name = "";
        String number = "";
        String position = "";
        String grid = "";
    }

}
