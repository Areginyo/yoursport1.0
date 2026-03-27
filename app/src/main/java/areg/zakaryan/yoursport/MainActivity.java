package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        edtEmail    = findViewById(R.id.edtEmailAddresLog);
        edtPassword = findViewById(R.id.edtPasswordLog);
        btnLogin    = findViewById(R.id.btnLoginLog);
        btnRegister = findViewById(R.id.btnRegisterLog);

        btnLogin.setOnClickListener(v -> loginUser());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Register.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Если пользователь уже вошёл — сразу на главный экран
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToHome();
        }
    }

    private void loginUser() {
        String email    = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty()) {
            edtEmail.setError("Введите email");
            edtEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Введите пароль");
            edtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Log in error";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToHome() {
        Intent intent = new Intent(MainActivity.this, SportChoice.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}