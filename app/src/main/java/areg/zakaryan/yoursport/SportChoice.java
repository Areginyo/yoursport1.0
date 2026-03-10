package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SportChoice extends AppCompatActivity {

    private CheckBox cbFootball, cbUFC, cbBasketball, cbFormula1, cbTennis;
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

        cbFootball   = findViewById(R.id.checkboxFootballSch);
        cbUFC        = findViewById(R.id.checkboxUFCSch);
        cbBasketball = findViewById(R.id.checkboxBasketballSch);
        cbFormula1   = findViewById(R.id.checkboxFormula1Sch);
        cbTennis     = findViewById(R.id.checkboxTennisSch);
        btnChoose    = findViewById(R.id.btnChoose);

        btnChoose.setOnClickListener(v -> {
            // Проверяем что выбран хотя бы один спорт
            if (!cbFootball.isChecked() && !cbUFC.isChecked() &&
                    !cbBasketball.isChecked() && !cbFormula1.isChecked() &&
                    !cbTennis.isChecked()) {
                Toast.makeText(this, "Select at least one sport", Toast.LENGTH_SHORT).show();
                return;
            }

            // Если выбран Football — переходим на поиск
            if (cbFootball.isChecked()) {
                Intent intent = new Intent(SportChoice.this, SearchActivity.class);
                intent.putExtra("sport", "football");
                startActivity(intent);
            } else {
                // Для остальных спортов пока заглушка
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}