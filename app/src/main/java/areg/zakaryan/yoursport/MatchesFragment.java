package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.model.MatchItem;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchesFragment extends Fragment {

    private static final String ARG_SELECTED_ITEMS = "selected_items";
    private static final String ARG_CURRENT_SPORT  = "current_sport";

    private static final int[] TOP_LEAGUE_IDS = {
            
            39,   
            140,  
            135,  
            78,   
            61,   

88,   
            94,   
            307,  

2,    
            3,    
            848,  

1,    
            4,    
            9,    
            7,    

45,   
            48,   
            143,  
            144,  
            137,  
            138,  
            66,   
            67,   
            81,   
            82,   
            87,   
            564   
    };

    private final Set<Integer> selectedLeagueIds = new HashSet<>();
    private final Map<Integer, Integer> selectedLeagueOrder = new HashMap<>();

private final Set<Integer> favoriteTeamIds = new HashSet<>();
    private final Set<Integer> favoriteLeagueIds = new HashSet<>();

    private static final List<String> FEMALE_COMPETITION_KEYWORDS = Collections.unmodifiableList(java.util.Arrays.asList(
            "women",
            "woman",
            "ladies",
            "female",
            "feminine",
            "féminine",
            "feminina",
            "femenina",
            "femenino",
            "wsl",
            "womens",
            "girls"
    ));

    private static final List<String> YOUTH_COMPETITION_KEYWORDS = Collections.unmodifiableList(java.util.Arrays.asList(
            "u23", "u-23", "u 23",
            "u21", "u-21", "u 21",
            "u20", "u-20", "u 20",
            "u19", "u-19", "u 19",
            "u18", "u-18", "u 18",
            "u17", "u-17", "u 17",
            "u16", "u-16", "u 16",
            "u15", "u-15", "u 15",
            "u14", "u-14", "u 14",
            "youth",
            "junior",
            "academy",
            "reserve",
            "reserves",
            "b team",
            "ii",
            "2nd team"
    ));

private List<MatchItem> filterMatchesByLeague(List<MatchItem> matches, int leagueId) {
        List<MatchItem> filtered = new ArrayList<>();

        for (MatchItem match : matches) {
            if (match.leagueId == leagueId) {
                filtered.add(match);
            }
        }

        return filtered;
    }

    private static final Map<Integer, Integer> LEAGUE_ORDER = createLeagueOrder();

    private ArrayList<SearchItem> selectedItems;
    private String currentSport;

    private RecyclerView rvMatches;
    private TextView tvEmpty;
    private LinearLayout llDates;
    private HorizontalScrollView hsvDates;
    private LinearLayout llLeagueTabs;
    private HorizontalScrollView hsvLeagueTabs;
    private MatchesAdapter adapter;

    private final List<String> dateList = new ArrayList<>();
    private final List<View> dateViews = new ArrayList<>();
    private int selectedDateIndex = -1;
    private String selectedDate;

private List<MatchItem> allMatchesForDate = new ArrayList<>();

private static final int TAB_ALL = -1;
    private static final int TAB_FAVORITES = -2;
    private int selectedTabId = TAB_ALL;
    private final List<View> leagueTabViews = new ArrayList<>();

    public static MatchesFragment newInstance(ArrayList<SearchItem> selectedItems, String currentSport) {
        MatchesFragment fragment = new MatchesFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, selectedItems);
        args.putString(ARG_CURRENT_SPORT, currentSport);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedItems = getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS);
            currentSport  = getArguments().getString(ARG_CURRENT_SPORT);
        }
        buildSelectedLeagueIds();
        buildFavoriteIds();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matches, container, false);

        rvMatches    = view.findViewById(R.id.rv_matches);
        tvEmpty      = view.findViewById(R.id.tv_empty);
        llDates      = view.findViewById(R.id.ll_dates);
        hsvDates     = view.findViewById(R.id.hsv_dates);
        llLeagueTabs = view.findViewById(R.id.ll_league_tabs);
        hsvLeagueTabs = view.findViewById(R.id.hsv_league_tabs);

        rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MatchesAdapter();

adapter.setOnMatchClickListener(match -> {
            Intent intent = new Intent(requireContext(), MatchDetailActivity.class);
            intent.putExtra(MatchDetailActivity.EXTRA_MATCH, match);
            startActivity(intent);
        });

adapter.setOnTeamClickListener((teamId, teamName, teamLogo) -> {
            SearchItem teamItem = new SearchItem(
                    SearchItem.TYPE_ITEM,
                    teamName,
                    "",
                    teamLogo,
                    teamId,
                    "team"
            );
            
            Intent intent = new Intent(requireContext(), TeamDetailActivity.class);
            intent.putExtra("team_item", teamItem);
            startActivity(intent);
        });

