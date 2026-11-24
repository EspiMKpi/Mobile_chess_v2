package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import client.control.ClientCtr;

/** Chủ: N04. Lớp cha của mọi Activity trong hệ thống (Hình 4.2a, 4.10a). */
public abstract class BaseActivity extends AppCompatActivity implements DataReceiver {

    protected ClientCtr myControl = ClientCtr.getInstance();

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
