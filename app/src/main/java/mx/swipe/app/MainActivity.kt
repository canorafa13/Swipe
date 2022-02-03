package mx.swipe.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.animation.TranslateAnimation
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.marginStart
import mx.swipe.app.databinding.ActivityMainBinding

import android.R
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.view.*
import androidx.annotation.RequiresApi
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.flexbox.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.net.URLEncoder


class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityMainBinding
    private val COMPLETE = 1F
    private val HIDE = 0.1F
    private lateinit var mDetector: GestureDetectorCompat
    private val DEBUG_TAG = "Gesture"
    val displayMetrics = DisplayMetrics()
    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mDetector = GestureDetectorCompat(this, this)


        windowManager.defaultDisplay.getMetrics(displayMetrics)

        binding.show.setOnClickListener {
            openAnimation()
        }

        binding.hide.setOnClickListener {
            closeAnimation()
        }

        //val background: Drawable = activity.getResources().getDrawable(R.drawable.gradient_theme)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(android.R.color.transparent, null)
        //window.navigationBarColor = resources.getColor(android.R.color.transparent, null)
        //window.setBackgroundDrawable(background)
        binding.container1.setOnTouchListener { v, event ->  onTouchEvent(event)}


        binding.recycler.apply {
            layoutManager = FlexboxLayoutManager(this@MainActivity).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
                alignItems = AlignItems.FLEX_START
                flexWrap = FlexWrap.WRAP
            }
            setHasFixedSize(true)
            adapter = Adapter(listOf(
                Item("Hola 1", true),
                Item("Hola 2", false),
                Item("Hola 3", false),
                Item("Hola 4", false),
                Item("Hola 5", false),
                Item("Hola 6", false),
                Item("Hola 7", false),
                Item("Hola 8", false),
                Item("Hola 9", false),
                Item("Hola 10", false),
                Item("Hola 11", false),
                Item("Hola 12", false),
                Item("Hola 13", false),
                Item("Hola 14", false),
                Item("Hola 15", false),
                Item("Hola 16", false),
                Item("Hola 17", false)
            ), resources.displayMetrics.widthPixels)
        }



        binding.zTag.apply {
            setBackground("#FFFFFF")
            setTextColor("#818181")
            setTextAndLogo("Noticias", "http://tvtolive.com/wp-content/uploads/ADN-40-tvtolive.com_.jpg")
            setCountBadge(15)
            setAction(View.OnClickListener {
                //val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + "email_to"))
                //intent.putExtra(Intent.EXTRA_SUBJECT, "email_subject")
                //intent.putExtra(Intent.EXTRA_TEXT, "email_body")
  /*              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
                intent.type = "message/rfc822";
                val chooser = Intent.createChooser(intent, "Abrir email")
                startActivity(chooser)
*/

                val checkPackages = listOf("com.citrix.mail.droid", "com.zenprise")


                /*val location = Uri.parse("geo:19.3045596,-99.2060263")
                val mapIntent = Intent(Intent.ACTION_VIEW, location)

                // Try to invoke the intent.
                try {
                    startActivity(mapIntent)
                } catch (e: ActivityNotFoundException) {
                    // Define what your app should do if no activity can handle the intent.
                }*/
            })
        }


    }

    @Throws(Exception::class)
    private fun openPackage(packageName: String, packageName2: String){
        if (isPackageInstalled(packageName, application.packageManager)) {
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
        }else{
            Toast.makeText(this@MainActivity, "No esta instalado", Toast.LENGTH_LONG).show()
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    private var isOpen: Boolean = false
    private var MOVING: Boolean = false
    private val VELOCITY_ANIMATION = 300L
    private fun openAnimation(){
        if (!MOVING) {
            MOVING = true
            isOpen = true

            ObjectAnimator.ofPropertyValuesHolder(
                binding.container1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0F, -(displayMetrics.widthPixels * .9).toFloat())
            ).setDuration(VELOCITY_ANIMATION).start()

            Handler().postDelayed({
                MOVING = false
            }, VELOCITY_ANIMATION)
        }
    }

    private fun closeAnimation(){
        if (!MOVING) {
            MOVING = true
            isOpen = false

            ObjectAnimator.ofPropertyValuesHolder(
                binding.container1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -(displayMetrics.widthPixels * .9).toFloat(), 0F)
            ).setDuration(VELOCITY_ANIMATION).start()
            Handler().postDelayed({
                MOVING = false
            }, VELOCITY_ANIMATION)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        //Log.e(DEBUG_TAG, "onDown: $event")
        return true
    }

    override fun onFling(
        event1: MotionEvent,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        //Log.e(DEBUG_TAG, "onFling: $event1 $event2")
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        //Log.e(DEBUG_TAG, "onLongPress: $event")
    }

    override fun onScroll(
        event1: MotionEvent,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if(event1.action == MotionEvent.ACTION_DOWN && event2.action == MotionEvent.ACTION_MOVE){
            if (event1.x > event2.x) {
                if(!isOpen) {
                    openAnimation()
                }
            }else{
                /**CLOSE*/
                if (binding.container1.translationX != 0F) {
                    closeAnimation()
                }

            }
        }

        ///Log.e(DEBUG_TAG, "onScroll: $event1 $event2")
        return true
    }

    override fun onShowPress(event: MotionEvent) {
        //Log.e(DEBUG_TAG, "onShowPress: $event")
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        //Log.e(DEBUG_TAG, "onSingleTapUp: $event")
        return true
    }
}