adapter.setOnLeagueClickListener(header -> {
            Intent intent = new Intent(requireContext(), LeagueDetailActivity.class);
            intent.putExtra("league_id", header.leagueId);
            intent.putExtra("league_name", header.leagueName);
            intent.putExtra("league_logo", header.leagueLogo);
            startActivity(intent);
        });

        rvMatches.setAdapter(adapter);

        if (currentSport == null) currentSport = "Football";

        switch (currentSport) {
            case "Football":
                buildDayCalendar();
                break;
        }

        return view;
    }

    private void buildDayCalendar() {
        dateList.clear();
        llDates.removeAllViews();
        dateViews.clear();

        SimpleDateFormat sdfKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("EEE", Locale.ENGLISH);
        SimpleDateFormat sdfNum = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat sdfMon = new SimpleDateFormat("MMM", Locale.ENGLISH);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        final int todayIndex = 7;

        for (int i = 0; i < 15; i++) {
            String dateKey = sdfKey.format(cal.getTime());
            dateList.add(dateKey);

            View dateView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_date_tab, llDates, false);

            ((TextView) dateView.findViewById(R.id.tv_day_name))
                    .setText(sdfDay.format(cal.getTime()).toUpperCase());
            ((TextView) dateView.findViewById(R.id.tv_day_number))
                    .setText(sdfNum.format(cal.getTime()));
            ((TextView) dateView.findViewById(R.id.tv_month))
                    .setText(sdfMon.format(cal.getTime()).toUpperCase());

            final int idx = i;
            final String d = dateKey;
            dateView.setOnClickListener(v -> selectDay(idx, d));

            llDates.addView(dateView);
            dateViews.add(dateView);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        selectDay(todayIndex, dateList.get(todayIndex));

        hsvDates.post(() -> {
            if (!isAdded()) return;
            View today = dateViews.get(todayIndex);
            int scrollX = today.getLeft() - hsvDates.getWidth() / 2 + today.getWidth() / 2;
            hsvDates.smoothScrollTo(Math.max(0, scrollX), 0);
        });
    }

    private void selectDay(int index, String date) {
        if (selectedDateIndex >= 0 && selectedDateIndex < dateViews.size()) {
            dateViews.get(selectedDateIndex).setBackgroundResource(R.drawable.bg_date_tab_normal);
        }
        selectedDateIndex = index;
        selectedDate = date;
        dateViews.get(index).setBackgroundResource(R.drawable.bg_date_tab_selected);

selectedTabId = TAB_ALL;

        loadFootballMatches(date);
    }

    private void loadFootballMatches(String date) {
        tvEmpty.setText("Loading matches...");
        tvEmpty.setVisibility(View.VISIBLE);

        Call<Object> call = ApiClient.getApiService().getAllMatchesByDate(date);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    allMatchesForDate = parseAllMatches(response.body());
                    buildLeagueTabs(allMatchesForDate);
                    applyCurrentFilter();
                } else {
                    tvEmpty.setText("No matches on this date");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (!isAdded()) return;
                tvEmpty.setText("Failed to load matches");
            }
        });
    }

