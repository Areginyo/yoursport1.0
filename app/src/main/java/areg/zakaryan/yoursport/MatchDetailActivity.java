package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.viewpager2.widget.ViewPager2;

import areg.zakaryan.yoursport.model.MatchItem;
import areg.zakaryan.yoursport.model.SearchItem;

public class MatchDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH = "extra_match";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_detail);

        MatchItem match = getIntent().getParcelableExtra(EXTRA_MATCH);

        if (match == null) {
            Log.e("MatchDetail", "match is null!");
            Toast.makeText(this, "Match not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("MatchDetail", "Opened: " + match.homeTeam + " vs " + match.awayTeam);

TextView tvHome   = findViewById(R.id.tv_home_team);
        TextView tvAway   = findViewById(R.id.tv_away_team);
        TextView tvScore  = findViewById(R.id.tv_score_big);
        TextView tvStatus = findViewById(R.id.tv_match_status);
        ImageView ivHome  = findViewById(R.id.iv_home_logo_big);
        ImageView ivAway  = findViewById(R.id.iv_away_logo_big);

        if (tvHome   != null) tvHome.setText(match.homeTeam != null ? match.homeTeam : "—");
        if (tvAway   != null) tvAway.setText(match.awayTeam != null ? match.awayTeam : "—");
        if (tvScore  != null) tvScore.setText(match.score   != null ? match.score    : "vs");
        if (tvStatus != null) tvStatus.setText(match.status != null ? match.status   : "Upcoming");

        if (ivHome != null && match.homeLogo != null && !match.homeLogo.isEmpty()) {
            Glide.with(this).load(match.homeLogo)
                    .placeholder(R.drawable.ic_placeholder).into(ivHome);

ivHome.setOnClickListener(v -> {
                if (match.homeTeamId > 0) {
                    SearchItem homeTeamItem = new SearchItem(
                            SearchItem.TYPE_ITEM,
                            match.homeTeam,
                            "",
                            match.homeLogo,
                            match.homeTeamId,
                            "team"
                    );
                    openTeamDetails(homeTeamItem);
                }
            });
        }
        if (ivAway != null && match.awayLogo != null && !match.awayLogo.isEmpty()) {
            Glide.with(this).load(match.awayLogo)
                    .placeholder(R.drawable.ic_placeholder).into(ivAway);

ivAway.setOnClickListener(v -> {
                if (match.awayTeamId > 0) {
                    SearchItem awayTeamItem = new SearchItem(
                            SearchItem.TYPE_ITEM,
                            match.awayTeam,
                            "",
                            match.awayLogo,
                            match.awayTeamId,
                            "team"
                    );
                    openTeamDetails(awayTeamItem);
                }
            });
        }

ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout  = findViewById(R.id.tab_layout);

        if (viewPager != null && tabLayout != null) {
            MatchDetailPagerAdapter pagerAdapter =
                    new MatchDetailPagerAdapter(this, match);
            viewPager.setAdapter(pagerAdapter);

            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("Overview"); break;
                    case 1: tab.setText("Lineups");  break;
                    case 2: tab.setText("Stats");    break;
                    case 3: tab.setText("H2H");      break;
                }
            }).attach();
        }
    }

    private void openTeamDetails(SearchItem teamItem) {
        Intent intent = new Intent(this, TeamDetailActivity.class);
        intent.putExtra("team_item", teamItem);
        startActivity(intent);
    }
}