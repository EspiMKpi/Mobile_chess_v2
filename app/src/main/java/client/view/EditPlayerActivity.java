package client.view;

import android.os.Bundle;
import android.util.Patterns;
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
            txtEmail.setText(p.getEmail() != null ? p.getEmail() : "");
            txtTel.setText(p.getTel() != null ? p.getTel() : "");
        }
    }

    private void update() {
        String name = txtName.getText().toString().trim();
        String email = txtEmail.getText().toString().trim();
        String tel = txtTel.getText().toString().trim();

        if (name.isEmpty()) {
            txtName.setError("Vui lòng nhập họ và tên");
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.setError("Email không đúng định dạng");
            return;
        }
        if (!tel.isEmpty() && tel.length() < 8) {
            txtTel.setError("Số điện thoại không hợp lệ");
            return;
        }

        Player updatedPlayer = new Player();
        updatedPlayer.setName(name);
        updatedPlayer.setEmail(email.isEmpty() ? null : email);
        updatedPlayer.setTel(tel.isEmpty() ? null : tel);

        myControl.sendData(new ObjectWrapper(ObjectWrapper.EDIT_PLAYER, updatedPlayer));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_EDIT_PLAYER) {
            if ("ok".equals(data.getData())) {
                Player p = myControl.getMyPlayer();
                if (p != null) {
                    p.setName(txtName.getText().toString().trim());
                    String email = txtEmail.getText().toString().trim();
                    p.setEmail(email.isEmpty() ? null : email);
                    String tel = txtTel.getText().toString().trim();
                    p.setTel(tel.isEmpty() ? null : tel);
                }
                toast("Cập nhật thông tin thành công!");
                finish();
            } else {
                toast("Cập nhật thông tin thất bại");
            }
        }
    }
}
