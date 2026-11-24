package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đổi mật khẩu (Hình 4.1, 4.6). */
public class ChangePasswordActivity extends BaseActivity implements View.OnClickListener {

    private EditText txtOldPassword;
    private EditText txtNewPassword;
    private EditText txtConfirmPassword;
    private Button btnSave;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setTitle(R.string.title_change_password);

        txtOldPassword = findViewById(R.id.txtOldPassword);
        txtNewPassword = findViewById(R.id.txtNewPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        listen(ObjectWrapper.REPLY_CHANGE_PASSWORD);
    }

    public boolean validateInput() {
        String oldPass = txtOldPassword.getText().toString();
        String newPass = txtNewPassword.getText().toString();
        String confirm = txtConfirmPassword.getText().toString();
        if (oldPass.isEmpty()) {
            txtOldPassword.setError("Nhập mật khẩu hiện tại");
            return false;
        }
        if (newPass.isEmpty()) {
            txtNewPassword.setError("Nhập mật khẩu mới");
            return false;
        }
        if (!newPass.equals(confirm)) {
            txtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        if (newPass.equals(oldPass)) {
            txtNewPassword.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSave) {
            if (!validateInput()) {
                return;
            }
            String[] payload = {
                    txtOldPassword.getText().toString(),
                    txtNewPassword.getText().toString()
            };
            btnSave.setEnabled(false);
            if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.CHANGE_PASSWORD, payload))) {
                btnSave.setEnabled(true);
                toast("Chưa kết nối server");
            }
        } else if (v.getId() == R.id.btnCancel) {
            finish();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (d.getPerformative() != ObjectWrapper.REPLY_CHANGE_PASSWORD) {
            return;
        }
        btnSave.setEnabled(true);
        if ("ok".equals(d.getData())) {
            toast("Đổi mật khẩu thành công");
            finish();
        } else {
            toast("Mật khẩu hiện tại không đúng");
        }
    }
}
