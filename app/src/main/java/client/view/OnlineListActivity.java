package client.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.OnlinePlayerAdapter;
import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Danh sách online (hình 4.11). Nhận REPLY_ONLINE_LIST và SERVER_INFORM_ONLINE_LIST.
 * Từ đây mở các màn hình cá nhân (sửa thông tin, đổi mật khẩu, lịch sử, bảng xếp hạng), chơi với máy (N11),
 * thách đấu và đăng xuất. Lời mời đến do ClientCtr.showInviteDialog() hiển thị.
 */
public class OnlineListActivity extends BaseActivity {

    private TextView lblUserInfo;
    private RecyclerView tblOnlineUsers;
    private OnlinePlayerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_list);
        setTitle(R.string.title_online);
        // TODO: bước 1 - listen(REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST)
        // TODO: findViewById, adapter = new OnlinePlayerAdapter(...)
        // TODO: nút btnEditProfile, btnChangePass, btnHistory, btnRanking -> startActivity(...)
        //       btnPlayAI -> AISetupDialog.open(this) (N11); btnLogout -> confirm(...) rồi logout();
        //       btnChallenge -> challenge()
        // TODO: bước 2 - sendData(GET_ONLINE_LIST, null)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: hiện lời chào với tên myPlayer lên lblUserInfo
    }

    /** Bước 12: bỏ chính mình khỏi danh sách, giữ dòng đang chọn theo id sau khi làm mới. */
    public void updateOnlineTable(ArrayList<PlayerStat> list) {
        // TODO
    }

    /**
     * Bước 13-14: chưa chọn ai -> nhắc chọn; người đó BUSY -> báo bận;
     * ngược lại ChallengeDialog.newInstance(selected).show(...).
     */
    private void challenge() {
        // TODO
    }

    /** Đăng xuất: myControl.closeConnection(), về ClientMainActivity (NEW_TASK | CLEAR_TASK), finish(). */
    private void logout() {
        // TODO
    }

    /** Bước 11: REPLY_ONLINE_LIST / SERVER_INFORM_ONLINE_LIST -> updateOnlineTable(list). */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