private void buildLeagueTabs(List<MatchItem> matches) {
        llLeagueTabs.removeAllViews();
        leagueTabViews.clear();

View allTab = createLeagueTab("All", null, TAB_ALL);
        llLeagueTabs.addView(allTab);
        leagueTabViews.add(allTab);

if (!favoriteTeamIds.isEmpty() || !favoriteLeagueIds.isEmpty()) {
            View favTab = createFavoritesTab();
            llLeagueTabs.addView(favTab);
            leagueTabViews.add(favTab);
        }

LinkedHashMap<Integer, MatchItem> leagueMap = new LinkedHashMap<>();
        
        List<MatchItem> sorted = new ArrayList<>(matches);
        sorted.sort((a, b) -> Integer.compare(a.leagueSortOrder, b.leagueSortOrder));

        for (MatchItem m : sorted) {
            if (!leagueMap.containsKey(m.leagueId)) {
                leagueMap.put(m.leagueId, m);
            }
        }

        for (Map.Entry<Integer, MatchItem> entry : leagueMap.entrySet()) {
            MatchItem m = entry.getValue();
            View tab = createLeagueTab(m.leagueName, m.leagueLogo, m.leagueId);
            llLeagueTabs.addView(tab);
            leagueTabViews.add(tab);
        }

updateLeagueTabsUI();
    }

    private View createLeagueTab(String name, String logoUrl, int tabId) {
        View tab = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_league_tab, llLeagueTabs, false);

        TextView tvName = tab.findViewById(R.id.tv_tab_name);
        ImageView ivIcon = tab.findViewById(R.id.iv_tab_icon);

        tvName.setText(name);

        if (logoUrl != null && !logoUrl.isEmpty()) {
            ivIcon.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(logoUrl).into(ivIcon);
        } else {
            ivIcon.setVisibility(View.GONE);
        }

        tab.setTag(tabId);
        tab.setOnClickListener(v -> {
            selectedTabId = tabId;
            updateLeagueTabsUI();
            applyCurrentFilter();
        });

        return tab;
    }

    private View createFavoritesTab() {
        View tab = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_league_tab, llLeagueTabs, false);

        TextView tvName = tab.findViewById(R.id.tv_tab_name);
        ImageView ivIcon = tab.findViewById(R.id.iv_tab_icon);

        tvName.setText("Favorites");
        ivIcon.setVisibility(View.VISIBLE);
        ivIcon.setImageResource(R.drawable.ic_star_filled);

        tab.setTag(TAB_FAVORITES);
        tab.setOnClickListener(v -> {
            selectedTabId = TAB_FAVORITES;
            updateLeagueTabsUI();
            applyCurrentFilter();
        });

        return tab;
    }

    private void updateLeagueTabsUI() {
        for (View tab : leagueTabViews) {
            int tabId = (int) tab.getTag();
            boolean isSelected = tabId == selectedTabId;

            tab.setBackgroundResource(isSelected
                    ? R.drawable.bg_league_tab_selected
                    : R.drawable.bg_league_tab_normal);

            TextView tvName = tab.findViewById(R.id.tv_tab_name);
            if (tvName != null) {
                tvName.setTextColor(isSelected ? 0xFF181818 : 0xFFFFFFFF);
            }

            ImageView ivIcon = tab.findViewById(R.id.iv_tab_icon);
            if (ivIcon != null && ivIcon.getVisibility() == View.VISIBLE) {
                if (tabId == TAB_FAVORITES) {
                    ivIcon.setColorFilter(isSelected ? 0xFF181818 : 0xFFAAFF00);
                }
            }
        }
    }

    private void applyCurrentFilter() {
        List<MatchItem> filtered;

        if (selectedTabId == TAB_ALL) {
            filtered = allMatchesForDate;
        } else if (selectedTabId == TAB_FAVORITES) {
            filtered = filterMatchesByFavorites(allMatchesForDate);
        } else {
            filtered = filterMatchesByLeague(allMatchesForDate, selectedTabId);
        }

        rebuildList(filtered);
    }

    private List<MatchItem> filterMatchesByFavorites(List<MatchItem> matches) {
        List<MatchItem> filtered = new ArrayList<>();
        for (MatchItem match : matches) {
            if (favoriteTeamIds.contains(match.homeTeamId)
                    || favoriteTeamIds.contains(match.awayTeamId)
                    || favoriteLeagueIds.contains(match.leagueId)) {
                filtered.add(match);
            }
        }
        return filtered;
    }

