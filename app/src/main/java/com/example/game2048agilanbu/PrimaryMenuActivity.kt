package com.example.game2048agilanbu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu


class PrimaryMenuActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {
    private val BACKGROUND_COLOR_KEY = "BackgroundColor"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        mIsMainMenu = true

//        Typeface ClearSans_Bold = Typeface.createFromAsset(getResources().getAssets(), "ClearSans-Bold.ttf");
        val bt4x4 = findViewById<Button>(R.id.btn_start_4x4)
        val bt5x5 = findViewById<Button>(R.id.btn_start_5x5)
        val bt6x6 = findViewById<Button>(R.id.btn_start_6x6)

        /*bt4x4.setTypeface(ClearSans_Bold);
        bt5x5.setTypeface(ClearSans_Bold);
        bt6x6.setTypeface(ClearSans_Bold);*/
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_color_picker -> {
                rows = 4 // because of its GameView!
                startActivity(Intent(this@PrimaryMenuActivity, ColorPlatActivity::class.java))
            }
            R.id.settings_sign_out -> {
            }
        }
        return false
    }

    // Buttons:
    fun onButtonsClick(view: View) {
        when (view.id) {
            R.id.btn_start_4x4 -> StartGame(4)
            R.id.btn_start_5x5 -> StartGame(5)
            R.id.btn_start_6x6 -> StartGame(6)
            R.id.btn_show_achievements -> {
            }
            R.id.btn_show_leaderboards -> {
            }
            R.id.btn_share -> {
            }
            R.id.btn_more_games -> {
            }
            R.id.btn_rate -> {
            }
            R.id.btn_social_instagram -> {
            }
            R.id.btn_settings -> {
                val popup = PopupMenu(this, view)
                popup.setOnMenuItemClickListener(this) // to implement on click event on items of menu
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menus, popup.menu)
                popup.show()
            }
            R.id.btn_send_email -> {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mIsMainMenu = true
        SaveColors()
        LoadColors()
    }

    private fun SaveColors() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        if (mBackgroundColor < 0) editor.putInt(BACKGROUND_COLOR_KEY, mBackgroundColor)
        editor.apply()
    }

    private fun LoadColors() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        if (settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor) < 0) mBackgroundColor =
            settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor) else mBackgroundColor =
            resources.getColor(R.color.colorBackground)
    }

    private fun StartGame(rows: Int) {
        Companion.rows = rows
        mIsMainMenu = false
        startActivity(Intent(this@PrimaryMenuActivity, PrimaryActivity::class.java))
    }

    companion object {
        @JvmField
        var mIsMainMenu = true
        var rows = 4
            private set
        @JvmField
        var mBackgroundColor = 0

        // request codes we use when invoking an external activity
        const val RC_UNUSED = 5001
        const val RC_SIGN_IN = 9001
    }
}