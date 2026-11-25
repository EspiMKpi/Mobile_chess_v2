package ptit.e23cnpm01.nguyentiendung.covua

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import client.view.LocalChessGame

class MainActivity : AppCompatActivity() {

    private lateinit var game: LocalChessGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        game = LocalChessGame(
            this,
            findViewById(R.id.n11_boardCanvas),
            findViewById(R.id.n11_txtClockWhite),
            findViewById(R.id.n11_txtClockBlack),
            findViewById(R.id.n11_moveLog)
        )
        findViewById<Button>(R.id.n11_btnResign).setOnClickListener { game.confirmResign() }
        findViewById<Button>(R.id.n11_btnExit).setOnClickListener { game.confirmExit { finish() } }
        game.start()
    }

    override fun onDestroy() {
        game.stop()
        super.onDestroy()
    }
}
