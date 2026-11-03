package client.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.3. Nhận REPLY_EDIT_PLAYER. */
public class EditPlayerActivity extends BaseActivity {

    private TextView txtUsername;
    private EditText txtName, txtEmail, txtTel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_player);
        setTitle(R.string.title_edit_player);
        listen(ObjectWrapper.REPLY_EDIT_PLAYER);

        txtUsername = findViewById(R.id.txtUsername);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtTel = findViewById(R.id.txtTel);
        findViewById(R.id.btnUpdate).setOnClickListener(v -> update());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        Player p = myControl.getMyPlayer();
        if (p != null) {
            txtUsername.setText(p.getUsername());
            txtName.setText(p.getName());
            txtEmail.setText(p.getEmail());
            txtTel.setText(p.getTel());
        }
    }

    private void update() {
        // TODO(N04, M2): validateInput(); sendData(EDIT_PLAYER, player với name/email/tel mới)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2): "ok" -> cập nhật getMyPlayer(), toast, finish()
    }
}
