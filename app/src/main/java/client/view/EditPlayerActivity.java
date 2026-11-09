package client.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Xem và sửa thông tin (hình 4.5). Nhận REPLY_EDIT_PLAYER. */
public class EditPlayerActivity extends BaseActivity {

    private TextView txtUsername;          // chỉ hiển thị, không sửa được
    private EditText txtName, txtEmail, txtTel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_player);
        setTitle(R.string.title_edit_player);
        // TODO: bước 3 - listen(REPLY_EDIT_PLAYER)
        // TODO: findViewById; btnUpdate -> update(); btnCancel -> finish()
        // TODO: showPlayer()
    }

    /** Bước 4-6: myControl.getMyPlayer() -> điền username, name, email, tel lên form. */
    private void showPlayer() {
        // TODO
    }

    /** Bước 8: họ tên không trống, email / số điện thoại hợp lệ (nếu nhập). */
    private boolean validateInput() {
        // TODO
        return false;
    }

    /** Bước 7-12: validateInput(), setter của Player (name, email, tel), sendData(EDIT_PLAYER). */
    private void update() {
        // TODO
    }

    /** Bước 18-20: "ok" -> cập nhật myPlayer trong ClientCtr, báo cập nhật thành công; "false" -> báo thất bại. */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
