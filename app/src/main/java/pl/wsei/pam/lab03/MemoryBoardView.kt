package pl.wsei.pam.lab03

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import pl.wsei.pam.lab01.R
import java.util.Random
import java.util.Stack

class MemoryBoardView(
    private val gridLayout: androidx.gridlayout.widget.GridLayout,
    private val cols: Int,
    private val rows: Int
) {
    private val tiles: MutableMap<String, Tile> = mutableMapOf()
    private val icons: List<Int> = listOf(
        R.drawable.baseline_bolt_24,
        R.drawable.baseline_blind_24,
        R.drawable.baseline_anchor_24,
        R.drawable.baseline_add_a_photo_24,
        R.drawable.baseline_add_call_24,
        R.drawable.baseline_agriculture_24,
        R.drawable.baseline_airport_shuttle_24,
        R.drawable.baseline_architecture_24,
        R.drawable.baseline_attach_money_24,
        R.drawable.baseline_auto_fix_high_24,
        R.drawable.baseline_bakery_dining_24,
        R.drawable.baseline_brightness_3_24,
        R.drawable.baseline_bug_report_24,
        R.drawable.baseline_business_24,
        R.drawable.baseline_cached_24,
        R.drawable.round_airplanemode_active_24,
        R.drawable.sharp_auto_awesome_24,
        R.drawable.baseline_celebration_24,
    )
    private val deckResource: Int = R.drawable.baseline_blur_on_24
    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = { (e) -> }
    private val matchedPair: Stack<Tile> = Stack()
    private val logic: MemoryGameLogic = MemoryGameLogic(cols * rows / 2)

    init {
        val shuffledIcons: MutableList<Int> = mutableListOf<Int>().also {
            it.addAll(icons.subList(0, cols * rows / 2))
            it.addAll(icons.subList(0, cols * rows / 2))
            it.shuffle()
        }

        var index = 0
        for(row in 0 until rows) {
            for(col in 0 until cols){

                val btn = ImageButton(gridLayout.context).also {
                    it.tag = "$row $col"
                    val layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams()
                    it.setImageResource(R.drawable.baseline_blur_on_24)
                    it.scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams.width = 0
                    layoutParams.height = 0
                    layoutParams.setMargins(5,5,5,5)
                    layoutParams.setGravity(Gravity.CENTER)
                    layoutParams.columnSpec = androidx.gridlayout.widget.GridLayout.spec(col, 1, 1f)
                    layoutParams.rowSpec = androidx.gridlayout.widget.GridLayout.spec(row, 1, 1f)
                    it.layoutParams = layoutParams
                    gridLayout.addView(it)
                }
                val tileResource = shuffledIcons.removeAt(0)
                val tile = Tile(btn, tileResource, deckResource)
                tiles[btn.tag.toString()] = tile
                btn.setOnClickListener(::onClickTile)
            }
        }
    }

    fun getState(): IntArray {
        return tiles.values.flatMap { tile ->
            listOf(if (tile.revealed) 1 else 0, tile.tileResource)
        }.toIntArray()
    }

    fun setState(state: IntArray) {
        var index = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = tiles["$row $col"]
                val isRevealed = state[index] == 1
                val resource = state[index + 1]
                if (tile != null) {
                    tile.tileResource = resource
                    tile.revealed = isRevealed
                    tile.button.setImageResource(if (isRevealed) resource else deckResource)
                }
                index += 2
            }
        }
    }

    private fun onClickTile(v: View) {
        val tile = tiles[v.tag]
        if(tile == null || tile.revealed) return
        matchedPair.push(tile)
        val matchResult = logic.process {
            tile?.tileResource?:-1
        }
        onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), matchResult))
        if (matchResult != GameStates.Matching) {
            matchedPair.clear()
        }
    }

    fun setOnGameChangeListener(listener: (event: MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    private fun addTile(button: ImageButton, resourceImage: Int) {
        button.setOnClickListener(::onClickTile)
        val tile = Tile(button, resourceImage, deckResource)
        tiles[button.tag.toString()] = tile
    }

    fun animatePairedButton(button: ImageButton, action: Runnable) {
        val flipOut = ObjectAnimator.ofFloat(button, "rotationY", 0f, 90f)
        val flipIn = ObjectAnimator.ofFloat(button, "rotationY", 90f, 0f)

        flipOut.duration = 150
        flipIn.duration = 150

        val scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f)
        val scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.2f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.2f, 1f)

        scaleUpX.duration = 100
        scaleUpY.duration = 100
        scaleDownX.duration = 100
        scaleDownY.duration = 100


        val flash = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.5f, 1f)
        flash.duration = 300

        val flipSet = AnimatorSet()
        flipSet.playSequentially(flipOut, flipIn)

        val scaleSet = AnimatorSet()
        scaleSet.playSequentially(scaleUpX, scaleUpY, scaleDownX, scaleDownY)

        val fullSet = AnimatorSet()
        fullSet.playTogether(flipSet, scaleSet, flash)
        fullSet.interpolator = DecelerateInterpolator()
        fullSet.startDelay = 100

        fullSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}

            override fun onAnimationEnd(animator: Animator) {
                button.rotationY = 0f
                button.scaleX = 1f
                button.scaleY = 1f
                button.alpha = 1f
                action.run()
            }

            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })

        fullSet.start()
    }

    fun animateWrongPairedButton(button: ImageButton, action: Runnable ) {
        val set = AnimatorSet()
        val random = Random()
        button.pivotX = random.nextFloat() * 200f

        val rotationToRight = ObjectAnimator.ofFloat(button, "rotation", 4f)
        val rotationToLeft = ObjectAnimator.ofFloat(button, "rotation", -4f)
        val rotationToZero = ObjectAnimator.ofFloat(button, "rotation", 4f)
        set.startDelay = 200
        set.duration = 40
        set.interpolator = DecelerateInterpolator()
        set.playSequentially(rotationToRight,rotationToLeft, rotationToZero)
        set.addListener(object: Animator.AnimatorListener {

            override fun onAnimationStart(animator: Animator) {
            }

            override fun onAnimationEnd(animator: Animator) {
                button.rotation = 0f
                action.run();
            }

            override fun onAnimationCancel(animator: Animator) {
            }

            override fun onAnimationRepeat(animator: Animator) {
            }
        })
        set.start()
    }

}