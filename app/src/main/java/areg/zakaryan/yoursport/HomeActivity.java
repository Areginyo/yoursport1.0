package areg.zakaryan.yoursport;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

import areg.zakaryan.yoursport.model.SearchItem;

public class HomeActivity extends AppCompatActivity {

    private ArrayList<SearchItem> selectedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        selectedItems = getIntent().getParcelableArrayListExtra("selected_items");
        if (selectedItems == null) selectedItems = new ArrayList<>();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_news) {
                fragment = NewsFragment.newInstance(selectedItems);
            } else if (id == R.id.nav_matches) {
                fragment = MatchesFragment.newInstance(selectedItems);
            } else if (id == R.id.nav_tv) {
                fragment = TvFragment.newInstance(selectedItems);
            } else if (id == R.id.nav_settings) {
                fragment = SettingsFragment.newInstance(selectedItems);
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
}