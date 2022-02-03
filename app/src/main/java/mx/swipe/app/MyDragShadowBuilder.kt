package mx.swipe.app

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.PixelCopy
import android.view.View
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer


class MyDragShadowBuilder(v: View, val drawable: Drawable?) : View.DragShadowBuilder(v) {
    private val shadow = ColorDrawable(Color.BLUE)
    //private val shadow = DrawableContainer()
    //private val shadow = loadBitmapFromView(v)

    // Defines a callback that sends the drag shadow dimensions and touch point
    // back to the system.
    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        val width: Int = view.width
        val height: Int = view.height
        if (drawable != null)
            drawable.setBounds(0, 0, width, height)
        else
            shadow.setBounds(0, 0, width, height)
        size.set(width, height)
        touch.set(width , height)
    }

    // Defines a callback that draws the drag shadow in a Canvas that the system
    // constructs from the dimensions passed to onProvideShadowMetrics().
    override fun onDrawShadow(canvas: Canvas) {
        if (drawable != null)
            drawable.draw(canvas)
        else
            shadow.draw(canvas)
    }
}