package client.view.ai;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N11. Ván cờ với máy, chạy hoàn toàn trên điện thoại (Giao diện 3.2 báo cáo N11).
 * Đã khai báo sẵn trong AndroidManifest (của N04). Extras gợi ý: whiteHuman, blackHuman, depth.
 */
public class AIMatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.n11_title_ai_match);
        // TODO(N11): setContentView(R.layout.n11_activity_ai_match); BoardCanvasView, AIThinkTank (ExecutorService + Handler)
        TextView placeholder = new TextView(this);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setText(R.string.n11_ai_todo);
        setContentView(placeholder);
    }
}
