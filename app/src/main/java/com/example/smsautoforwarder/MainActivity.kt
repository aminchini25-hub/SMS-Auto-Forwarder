package com.example.smsautoforwarder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        setSupportActionBar(toolbar)

        bottomNavigation.setOnItemSelectedListener { item ->
            showScreen(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun navigateTo(itemId: Int) {
        if (bottomNavigation.selectedItemId == itemId) {
            showScreen(itemId)
        } else {
            bottomNavigation.selectedItemId = itemId
        }
    }

    private fun showScreen(itemId: Int) {
        val (fragment, titleRes) = when (itemId) {
            R.id.nav_rules -> RulesFragment() to R.string.nav_rules
            R.id.nav_channels -> ChannelsFragment() to R.string.nav_channels
            R.id.nav_settings -> SettingsFragment() to R.string.nav_settings
            else -> HomeFragment() to R.string.nav_home
        }

        toolbar.setTitle(titleRes)
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
        }
    }
}
