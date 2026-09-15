package com.yad.musicstudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.section("MAIN ACTIVITY ONCREATE")

        try {
            setContentView(R.layout.activity_main)
            Logger.i("MainActivity", "Layout loaded")

            requestPermissions()
            Logger.i("MainActivity", "Permissions requested")

            AudioEngine.init(this)
            Logger.i("MainActivity", "AudioEngine initialized")

            SampleManager.init(this)
            Logger.i("MainActivity", "SampleManager initialized")

            viewPager = findViewById(R.id.viewPager)
            tabLayout = findViewById(R.id.tabLayout)
            Logger.i("MainActivity", "Views found")

            val adapter = StudioPagerAdapter(this)
            viewPager.adapter = adapter
            Logger.i("MainActivity", "Pager adapter set")

            TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
                tab.text = when (pos) {
                    0 -> "🥁 Seq"
                    1 -> "🎹 Piano"
                    2 -> "🎛️ Mixer"
                    3 -> "🎵 Samples"
                    4 -> "🎼 Playlist"
                    else -> "Tab"
                }
            }.attach()
            Logger.i("MainActivity", "Tab mediator attached")

        } catch (e: Exception) {
            Logger.e("MainActivity", "onCreate FAILED", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        Logger.i("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Logger.i("MainActivity", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("MainActivity", "onDestroy")
        AudioEngine.stop()
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        if (perms.isNotEmpty()) {
            Logger.i("MainActivity", "Requesting perms: $perms")
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        }
    }

    /**
     * Buka log viewer — dipanggil dari tombol.
     */
    fun openLogViewer() {
        try {
            startActivity(Intent(this, LogViewerActivity::class.java))
        } catch (e: Exception) {
            Logger.e("MainActivity", "Failed to open LogViewer", e)
            Toast.makeText(this, "Log viewer error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

class StudioPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 5
    override fun createFragment(position: Int): Fragment = try {
        Logger.i("Pager", "createFragment: $position")
        when (position) {
            0 -> SequencerFragment()
            1 -> PianoRollFragment()
            2 -> MixerFragment()
            3 -> SampleBrowserFragment()
            4 -> PlaylistFragment()
            else -> SequencerFragment()
        }
    } catch (e: Exception) {
        Logger.e("Pager", "createFragment FAILED: $position", e)
        SequencerFragment()
    }
}
