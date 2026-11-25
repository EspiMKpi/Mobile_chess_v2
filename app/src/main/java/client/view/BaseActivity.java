package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import client.control.ClientCtr;

/** Chủ: N04. Lớp cha của mọi Activity trong hệ thống (Hình 4.2a, 4.10a). */
public abstract class BaseActivity extends AppCompatActivity implements DataReceiver {

    protected ClientCtr myControl = ClientCtr.getInstance();

    /**
     * Sửa lỗi trên Android mới (targetSdk 35+ bắt buộc hiển thị tràn viền): thanh tiêu đề (ActionBar)
     * của AppCompat 1.6.1 bị vẽ ĐÈ lên đầu nội dung nên ô nhập đầu tiên (ví dụ "Tên đăng nhập")
     * bị che mất. Hàm này đo phần bị đè và đẩy nội dung xuống đúng bằng phần đó.
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        avoidActionBarOverlap();
    }

    private void avoidActionBarOverlap() {
        final View content = findViewById(android.R.id.content);
        final int barId = getResources().getIdentifier("action_bar_container", "id", getPackageName());
        final View bar = barId != 0 ? findViewById(barId) : null;
        if (content == null || bar == null) {
            return;
        }
        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int overlap = bar.getVisibility() == View.VISIBLE ? Math.max(0, bar.getBottom() - content.getTop()) : 0;
            if (content.getPaddingTop() != overlap) {
                content.setPadding(content.getPaddingLeft(), overlap,
                        content.getPaddingRight(), content.getPaddingBottom());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        myControl.setCurrentActivity(this);
        if (!(this instanceof LoginActivity) && !(this instanceof RegisterActivity) && !myControl.isConnected()) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onPause() {
        if (myControl.getCurrentActivity() == this) {
            myControl.setCurrentActivity(null);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    protected void listen(int... codes) {
        for (int code : codes) {
            myControl.register(code, this);
        }
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
