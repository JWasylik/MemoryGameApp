package pl.wsei.pam.lab03

import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.wsei.pam.lab01.R
import android.widget.ImageButton
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import java.util.Timer
import kotlin.concurrent.schedule

class Lab03Activity : AppCompatActivity() {
    private lateinit var mBoard: androidx.gridlayout.widget.GridLayout
    private lateinit var mBoardModel: MemoryBoardView
    lateinit var completionPlayer: MediaPlayer
    lateinit var negativePlayer: MediaPlayer
    private var isSound: Boolean = true

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("game_state", mBoardModel.getState())
    }
    override protected fun onResume() {
        super.onResume()
        completionPlayer = MediaPlayer.create(applicationContext, R.raw.completion)
        negativePlayer = MediaPlayer.create(applicationContext, R.raw.negative_guitar)
    }

    override protected fun onPause() {
        super.onPause();
        completionPlayer.release()
        negativePlayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //return super.onCreateOptionsMenu(menu)
        val inflater: MenuInflater  = getMenuInflater()
        inflater.inflate(R.menu.board_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.board_activity_sound -> {
                isSound = !isSound

                if (isSound) {
                    item.setIcon(R.drawable.volume_up)
                    Toast.makeText(this, "Sound turned on", Toast.LENGTH_SHORT).show()
                } else {
                    item.setIcon(R.drawable.volume_off)
                    Toast.makeText(this, "Sound turned off", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lab03)
        mBoard = findViewById(R.id.memoryGameLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mBoard) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val size: IntArray? = intent.getIntArrayExtra("size")
        val (rows, columns) = if (size != null && size.size == 2) {
            size[0] to size[1]
        } else {
            Toast.makeText(this, "Error, set default: 3x3", Toast.LENGTH_SHORT).show()
            3 to 3
        }

        mBoardModel = MemoryBoardView(mBoard, columns, rows)
        val savedState = savedInstanceState?.getIntArray("game_state")
        mBoardModel = MemoryBoardView(mBoard, columns, rows)
        if (savedState != null) {
            mBoardModel.setState(savedState)
        }

            mBoardModel.setOnGameChangeListener { e ->
                run {
                    when (e.state) {
                        GameStates.Matching -> {
                            e.tiles.forEach{it.revealed = true}
                        }
                        GameStates.Match -> {
                            e.tiles.forEach{it.revealed = true}
                            if(isSound){
                                if(completionPlayer.isPlaying){
                                    completionPlayer.pause()
                                    completionPlayer.seekTo(0)
                                }
                                completionPlayer.start()
                            }
                            e.tiles.forEach{ tile ->
                                mBoardModel.animatePairedButton(tile.button) {
                                    tile.button.isEnabled = false
                                }
                            }
                        }
                        GameStates.NoMatch -> {
                            e.tiles.forEach{it.revealed = true}
                            if(isSound){
                                negativePlayer.start()
                            }
                            e.tiles.forEach{ tile ->
                                mBoardModel.animateWrongPairedButton(tile.button) {
                                    tile.button.isEnabled = true
                                }
                            }
                            Timer().schedule(800){
                                runOnUiThread() {
                                    e.tiles.forEach{it.revealed = false}
                                }
                            }
                        }
                        GameStates.Finished -> {
                            e.tiles.forEach{it.revealed = true}
                            if(isSound){
                                if(completionPlayer.isPlaying){
                                    completionPlayer.pause()
                                    completionPlayer.seekTo(0)
                                }
                                completionPlayer.start()
                            }
                            e.tiles.forEach{ tile ->
                                mBoardModel.animatePairedButton(tile.button) {
                                    tile.button.isEnabled = false
                                }
                            }
                            Toast.makeText(this, "Game finished", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}