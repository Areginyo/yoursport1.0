package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import areg.zakaryan.yoursport.model.SearchItem;

public class TeamDetailActivity extends AppCompatActivity {

    private ImageView imgTeamLogo;
    private TextView txtTeamName;
    private TextView txtTeamCountry;
    private ImageButton btnBack;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TeamDetailPagerAdapter adapter;

    private SearchItem teamItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

Intent intent = getIntent();
        if (intent != null && intent.hasExtra("team_item")) {
            teamItem = intent.getParcelableExtra("team_item");
        }

        if (teamItem == null) {
            finish();
            return;
        }

        initViews();
        setupTeamInfo();
        setupViewPager();
        setupClickListeners();
    }

    private void initViews() {
        imgTeamLogo = findViewById(R.id.imgTeamLogo);
        txtTeamName = findViewById(R.id.txtTeamName);
        txtTeamCountry = findViewById(R.id.txtTeamCountry);
        btnBack = findViewById(R.id.btnBack);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupTeamInfo() {
        txtTeamName.setText(teamItem.title);
        txtTeamCountry.setText(teamItem.subtitle != null ? teamItem.subtitle : "");

        if (teamItem.logoUrl != null && !teamItem.logoUrl.isEmpty()) {
            Glide.with(this)
                    .load(teamItem.logoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(imgTeamLogo);
        }
    }

    private void setupViewPager() {
        adapter = new TeamDetailPagerAdapter(this, teamItem);
        viewPager.setAdapter(adapter);

        String[] tabTitles = {
                "News",
                "Matches",
                "Season",
                "Squad",
                "Transfers"
        };

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}
