package com.chathuninimesha.coreflow.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.databinding.ActivityMainBinding
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.ui.habits.HabitsFragment
import com.chathuninimesha.coreflow.ui.home.HomeFragment
import com.chathuninimesha.coreflow.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthModule.init(this)
        if (!AuthModule.repository().hasActiveSession()) {
            AuthNavigator.openSignIn(this)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        Repositories.init(applicationContext)

        if (savedInstanceState == null) {
            openTab(HomeFragment(), getString(R.string.home))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> openTab(HomeFragment(), getString(R.string.home))
                R.id.nav_habits -> openTab(HabitsFragment(), getString(R.string.habits))
                R.id.nav_settings -> openTab(SettingsFragment(), getString(R.string.settings))
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val depth = supportFragmentManager.backStackEntryCount
            binding.bottomNav.visibility = if (depth == 0) View.VISIBLE else View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(depth > 0)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return super.onSupportNavigateUp()
    }

    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun openTab(fragment: Fragment, title: String) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
        binding.toolbar.title = title
        binding.bottomNav.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    fun openDetail(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .addToBackStack(null)
            .commit()
        binding.toolbar.title = title
        binding.bottomNav.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
