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
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PlayerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_ID   = "player_id";
    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_PLAYER_PHOTO = "player_photo";
    public static final String EXTRA_PLAYER_POSITION = "player_position";
    public static final String EXTRA_PLAYER_NUMBER = "player_number";

    private int playerId;
    private String playerName;
    private String playerPhoto;
    private String playerPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        playerId       = intent.getIntExtra(EXTRA_PLAYER_ID, 0);
        playerName     = intent.getStringExtra(EXTRA_PLAYER_NAME);
        playerPhoto    = intent.getStringExtra(EXTRA_PLAYER_PHOTO);
        playerPosition = intent.getStringExtra(EXTRA_PLAYER_POSITION);

        if (playerId <= 0) { finish(); return; }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView txtTitle = findViewById(R.id.txtPlayerTitle);
        txtTitle.setText(playerName != null ? playerName : "Player");

        TextView txtName = findViewById(R.id.txtPlayerName);
        txtName.setText(playerName != null ? playerName : "—");

        TextView txtPos = findViewById(R.id.txtPlayerPosition);
        txtPos.setText(playerPosition != null ? playerPosition : "");

if (playerPhoto == null || playerPhoto.isEmpty()) {
            playerPhoto = "https://media.api-sports.io/football/players/" + playerId + ".png";
        }
        ImageView imgPhoto = findViewById(R.id.imgPlayerPhoto);
        Glide.with(this)
                .load(playerPhoto)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(imgPhoto);

findViewById(R.id.imgClubLogo).setVisibility(android.view.View.GONE);
        findViewById(R.id.txtClubName).setVisibility(android.view.View.GONE);

TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        PlayerDetailPagerAdapter adapter = new PlayerDetailPagerAdapter(this, playerId);
        viewPager.setAdapter(adapter);

        String[] tabs = {"Overview", "Statistics", "Career"};
        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> tab.setText(tabs[pos])).attach();
    }

public void updateClubInfo(String clubName, String clubLogo) {
        ImageView imgClub  = findViewById(R.id.imgClubLogo);
        TextView  txtClub  = findViewById(R.id.txtClubName);

        if (clubName != null && !clubName.isEmpty()) {
            txtClub.setText(clubName);
            txtClub.setVisibility(android.view.View.VISIBLE);
            imgClub.setVisibility(android.view.View.VISIBLE);
            if (clubLogo != null && !clubLogo.isEmpty()) {
                Glide.with(this).load(clubLogo)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(imgClub);
            }
        }
    }
}
