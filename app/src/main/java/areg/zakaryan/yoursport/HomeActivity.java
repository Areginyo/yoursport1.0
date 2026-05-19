package areg.zakaryan.yoursport;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import areg.zakaryan.yoursport.model.SearchItem;

public class HomeActivity extends AppCompatActivity {
    private static final Set<String> SUPPORTED_SPORTS = new HashSet<>(
            Arrays.asList("Football")
    );

    private ArrayList<SearchItem> selectedItems;
    private List<String> selectedSports = new ArrayList<>();

    private RecyclerView rvSportTabs;
    private SportTabAdapter sportTabAdapter;

    private String currentSport = "Football";

    private BottomNavigationView bottomNav;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

selectedItems = getIntent().getParcelableArrayListExtra("selected_items");

        if (selectedItems != null && !selectedItems.isEmpty()) {
            
            initializeUI(savedInstanceState);
        } else {
            
            selectedItems = new ArrayList<>();
            FavoritesManager.loadSelectedItems(items -> {
                selectedItems = items;
                if (!isFinishing()) {
                    initializeUI(savedInstanceState);
                }
            });
        }
    }

    private void initializeUI(Bundle savedInstanceState) {
        if (isInitialized) return;
        isInitialized = true;

SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> savedSports = prefs.getStringSet("selected_sports", new HashSet<>());

        selectedSports.clear();
        selectedSports.addAll(savedSports);
        selectedSports.removeIf(sport -> !SUPPORTED_SPORTS.contains(sport));
        if (!savedSports.equals(new HashSet<>(selectedSports))) {
            Set<String> updated = new HashSet<>(selectedSports);
            prefs.edit().putStringSet("selected_sports", updated).apply();
        }

        if (selectedSports.contains("Football")) {
            selectedSports.remove("Football");
            selectedSports.add(0, "Football");
            currentSport = "Football";
        }

if (selectedSports.isEmpty()) {
            selectedSports.add("Football");
            currentSport = "Football";
        } else if (!selectedSports.contains(currentSport)) {
            currentSport = selectedSports.get(0);
        }

rvSportTabs = findViewById(R.id.rv_sport_tabs);
        rvSportTabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        sportTabAdapter = new SportTabAdapter(selectedSports, sport -> {
            currentSport = sport;
            refreshCurrentFragment();
        });
        rvSportTabs.setAdapter(sportTabAdapter);

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            boolean showTabs = (itemId == R.id.nav_news ||
                    itemId == R.id.nav_matches);

            rvSportTabs.setVisibility(showTabs ? View.VISIBLE : View.GONE);
            findViewById(R.id.divider_sport_tabs).setVisibility(showTabs ? View.VISIBLE : View.GONE);

            if (itemId == R.id.nav_news) {
                fragment = NewsFragment.newInstance(selectedItems, currentSport);
            } else if (itemId == R.id.nav_matches) {
                fragment = MatchesFragment.newInstance(selectedItems, currentSport);
            } else if (itemId == R.id.nav_settings) {
                fragment = SettingsFragment.newInstance(selectedItems, currentSport);
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_news);
        }
    }

    private void refreshCurrentFragment() {
        int currentId = bottomNav.getSelectedItemId();
        bottomNav.setSelectedItemId(currentId);
    }
}