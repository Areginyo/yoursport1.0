package areg.zakaryan.yoursport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import areg.zakaryan.yoursport.api.ApiClient;
import areg.zakaryan.yoursport.api.ApiClientSports;
import areg.zakaryan.yoursport.model.SearchItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {
    private static final List<String> SUPPORTED_SPORTS =
            Arrays.asList("Football");

    private EditText edtSearch;
    private RecyclerView recycler;
    private Button btnContinue;
    private ProgressBar progressBar;

    private SearchAdapter adapter;

    private final List<SearchItem> allLeagues = new ArrayList<>();

    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    private ArrayList<String> selectedSports = new ArrayList<>();
    private final ArrayList<SearchItem> selectedItems = new ArrayList<>();
    private final int currentSeason = Calendar.getInstance().get(Calendar.YEAR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtSearch = findViewById(R.id.edtSearch);
        recycler = findViewById(R.id.recyclerSearch);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);

        selectedSports = getIntent().getStringArrayListExtra("selected_sports");
        if (selectedSports == null || selectedSports.isEmpty()) {
            selectedSports = new ArrayList<>();
            selectedSports.add("Football");
        }
        selectedSports.removeIf(sport -> !SUPPORTED_SPORTS.contains(sport));

        ArrayList<SearchItem> preselectedItems = getIntent().getParcelableArrayListExtra("selected_items");
        if (preselectedItems != null && !preselectedItems.isEmpty()) {
            selectedItems.addAll(preselectedItems);
        }

        adapter = new SearchAdapter(selectedItems, new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(SearchItem item) {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.add(item);
                }
                btnContinue.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTeamClicked(SearchItem teamItem) {
                
                Intent intent = new Intent(SearchActivity.this, TeamDetailActivity.class);
                intent.putExtra("team_item", teamItem);
                startActivity(intent);
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnContinue.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);

        loadInitialData();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 3) {
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, 500);
                } else {
                    showInitialList();
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Select at least one item", Toast.LENGTH_SHORT).show();
                return;
            }

SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("onboarding_completed", true)
                    .apply();

FavoritesManager.saveSelectedItems(selectedItems);

            Intent intent = new Intent(SearchActivity.this, HomeActivity.class);
            intent.putParcelableArrayListExtra("selected_items", selectedItems);

intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);

        if (selectedSports.contains("Football")) {
            loadFootballLeagues();
        }
    }

    private void loadFootballLeagues() {
        ApiClient.getApiService().getCompetitions().enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("Search", "Failed to load leagues");
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }

                try {
                    Map<String, Object> body = (Map<String, Object>) response.body();
                    List<Map<String, Object>> competitions = (List<Map<String, Object>>) body.get("response");

                    if (competitions != null) {
                        for (Map<String, Object> comp : competitions) {
                            Map<String, Object> league = (Map<String, Object>) comp.get("league");
                            Map<String, Object> countryMap = (Map<String, Object>) comp.get("country");
                            if (league == null) continue;

                            Number idNum = (Number) league.get("id");
                            int id = idNum != null ? idNum.intValue() : 0;
                            String name = (String) league.get("name");
                            String emblem = (String) league.get("logo");
                            String country = countryMap != null ? (String) countryMap.get("name") : "";
                            if (id <= 0 || name == null || name.trim().isEmpty()) continue;

                            allLeagues.add(new SearchItem(
                                    SearchItem.TYPE_ITEM,
                                    name,
                                    country,
                                    emblem,
                                    id,
                                    "league"
                            ));
                        }
                    }

runOnUiThread(() -> {
                        showInitialList();
                        progressBar.setVisibility(View.GONE);
                    });

                } catch (Exception e) {
                    Log.e("Search", "Leagues parsing error", e);
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Search", "Leagues network error", t);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);

        String lowerQuery = query.toLowerCase();
        List<SearchItem> results = new ArrayList<>();

List<SearchItem> filteredLeagues = new ArrayList<>();
        for (SearchItem item : allLeagues) {
            if (item.title != null && item.title.toLowerCase().contains(lowerQuery)) {
                filteredLeagues.add(item);
            }
        }
        if (!filteredLeagues.isEmpty()) {
            results.add(new SearchItem("LEAGUES"));
            results.addAll(filteredLeagues);
        }

ApiClient.getApiService().searchTeamsByName(query).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                List<SearchItem> teams = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> teamList = (List<Map<String, Object>>) body.get("response");
                        if (teamList != null) {
                            for (Map<String, Object> teamWrapper : teamList) {
                                Map<String, Object> team = (Map<String, Object>) teamWrapper.get("team");
                                Map<String, Object> venue = (Map<String, Object>) teamWrapper.get("venue");
                                if (team == null) continue;

                                Number idNum = (Number) team.get("id");
                                int id = idNum != null ? idNum.intValue() : 0;
                                String name = (String) team.get("name");
                                String logo = (String) team.get("logo");
                                String country = venue != null ? (String) venue.get("country") : "";

                                if (id > 0 && name != null) {
                                    teams.add(new SearchItem(
                                            SearchItem.TYPE_ITEM,
                                            name, country, logo, id, "team"
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Search", "Teams search parsing error", e);
                    }
                }

                if (!teams.isEmpty()) {
                    results.add(new SearchItem("TEAMS"));
                    results.addAll(teams);
                }

searchPlayers(query, results);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Search", "Team search failed", t);
                
                searchPlayers(query, results);
            }
        });
    }

    private void searchPlayers(String query, List<SearchItem> results) {
        ApiClientSports.getApiService().searchPlayers(query).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                List<SearchItem> players = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> body = (Map<String, Object>) response.body();
                        List<Map<String, Object>> playerList = (List<Map<String, Object>>) body.get("player");

                        if (playerList != null) {
                            for (Map<String, Object> player : playerList) {
                                String sport = (String) player.get("strSport");
                                String appSport = getAppSportName(sport);

                                if (selectedSports.contains(appSport)) {
                                    String idStr = (String) player.get("idPlayer");
                                    int id = idStr != null ? Integer.parseInt(idStr) : 0;
                                    if (id == 0) continue;

                                    String name = (String) player.get("strPlayer");
                                    String photo = (String) player.get("strThumb");
                                    String nationality = (String) player.get("strNationality");

                                    players.add(new SearchItem(
                                            SearchItem.TYPE_ITEM,
                                            name,
                                            nationality,
                                            photo,
                                            id,
                                            "player"
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Search", "Players parsing error", e);
                    }
                }

                if (!players.isEmpty()) {
                    results.add(new SearchItem("PLAYERS"));
                    results.addAll(players);
                }

                runOnUiThread(() -> {
                    adapter.setItems(results);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                runOnUiThread(() -> {
                    adapter.setItems(results);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showInitialList() {
        List<SearchItem> list = new ArrayList<>();

        if (!allLeagues.isEmpty()) {
            list.add(new SearchItem("LEAGUES"));
            list.addAll(allLeagues.subList(0, Math.min(10, allLeagues.size())));
        }

        adapter.setItems(list);
        progressBar.setVisibility(View.GONE);
    }

    private String getAppSportName(String sportsDbSport) {
        if (sportsDbSport == null) return "";
        switch (sportsDbSport) {
            case "Soccer": return "Football";
            default:       return "";
        }
    }
}