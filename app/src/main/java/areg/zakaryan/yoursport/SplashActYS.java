package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActYS extends AppCompatActivity {

    // Длительность показа splash — 2.5 секунды
    private static final int SPLASH_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_act_ys);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActYS.this, MainActivity.class);
                startActivity(i);
                finish(); // закрыть splash чтобы нельзя было вернуться назад
            }
        }, SPLASH_DURATION);
    }
}