@SuppressWarnings("unchecked")
    private List<MatchItem> parseAllMatches(Object raw) {
        List<MatchItem> list = new ArrayList<>();
        if (!(raw instanceof Map)) return list;

        Map<String, Object> root = (Map<String, Object>) raw;
        Object matchesObj = root.get("response");
        if (!(matchesObj instanceof List)) return list;

        for (Object obj : (List<?>) matchesObj) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) obj;

            Map<String, Object> league = castMap(m.get("league"));
            Map<String, Object> fixture = castMap(m.get("fixture"));
            Map<String, Object> teams = castMap(m.get("teams"));
            Map<String, Object> goals = castMap(m.get("goals"));
            if (league == null || fixture == null || teams == null) continue;

            int leagueId = asInt(league.get("id"));
            if (!isWhitelistedLeague(leagueId)) continue;
            String leagueName = safe(league.get("name"));
            String leagueType = safe(league.get("type"));
            int order = getLeagueOrder(leagueId, leagueName, leagueType);
            if (order < 0) continue;

            MatchItem item = new MatchItem();
            item.leagueId = leagueId;
            item.leagueID = leagueId;
            item.leagueName = leagueName;
            item.leagueLogo = safe(league.get("logo"));
            item.leagueSortOrder = order;

            Map<String, Object> homeObj = castMap(teams.get("home"));
            Map<String, Object> awayObj = castMap(teams.get("away"));
            if (homeObj != null) {
                item.homeTeam = safe(homeObj.get("name"));
                item.homeLogo = safe(homeObj.get("logo"));
                item.homeTeamId = asInt(homeObj.get("id"));
            }
            if (awayObj != null) {
                item.awayTeam = safe(awayObj.get("name"));
                item.awayLogo = safe(awayObj.get("logo"));
                item.awayTeamId = asInt(awayObj.get("id"));
            }

            item.time = safe(fixture.get("date"));
            item.date = selectedDate;
            item.matchId = String.valueOf(asInt(fixture.get("id")));
            String status = safe(getNestedMapValue(fixture, "status", "short"));
            item.status = status;
            item.isLive = "1H".equals(status) || "2H".equals(status) || "HT".equals(status) || "ET".equals(status) || "P".equals(status);

            if (goals != null) {
                String h = parseScoreValue(goals.get("home"));
                String a = parseScoreValue(goals.get("away"));
                if (!h.isEmpty() && !a.isEmpty()) {
                    item.score = h + " - " + a;
                }
            }

            if (item.homeTeam != null && !item.homeTeam.isEmpty()) {
                list.add(item);
            }
        }
        return list;
    }

    private void rebuildList(List<MatchItem> allMatches) {
        List<MatchItem> sorted = new ArrayList<>(allMatches);
        sorted.sort((a, b) -> {
            if (a.leagueSortOrder != b.leagueSortOrder)
                return Integer.compare(a.leagueSortOrder, b.leagueSortOrder);
            String ta = a.time != null ? a.time : "";
            String tb = b.time != null ? b.time : "";
            return ta.compareTo(tb);
        });

        List<MatchItem> grouped = new ArrayList<>();
        int currentOrder = -1;
        for (MatchItem m : sorted) {
            if (m.leagueSortOrder != currentOrder) {
                currentOrder = m.leagueSortOrder;
                MatchItem header = new MatchItem();
                header.isLeagueHeader = true;
                header.leagueName = m.leagueName;
                header.leagueLogo = m.leagueLogo;
                header.leagueId = m.leagueId;
                grouped.add(header);
            }
            grouped.add(m);
        }

        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            adapter.submitList(grouped);
            if (grouped.isEmpty()) {
                if (selectedTabId == TAB_FAVORITES) {
                    tvEmpty.setText("No favorite matches on this date");
                } else {
                    tvEmpty.setText("No matches on this date");
                }
                tvEmpty.setVisibility(View.VISIBLE);
                rvMatches.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvMatches.setVisibility(View.VISIBLE);
            }
        });
    }

    private static Map<Integer, Integer> createLeagueOrder() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < TOP_LEAGUE_IDS.length; i++) {
            map.put(TOP_LEAGUE_IDS[i], i);
        }
        return map;
    }

    private int getLeagueOrder(int leagueId, String leagueName, String leagueType) {
        if (!isAdultMaleCompetition(leagueName)) return -1;
        Integer order = LEAGUE_ORDER.get(leagueId);
        return order != null ? order : -1;
    }

    private void buildSelectedLeagueIds() {
        selectedLeagueIds.clear();
        selectedLeagueOrder.clear();
        if (selectedItems == null) return;
        int index = 0;
        for (SearchItem item : selectedItems) {
            if (item == null) continue;
            if ("league".equalsIgnoreCase(item.category)) {
                selectedLeagueIds.add(item.id);
                if (!selectedLeagueOrder.containsKey(item.id)) {
                    selectedLeagueOrder.put(item.id, index++);
                }
            }
        }
    }

    private void buildFavoriteIds() {
        favoriteTeamIds.clear();
        favoriteLeagueIds.clear();
        if (selectedItems == null) return;
        for (SearchItem item : selectedItems) {
            if (item == null) continue;
            if ("team".equalsIgnoreCase(item.category)) {
                favoriteTeamIds.add(item.id);
            } else if ("league".equalsIgnoreCase(item.category)) {
                favoriteLeagueIds.add(item.id);
            }
        }
    }

    private boolean isWhitelistedLeague(int leagueId) {
        return LEAGUE_ORDER.containsKey(leagueId);
    }

    private boolean isAdultMaleCompetition(String leagueName) {
        String n = leagueName == null ? "" : leagueName.toLowerCase(Locale.ROOT);
        for (String keyword : FEMALE_COMPETITION_KEYWORDS) {
            if (n.contains(keyword)) return false;
        }
        for (String keyword : YOUTH_COMPETITION_KEYWORDS) {
            if (n.contains(keyword)) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }

    private Object getNestedMapValue(Map<String, Object> root, String key1, String key2) {
        Map<String, Object> nested = castMap(root.get(key1));
        return nested == null ? null : nested.get(key2);
    }

    private int asInt(Object val) {
        try {
            if (val == null) return 0;
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            String raw = String.valueOf(val).trim();
            if (raw.isEmpty()) return 0;
            if (raw.contains(".")) {
                return (int) Double.parseDouble(raw);
            }
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String parseScoreValue(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        if (s.equals("null")) return "";
        try {
            return String.valueOf((int) Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private String safe(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return s.equals("null") ? "" : s;
    }
}