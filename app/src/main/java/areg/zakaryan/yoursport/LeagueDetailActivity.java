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

public class LeagueDetailActivity extends AppCompatActivity {

    private ImageView imgLeagueLogo;
    private TextView txtLeagueName;
    private ImageButton btnBack;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private int leagueId;
    private String leagueName;
    private String leagueLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_league_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null) {
            leagueId   = intent.getIntExtra("league_id", 0);
            leagueName = intent.getStringExtra("league_name");
            leagueLogo = intent.getStringExtra("league_logo");
        }

        if (leagueId <= 0) {
            finish();
            return;
        }

        initViews();
        setupLeagueInfo();
        setupViewPager();
    }

    private void initViews() {
        imgLeagueLogo = findViewById(R.id.imgLeagueLogo);
        txtLeagueName = findViewById(R.id.txtLeagueName);
        btnBack       = findViewById(R.id.btnBack);
        tabLayout     = findViewById(R.id.tabLayout);
        viewPager     = findViewById(R.id.viewPager);
    }

    private void setupLeagueInfo() {
        txtLeagueName.setText(leagueName != null ? leagueName : "");

        if (leagueLogo != null && !leagueLogo.isEmpty()) {
            Glide.with(this)
                    .load(leagueLogo)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(imgLeagueLogo);
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        LeagueDetailPagerAdapter adapter = new LeagueDetailPagerAdapter(this, leagueId, leagueName, leagueLogo);
        viewPager.setAdapter(adapter);

        String[] tabTitles = { "Calendar", "Table", "Statistics" };

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();
    }
}
