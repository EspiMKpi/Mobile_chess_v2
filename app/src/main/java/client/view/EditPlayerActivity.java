package client.view;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Xem và sửa thông tin cá nhân (Hình 4.1, 4.5). */
public class EditPlayerActivity extends BaseActivity implements View.OnClickListener {

    private TextView lblUsername;
    private EditText txtName;
    private EditText txtEmail;
    private EditText txtTel;
    private Button btnUpdate;
    private Button btnCancel;
    private Player pendingUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_player);
        setTitle(R.string.title_edit_player);

        lblUsername = findViewById(R.id.txtUsername);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtTel = findViewById(R.id.txtTel);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);

        btnUpdate.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        listen(ObjectWrapper.REPLY_EDIT_PLAYER);

        Player me = myControl.getMyPlayer();
        if (me != null) {
            lblUsername.setText(me.getUsername() + " (cố định)");
            txtName.setText(me.getName() != null ? me.getName() : "");
            txtEmail.setText(me.getEmail() != null ? me.getEmail() : "");
            txtTel.setText(me.getTel() != null ? me.getTel() : "");
        }
    }

    public boolean validateInput() {
        String name = txtName.getText().toString().trim();
        if (name.isEmpty()) {
            txtName.setError("Nhập họ tên");
            return false;
        }
        String email = txtEmail.getText().toString().trim();
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.setError("Email không hợp lệ");
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnUpdate) {
            if (!validateInput()) {
                return;
            }
            Player p = new Player();
            p.setName(txtName.getText().toString().trim());
            p.setEmail(txtEmail.getText().toString().trim());
            p.setTel(txtTel.getText().toString().trim());
            pendingUpdate = p;
            btnUpdate.setEnabled(false);
            if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.EDIT_PLAYER, p))) {
                btnUpdate.setEnabled(true);
                toast("Chưa kết nối server");
            }
        } else if (v.getId() == R.id.btnCancel) {
            finish();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (d.getPerformative() != ObjectWrapper.REPLY_EDIT_PLAYER) {
            return;
        }
        btnUpdate.setEnabled(true);
        if ("ok".equals(d.getData())) {
            Player me = myControl.getMyPlayer();
            if (me != null && pendingUpdate != null) {
                me.setName(pendingUpdate.getName());
                me.setEmail(pendingUpdate.getEmail());
                me.setTel(pendingUpdate.getTel());
            }
            toast("Cập nhật thành công");
            finish();
        } else {
            toast("Cập nhật thất bại");
        }
    }
}
