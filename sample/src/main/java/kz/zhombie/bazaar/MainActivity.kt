package kz.zhombie.bazaar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private var button: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)

        button?.setOnClickListener {
            Bazaar.Builder(supportFragmentManager)
                .setTag(Bazaar.TAG)
                .show()
        }
    }

}