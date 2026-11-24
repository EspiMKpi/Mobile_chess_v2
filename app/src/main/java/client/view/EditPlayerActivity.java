package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Xem và sửa thông tin cá nhân (Hình 4.1, 4.2a, 4.5).
 */
public class EditPlayerActivity extends BaseActivity implements View.OnClickListener {

    private TextView lblUsername;
    private EditText txtName;
    private EditText txtEmail;
    private EditText txtTel;
    private Button btnUpdate;
    private Button btnCancel;

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

        // [HƯỚNG DẪN - Hình 4.5, bước 3-6]
        // 1. myControl.register(ObjectWrapper.REPLY_EDIT_PLAYER, this);
        // 2. Lấy Player hiện tại từ myControl.getMyPlayer() và đổ username (cố định), name, email, tel lên form.
    }

    /** TODO(N04 - Hình 4.5, bước 8): kiểm tra họ tên không trống, email hợp lệ nếu có nhập. */
    public boolean validateInput() {
        return true;
    }

    @Override
    public void onClick(View v) {
        // [HƯỚNG DẪN - Hình 4.5, bước 7-11]
        // - btnUpdate: gọi validateInput(), đóng gói Player mới (name, email, tel),
        //   gửi myControl.sendData(new ObjectWrapper(ObjectWrapper.EDIT_PLAYER, updatedPlayer)).
        // - btnCancel: finish().
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.5, bước 18-20]
        // Nếu REPLY_EDIT_PLAYER:
        // - "ok": cập nhật name, email, tel vào myControl.getMyPlayer(), Toast "Cập nhật thành công", finish().
        // - Ngược lại: Toast "Cập nhật thất bại".
    }
}
