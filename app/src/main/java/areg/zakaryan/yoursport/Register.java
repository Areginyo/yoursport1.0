package areg.zakaryan.yoursport;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Register extends AppCompatActivity {

    private EditText edtFullName, edtEmail, edtPassword, edtRepeatPassword;
    private Button btnRegister, btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        edtFullName       = findViewById(R.id.edtFullNameReg);
        edtEmail          = findViewById(R.id.edtEmailAdressReg);
        edtPassword       = findViewById(R.id.edtPasswordReg);
        edtRepeatPassword = findViewById(R.id.edtRepeatPasswordReg);
        btnRegister       = findViewById(R.id.btnRegisterReg);
        btnLogin          = findViewById(R.id.btnLoginReg);

        btnRegister.setOnClickListener(v -> registerUser());

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(Register.this, MainActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String fullName  = edtFullName.getText().toString().trim();
        String email     = edtEmail.getText().toString().trim();
        String password  = edtPassword.getText().toString().trim();
        String repeatPwd = edtRepeatPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            edtFullName.setError("Enter your name");
            edtFullName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            edtEmail.setError("Enter your email");
            edtEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Enter your password");
            edtPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(repeatPwd)) {
            edtRepeatPassword.setError("Passwords do not match");
            edtRepeatPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdate =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(fullName)
                                            .build();

                            user.updateProfile(profileUpdate)
                                    .addOnCompleteListener(profileTask -> {
                                        Toast.makeText(this,
                                                "Account created!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Register.this, SportChoice.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}