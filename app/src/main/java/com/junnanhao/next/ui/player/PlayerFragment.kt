package com.junnanhao.next.ui.player

import android.Manifest
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnTouch
import com.junnanhao.next.R
import com.junnanhao.next.data.model.Song
import android.support.v4.view.GestureDetectorCompat
import timber.log.Timber
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.view.*
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.frag_player.*
import java.io.File
import java.io.FileInputStream


/**
 * Created by Jonas on 2017/5/27.
 * player fragment
 */
class PlayerFragment : Fragment(), PlayerContract.View {

    @BindView(R.id.tv_song_title) lateinit var title: TextView
    @BindView(R.id.tv_song_artist) lateinit var artist: TextView
    @BindView(R.id.img_art) lateinit var art: ImageView
    @BindView(R.id.container) lateinit var background: ViewGroup
    lateinit var rect: Rect

    companion object {
        fun instance(): PlayerFragment {
            return PlayerFragment()
        }
        private val SWIPE_MIN_DISTANCE = 120
        private val SWIPE_MAX_OFF_PATH = 250
        private val SWIPE_THRESHOLD_VELOCITY = 200
    }

    lateinit var mPresenter: PlayerContract.Presenter
    override fun setPresenter(presenter: PlayerContract.Presenter) {
        mPresenter = presenter
    }

    lateinit var mDetector: GestureDetectorCompat

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.frag_player, container, false)
        ButterKnife.bind(this, view)
        checkPermission()
        measure()
        return view
    }

    fun measure() {
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size: Point = Point()
        wm.defaultDisplay.getSize(size)
        rect = Rect(0,
                (size.y * 0.2).toInt(),
                size.x,
                (size.y * 0.8).toInt())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                Timber.wtf("fling")
                if (rect.contains(
                        if (e1.x < e2.x) (e1.x).toInt() else e2.x.toInt(),
                        if (e1.y < e2.y) (e1.y).toInt() else e2.y.toInt(),
                        if (e1.x < e2.x) (e2.x).toInt() else e1.x.toInt(),
                        if (e1.y < e2.y) (e2.y).toInt() else e1.y.toInt())) {
                    try {
                        if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) {
                            return false
                        }

                        if (e1.x - e2.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            // left swipe
                            mPresenter.next()
                        } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            // right swipe
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }
        })
    }

    @OnClick(R.id.container)
    fun play() {
        mPresenter.playPause()
    }

    @OnTouch()
    fun onTouch(m: MotionEvent): Boolean {
        return mDetector.onTouchEvent(m)
    }

    override fun showError() {
        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
    }

    private var DarkVibrantColor: Int = 0

    private var DarkMutedColor: Int = 0

    private var LightVibrantColor: Int = 0

    override fun showSongInfo(song: Song?) {
        title.text = song?.title
        artist.text = song?.artist
        Timber.wtf("$song")
        var mBitmapCover: Bitmap? = null

        if (song?.art != null) {
            val file = File(song.art)
            if (file.exists()) {
                val input = FileInputStream(file)
                mBitmapCover = BitmapFactory.decodeStream(input)
                input.close()
            } else {
//            throw IOException("setDataSource failed.")
                Timber.wtf("file not exist")
            }
        }
        if (mBitmapCover == null) {
            val drawable: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_music)
            mBitmapCover = drawable.toBitmap()
        }

        art.setImageBitmap(mBitmapCover)
        art.scaleType = ImageView.ScaleType.CENTER_CROP

        val palette = Palette.from(mBitmapCover).generate()
        DarkVibrantColor = palette.getDarkVibrantColor(Color.GRAY)
        DarkMutedColor = palette.getDarkMutedColor(Color.BLACK)
        LightVibrantColor = palette.getLightVibrantColor(Color.WHITE)
        background.setBackgroundColor(DarkMutedColor)
        title.setTextColor(LightVibrantColor)
        artist.setTextColor(LightVibrantColor)
    }

    override fun showPermissionNotGranted() {
        title.setText(getString(R.string.require_permission))
        artist.setText(getString(R.string.permission_reason))
        art.setImageResource(R.drawable.ic_permission)
        art.scaleType = ImageView.ScaleType.CENTER
        container.setOnClickListener {
            checkPermission()
        }
    }

    fun checkPermission() {
        Timber.wtf("check permission")
        RxPermissions(activity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (granted) { // Always true pre-M
                        mPresenter.next()
                        showLoading()
                        container?.setOnClickListener { mPresenter.playPause() }
                    } else {
                        showPermissionNotGranted()
                    }
                })
    }

    override fun showLoading() {
        title.setText(getString(R.string.loading))
        artist.setText("")
    }

    fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return bitmap
        }

        val width = if (intrinsicWidth > 0) intrinsicWidth else 1
        val height = if (intrinsicHeight > 0) intrinsicHeight else 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

}