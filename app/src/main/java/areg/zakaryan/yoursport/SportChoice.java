package areg.zakaryan.yoursport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashSet;

import areg.zakaryan.yoursport.model.SearchItem;

public class SportChoice extends AppCompatActivity {

    private CheckBox cbFootball;
    private Button btnChoose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sport_choice);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isOnboardingDone = prefs.getBoolean("onboarding_completed", false);
        boolean fromSettings = getIntent().getBooleanExtra("from_settings", false);

        if (isOnboardingDone && !fromSettings) {
            
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        cbFootball = findViewById(R.id.checkboxFootballSch);
        btnChoose = findViewById(R.id.btnChoose);

java.util.Set<String> savedSports = prefs.getStringSet("selected_sports", new HashSet<>());
        if (savedSports != null) {
            cbFootball.setChecked(savedSports.contains("Football"));
        }

        btnChoose.setOnClickListener(v -> {
            if (!cbFootball.isChecked()) {
                Toast.makeText(this, "Select at least one sport", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> selectedSports = new ArrayList<>();
            if (cbFootball.isChecked()) selectedSports.add("Football");

prefs.edit()
                    .putStringSet("selected_sports", new HashSet<>(selectedSports))
                    .putBoolean("onboarding_completed", true)
                    .apply();

ArrayList<SearchItem> intentItems = getIntent().getParcelableArrayListExtra("selected_items");

            if (intentItems != null && !intentItems.isEmpty()) {
                openSearchActivity(selectedSports, intentItems, fromSettings);
            } else {
                
                FavoritesManager.loadSelectedItems(items -> {
                    if (!isFinishing()) {
                        openSearchActivity(selectedSports, items, fromSettings);
                    }
                });
            }
        });
    }

    private void openSearchActivity(ArrayList<String> selectedSports,
                                     ArrayList<SearchItem> preselectedItems,
                                     boolean fromSettings) {
        Intent intent = new Intent(SportChoice.this, SearchActivity.class);
        intent.putStringArrayListExtra("selected_sports", selectedSports);
        intent.putExtra("from_settings", fromSettings);
        intent.putParcelableArrayListExtra("selected_items", preselectedItems);
        startActivity(intent);
    }
}