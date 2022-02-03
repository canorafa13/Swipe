package mx.custom.views.SwipeReveal

import android.util.DisplayMetrics
import androidx.customview.widget.ViewDragHelper
import androidx.core.view.ViewCompat
import android.view.MotionEvent
import android.view.GestureDetector
import androidx.core.view.GestureDetectorCompat
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View
import android.view.ViewGroup
import mx.custom.views.R
import java.lang.RuntimeException


class SwipeRevealLayout : ViewGroup {
    /**
     * Main view is the view which is shown when the layout is closed.
     */
    private lateinit var mMainView: View

    /**
     * Secondary view is the view which is shown when the layout is opened.
     */
    private var mSecondaryView: View? = null

    /**
     * The rectangle position of the main view when the layout is closed.
     */
    private val mRectMainClose: Rect = Rect()

    /**
     * The rectangle position of the main view when the layout is opened.
     */
    private val mRectMainOpen: Rect = Rect()

    /**
     * The rectangle position of the secondary view when the layout is closed.
     */
    private val mRectSecClose: Rect = Rect()

    /**
     * The rectangle position of the secondary view when the layout is opened.
     */
    private val mRectSecOpen: Rect = Rect()

    /**
     * The minimum distance (px) to the closest drag edge that the SwipeRevealLayout
     * will disallow the parent to intercept touch event.
     */
    private var mMinDistRequestDisallowParent = 0
    private var mIsOpenBeforeInit = false

    @Volatile
    private var mAborted = false

    @Volatile
    private var mIsScrolling = false

    /**
     * @return true if the drag/swipe motion is currently locked.
     */
    @Volatile
    var isDragLocked = false
        private set
    /**
     * Get the minimum fling velocity to cause the layout to open/close.
     * @return dp per second
     */
    /**
     * Set the minimum fling velocity to cause the layout to open/close.
     * @param velocity dp per second
     */
    var minFlingVelocity = DEFAULT_MIN_FLING_VELOCITY
    private var mState = STATE_CLOSE
    private var mMode = MODE_NORMAL
    private var mLastMainLeft = 0
    private var mLastMainTop = 0
    /**
     * Get the edge where the layout can be dragged from.
     * @return Can be one of these
     *
     *  * [.DRAG_EDGE_LEFT]
     *  * [.DRAG_EDGE_TOP]
     *  * [.DRAG_EDGE_RIGHT]
     *  * [.DRAG_EDGE_BOTTOM]
     *
     */
    /**
     * Set the edge where the layout can be dragged from.
     * @param dragEdge Can be one of these
     *
     *  * [.DRAG_EDGE_LEFT]
     *  * [.DRAG_EDGE_TOP]
     *  * [.DRAG_EDGE_RIGHT]
     *  * [.DRAG_EDGE_BOTTOM]
     *
     */
    var dragEdge = DRAG_EDGE_LEFT
    private var mDragDist = 0f
    private var mPrevX = -1f
    private var mPrevY = -1f
    private var mDragHelper: ViewDragHelper? = null
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mDragStateChangeListener // only used for ViewBindHelper
            : DragStateChangeListener? = null
    private var mSwipeListener: SwipeListener? = null
    private var mOnLayoutCount = 0

    interface DragStateChangeListener {
        fun onDragStateChanged(state: Int)
    }

    /**
     * Listener for monitoring events about swipe layout.
     */
    interface SwipeListener {
        /**
         * Called when the main view becomes completely closed.
         */
        fun onClosed(view: SwipeRevealLayout?)

        /**
         * Called when the main view becomes completely opened.
         */
        fun onOpened(view: SwipeRevealLayout?)

        /**
         * Called when the main view's position changes.
         * @param slideOffset The new offset of the main view within its range, from 0-1
         */
        fun onSlide(view: SwipeRevealLayout?, slideOffset: Float)
    }

    /**
     * No-op stub for [SwipeListener]. If you only want ot implement a subset
     * of the listener methods, you can extend this instead of implement the full interface.
     */
    class SimpleSwipeListener : SwipeListener {
        override fun onClosed(view: SwipeRevealLayout?) {}
        override fun onOpened(view: SwipeRevealLayout?) {}
        override fun onSlide(view: SwipeRevealLayout?, slideOffset: Float) {}
    }

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector!!.onTouchEvent(event)
        mDragHelper!!.processTouchEvent(event)
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDragLocked) {
            return super.onInterceptTouchEvent(ev)
        }
        mDragHelper!!.processTouchEvent(ev)
        mGestureDetector!!.onTouchEvent(ev)
        accumulateDragDist(ev)
        val couldBecomeClick = couldBecomeClick(ev)
        val settling = mDragHelper!!.viewDragState == ViewDragHelper.STATE_SETTLING
        val idleAfterScrolled = (mDragHelper!!.viewDragState == ViewDragHelper.STATE_IDLE
                && mIsScrolling)

        // must be placed as the last statement
        mPrevX = ev.x
        mPrevY = ev.y

        // return true => intercept, cannot trigger onClick event
        return !couldBecomeClick && (settling || idleAfterScrolled)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // get views
        if (childCount >= 2) {
            mSecondaryView = getChildAt(0)
            mMainView = getChildAt(1)
        } else if (childCount == 1) {
            mMainView = getChildAt(0)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mAborted = false
        for (index in 0 until childCount) {
            val child: View = getChildAt(index)
            var left: Int
            var right: Int
            var top: Int
            var bottom: Int
            bottom = 0
            top = bottom
            right = top
            left = right
            val minLeft = paddingLeft
            val maxRight = (r - paddingRight - l).coerceAtLeast(0)
            val minTop = paddingTop
            val maxBottom = (b - paddingBottom - t).coerceAtLeast(0)
            var measuredChildHeight: Int = child.measuredHeight
            var measuredChildWidth: Int = child.measuredWidth

            // need to take account if child size is match_parent
            val childParams: LayoutParams = child.layoutParams
            var matchParentHeight = false
            var matchParentWidth = false
            matchParentHeight = childParams.height == LayoutParams.MATCH_PARENT ||
                    childParams.height == LayoutParams.MATCH_PARENT
            matchParentWidth = childParams.width == LayoutParams.MATCH_PARENT ||
                    childParams.width == LayoutParams.MATCH_PARENT
            if (matchParentHeight) {
                measuredChildHeight = maxBottom - minTop
                childParams.height = measuredChildHeight
            }
            if (matchParentWidth) {
                measuredChildWidth = maxRight - minLeft
                childParams.width = measuredChildWidth
            }
            when (dragEdge) {
                DRAG_EDGE_RIGHT -> {
                    left = (r - measuredChildWidth - paddingRight - l).coerceAtLeast(minLeft)
                    top = paddingTop.coerceAtMost(maxBottom)
                    right = (r - paddingRight - l).coerceAtLeast(minLeft)
                    bottom = (measuredChildHeight + paddingTop).coerceAtMost(maxBottom)
                }
                DRAG_EDGE_LEFT -> {
                    left = paddingLeft.coerceAtMost(maxRight)
                    top = paddingTop.coerceAtMost(maxBottom)
                    right = (measuredChildWidth + paddingLeft).coerceAtMost(maxRight)
                    bottom = (measuredChildHeight + paddingTop).coerceAtMost(maxBottom)
                }
                DRAG_EDGE_TOP -> {
                    left = paddingLeft.coerceAtMost(maxRight)
                    top = paddingTop.coerceAtMost(maxBottom)
                    right = (measuredChildWidth + paddingLeft).coerceAtMost(maxRight)
                    bottom = (measuredChildHeight + paddingTop).coerceAtMost(maxBottom)
                }
                DRAG_EDGE_BOTTOM -> {
                    left = paddingLeft.coerceAtMost(maxRight)
                    top = (b - measuredChildHeight - paddingBottom - t).coerceAtLeast(minTop)
                    right = (measuredChildWidth + paddingLeft).coerceAtMost(maxRight)
                    bottom = (b - paddingBottom - t).coerceAtLeast(minTop)
                }
            }
            child.layout(left, top, right, bottom)
        }

        // taking account offset when mode is SAME_LEVEL
        if (mMode == MODE_SAME_LEVEL) {
            when (dragEdge) {
                DRAG_EDGE_LEFT -> mSecondaryView?.offsetLeftAndRight(-mSecondaryView!!.width)
                DRAG_EDGE_RIGHT -> mSecondaryView?.offsetLeftAndRight(mSecondaryView!!.width)
                DRAG_EDGE_TOP -> mSecondaryView?.offsetTopAndBottom(-mSecondaryView!!.height)
                DRAG_EDGE_BOTTOM -> mSecondaryView?.offsetTopAndBottom(mSecondaryView!!.height)
            }
        }
        initRects()
        if (mIsOpenBeforeInit) {
            open(false)
        } else {
            close(false)
        }
        mLastMainLeft = mMainView.left
        mLastMainTop = mMainView.top
        mOnLayoutCount++
    }

    /**
     * {@inheritDoc}
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (childCount < 2) {
            throw RuntimeException("Layout must have two children")
        }
        val params = layoutParams
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var desiredWidth = 0
        var desiredHeight = 0

        // first find the largest child
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            desiredWidth = child.measuredWidth.coerceAtLeast(desiredWidth)
            desiredHeight = child.measuredHeight.coerceAtLeast(desiredHeight)
        }
        // create new measure spec using the largest child width
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, widthMode)
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, heightMode)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            val childParams: LayoutParams = child.layoutParams
            if (childParams != null) {
                if (childParams.height == LayoutParams.MATCH_PARENT) {
                    child.minimumHeight = measuredHeight
                }
                if (childParams.width == LayoutParams.MATCH_PARENT) {
                    child.minimumWidth = measuredWidth
                }
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            desiredWidth = child.measuredWidth.coerceAtLeast(desiredWidth)
            desiredHeight = child.measuredHeight.coerceAtLeast(desiredHeight)
        }

        // taking accounts of padding
        desiredWidth += paddingLeft + paddingRight
        desiredHeight += paddingTop + paddingBottom

        // adjust desired width
        if (widthMode == MeasureSpec.EXACTLY) {
            desiredWidth = measuredWidth
        } else {
            if (params.width == LayoutParams.MATCH_PARENT) {
                desiredWidth = measuredWidth
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                desiredWidth = if (desiredWidth > measuredWidth) measuredWidth else desiredWidth
            }
        }

        // adjust desired height
        if (heightMode == MeasureSpec.EXACTLY) {
            desiredHeight = measuredHeight
        } else {
            if (params.height == LayoutParams.MATCH_PARENT) {
                desiredHeight = measuredHeight
            }
            if (heightMode == MeasureSpec.AT_MOST) {
                desiredHeight =
                    if (desiredHeight > measuredHeight) measuredHeight else desiredHeight
            }
        }
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun computeScroll() {
        if (mDragHelper!!.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     * Open the panel to show the secondary view
     * @param animation true to animate the open motion. [SwipeListener] won't be
     * called if is animation is false.
     */
    fun open(animation: Boolean) {
        mIsOpenBeforeInit = true
        mAborted = false
        if (animation) {
            mState = STATE_OPENING
            mDragHelper!!.smoothSlideViewTo(mMainView, mRectMainOpen.left, mRectMainOpen.top)
            if (mDragStateChangeListener != null) {
                mDragStateChangeListener!!.onDragStateChanged(mState)
            }
        } else {
            mState = STATE_OPEN
            mDragHelper!!.abort()
            mMainView.layout(
                mRectMainOpen.left,
                mRectMainOpen.top,
                mRectMainOpen.right,
                mRectMainOpen.bottom
            )
            mSecondaryView?.layout(
                mRectSecOpen.left,
                mRectSecOpen.top,
                mRectSecOpen.right,
                mRectSecOpen.bottom
            )
        }
        ViewCompat.postInvalidateOnAnimation(this@SwipeRevealLayout)
    }

    /**
     * Close the panel to hide the secondary view
     * @param animation true to animate the close motion. [SwipeListener] won't be
     * called if is animation is false.
     */
    fun close(animation: Boolean) {
        mIsOpenBeforeInit = false
        mAborted = false
        if (animation) {
            mState = STATE_CLOSING
            mDragHelper!!.smoothSlideViewTo(mMainView, mRectMainClose.left, mRectMainClose.top)
            if (mDragStateChangeListener != null) {
                mDragStateChangeListener!!.onDragStateChanged(mState)
            }
        } else {
            mState = STATE_CLOSE
            mDragHelper!!.abort()
            mMainView.layout(
                mRectMainClose.left,
                mRectMainClose.top,
                mRectMainClose.right,
                mRectMainClose.bottom
            )
            mSecondaryView?.layout(
                mRectSecClose.left,
                mRectSecClose.top,
                mRectSecClose.right,
                mRectSecClose.bottom
            )
        }
        ViewCompat.postInvalidateOnAnimation(this@SwipeRevealLayout)
    }

    fun setSwipeListener(listener: SwipeListener?) {
        mSwipeListener = listener
    }

    /**
     * @param lock if set to true, the user cannot drag/swipe the layout.
     */
    fun setLockDrag(lock: Boolean) {
        isDragLocked = lock
    }

    /**
     * @return true if layout is fully opened, false otherwise.
     */
    val isOpened: Boolean
        get() = (mState == STATE_OPEN)

    /**
     * @return true if layout is fully closed, false otherwise.
     */
    val isClosed: Boolean
        get() = (mState == STATE_CLOSE)

    /** Only used for [ViewBinderHelper]  */
    fun setDragStateChangeListener(listener: DragStateChangeListener?) {
        mDragStateChangeListener = listener
    }

    /** Abort current motion in progress. Only used for [ViewBinderHelper]  */
    fun abort() {
        mAborted = true
        mDragHelper!!.abort()
    }

    /**
     * In RecyclerView/ListView, onLayout should be called 2 times to display children views correctly.
     * This method check if it've already called onLayout two times.
     * @return true if you should call [.requestLayout].
     */
    fun shouldRequestLayout(): Boolean {
        return mOnLayoutCount < 2
    }

    private val mainOpenLeft: Int
        private get() = when (dragEdge) {
            DRAG_EDGE_LEFT -> mRectMainClose.left + mSecondaryView!!.width
            DRAG_EDGE_RIGHT -> mRectMainClose.left - mSecondaryView!!.width
            DRAG_EDGE_TOP -> mRectMainClose.left
            DRAG_EDGE_BOTTOM -> mRectMainClose.left
            else -> 0
        }
    private val mainOpenTop: Int
        private get() {
            return when (dragEdge) {
                DRAG_EDGE_LEFT -> mRectMainClose.top
                DRAG_EDGE_RIGHT -> mRectMainClose.top
                DRAG_EDGE_TOP -> mRectMainClose.top + mSecondaryView!!.height
                DRAG_EDGE_BOTTOM -> mRectMainClose.top - mSecondaryView!!.height
                else -> 0
            }
        }
    private val secOpenLeft: Int
        private get() {
            if (mMode == MODE_NORMAL || dragEdge == DRAG_EDGE_BOTTOM || dragEdge == DRAG_EDGE_TOP) {
                return mRectSecClose.left
            }
            return if (dragEdge == DRAG_EDGE_LEFT) {
                mRectSecClose.left + mSecondaryView!!.width
            } else {
                mRectSecClose.left - mSecondaryView!!.width
            }
        }
    private val secOpenTop: Int
        private get() {
            if (mMode == MODE_NORMAL || dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                return mRectSecClose.top
            }
            return if (dragEdge == DRAG_EDGE_TOP) {
                mRectSecClose.top + mSecondaryView!!.height
            } else {
                mRectSecClose.top - mSecondaryView!!.height
            }
        }

    private fun initRects() {
        // close position of main view
        mRectMainClose.set(
            mMainView.left,
            mMainView.top,
            mMainView.right,
            mMainView.bottom
        )

        // close position of secondary view
        mRectSecClose.set(
            mSecondaryView!!.left,
            mSecondaryView!!.top,
            mSecondaryView!!.right,
            mSecondaryView!!.bottom
        )

        // open position of the main view
        mRectMainOpen.set(
            mainOpenLeft,
            mainOpenTop,
            mainOpenLeft + mMainView.width,
            mainOpenTop + mMainView.height
        )

        // open position of the secondary view
        mRectSecOpen.set(
            secOpenLeft,
            secOpenTop,
            secOpenLeft + mSecondaryView!!.width,
            secOpenTop + mSecondaryView!!.height
        )
    }

    private fun couldBecomeClick(ev: MotionEvent): Boolean {
        return isInMainView(ev) && !shouldInitiateADrag()
    }

    private fun isInMainView(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        val withinVertical = mMainView.getTop() <= y && y <= mMainView.getBottom()
        val withinHorizontal = mMainView.getLeft() <= x && x <= mMainView.getRight()
        return withinVertical && withinHorizontal
    }

    private fun shouldInitiateADrag(): Boolean {
        val minDistToInitiateDrag = mDragHelper!!.touchSlop.toFloat()
        return mDragDist >= minDistToInitiateDrag
    }

    private fun accumulateDragDist(ev: MotionEvent) {
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN) {
            mDragDist = 0f
            return
        }
        val dragHorizontally = dragEdge == DRAG_EDGE_LEFT ||
                dragEdge == DRAG_EDGE_RIGHT
        val dragged: Float
        dragged = if (dragHorizontally) {
            Math.abs(ev.x - mPrevX)
        } else {
            Math.abs(ev.y - mPrevY)
        }
        mDragDist += dragged
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        if (attrs != null && context != null) {
            val a: TypedArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SwipeRevealLayout,
                0, 0
            )
            dragEdge = a.getInteger(R.styleable.SwipeRevealLayout_dragEdge, DRAG_EDGE_LEFT)
            minFlingVelocity = a.getInteger(
                R.styleable.SwipeRevealLayout_flingVelocity,
                DEFAULT_MIN_FLING_VELOCITY
            )
            mMode = a.getInteger(R.styleable.SwipeRevealLayout_mode, MODE_NORMAL)
            mMinDistRequestDisallowParent = a.getDimensionPixelSize(
                R.styleable.SwipeRevealLayout_minDistRequestDisallowParent,
                dpToPx(DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT)
            )
        }
        mDragHelper = ViewDragHelper.create(this, 1.0f, mDragHelperCallback)
        mDragHelper?.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL)
        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
    }

    private val mGestureListener: GestureDetector.OnGestureListener =
        object : SimpleOnGestureListener() {
            var hasDisallowed = false
            override fun onDown(e: MotionEvent): Boolean {
                mIsScrolling = false
                hasDisallowed = false
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                mIsScrolling = true
                return false
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                mIsScrolling = true
                if (parent != null) {
                    val shouldDisallow: Boolean
                    if (!hasDisallowed) {
                        shouldDisallow = distToClosestEdge >= mMinDistRequestDisallowParent
                        if (shouldDisallow) {
                            hasDisallowed = true
                        }
                    } else {
                        shouldDisallow = true
                    }

                    // disallow parent to intercept touch event so that the layout will work
                    // properly on RecyclerView or view that handles scroll gesture.
                    parent.requestDisallowInterceptTouchEvent(shouldDisallow)
                }
                return false
            }
        }
    private val distToClosestEdge: Int
        private get() {
            when (dragEdge) {
                DRAG_EDGE_LEFT -> {
                    val pivotRight: Int = mRectMainClose.left + mSecondaryView!!.getWidth()
                    return Math.min(
                        mMainView.getLeft() - mRectMainClose.left,
                        pivotRight - mMainView.getLeft()
                    )
                }
                DRAG_EDGE_RIGHT -> {
                    val pivotLeft: Int = mRectMainClose.right - mSecondaryView!!.getWidth()
                    return Math.min(
                        mMainView.getRight() - pivotLeft,
                        mRectMainClose.right - mMainView.getRight()
                    )
                }
                DRAG_EDGE_TOP -> {
                    val pivotBottom: Int = mRectMainClose.top + mSecondaryView!!.getHeight()
                    return Math.min(
                        mMainView.getBottom() - pivotBottom,
                        pivotBottom - mMainView.getTop()
                    )
                }
                DRAG_EDGE_BOTTOM -> {
                    val pivotTop: Int = mRectMainClose.bottom - mSecondaryView!!.getHeight()
                    return Math.min(
                        mRectMainClose.bottom - mMainView.getBottom(),
                        mMainView.getBottom() - pivotTop
                    )
                }
            }
            return 0
        }
    private val halfwayPivotHorizontal: Int
        private get() {
            return if (dragEdge == DRAG_EDGE_LEFT) {
                mRectMainClose.left + mSecondaryView!!.getWidth() / 2
            } else {
                mRectMainClose.right - mSecondaryView!!.getWidth() / 2
            }
        }
    private val halfwayPivotVertical: Int
        private get() {
            return if (dragEdge == DRAG_EDGE_TOP) {
                mRectMainClose.top + mSecondaryView!!.getHeight() / 2
            } else {
                mRectMainClose.bottom - mSecondaryView!!.getHeight() / 2
            }
        }
    private val mDragHelperCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            mAborted = false
            if (isDragLocked) return false
            mDragHelper!!.captureChildView(mMainView, pointerId)
            return false
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return when (dragEdge) {
                DRAG_EDGE_TOP -> Math.max(
                    Math.min(top, mRectMainClose.top + mSecondaryView!!.getHeight()),
                    mRectMainClose.top
                )
                DRAG_EDGE_BOTTOM -> Math.max(
                    Math.min(top, mRectMainClose.top),
                    mRectMainClose.top - mSecondaryView!!.getHeight()
                )
                else -> child.getTop()
            }
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return when (dragEdge) {
                DRAG_EDGE_RIGHT -> Math.max(
                    Math.min(left, mRectMainClose.left),
                    mRectMainClose.left - mSecondaryView!!.getWidth()
                )
                DRAG_EDGE_LEFT -> Math.max(
                    Math.min(left, mRectMainClose.left + mSecondaryView!!.getWidth()),
                    mRectMainClose.left
                )
                else -> child.getLeft()
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val velRightExceeded: Boolean = pxToDp(xvel.toInt()) >= minFlingVelocity
            val velLeftExceeded: Boolean = pxToDp(xvel.toInt()) <= -minFlingVelocity
            val velUpExceeded: Boolean = pxToDp(yvel.toInt()) <= -minFlingVelocity
            val velDownExceeded: Boolean = pxToDp(yvel.toInt()) >= minFlingVelocity
            val pivotHorizontal = halfwayPivotHorizontal
            val pivotVertical = halfwayPivotVertical
            when (dragEdge) {
                DRAG_EDGE_RIGHT -> if (velRightExceeded) {
                    close(true)
                } else if (velLeftExceeded) {
                    open(true)
                } else {
                    if (mMainView.getRight() < pivotHorizontal) {
                        open(true)
                    } else {
                        close(true)
                    }
                }
                DRAG_EDGE_LEFT -> if (velRightExceeded) {
                    open(true)
                } else if (velLeftExceeded) {
                    close(true)
                } else {
                    if (mMainView.getLeft() < pivotHorizontal) {
                        close(true)
                    } else {
                        open(true)
                    }
                }
                DRAG_EDGE_TOP -> if (velUpExceeded) {
                    close(true)
                } else if (velDownExceeded) {
                    open(true)
                } else {
                    if (mMainView.getTop() < pivotVertical) {
                        close(true)
                    } else {
                        open(true)
                    }
                }
                DRAG_EDGE_BOTTOM -> if (velUpExceeded) {
                    open(true)
                } else if (velDownExceeded) {
                    close(true)
                } else {
                    if (mMainView.getBottom() < pivotVertical) {
                        open(true)
                    } else {
                        close(true)
                    }
                }
            }
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            super.onEdgeDragStarted(edgeFlags, pointerId)
            if (isDragLocked) {
                return
            }
            val edgeStartLeft = (dragEdge == DRAG_EDGE_RIGHT
                    && edgeFlags == ViewDragHelper.EDGE_LEFT)
            val edgeStartRight = (dragEdge == DRAG_EDGE_LEFT
                    && edgeFlags == ViewDragHelper.EDGE_RIGHT)
            val edgeStartTop = (dragEdge == DRAG_EDGE_BOTTOM
                    && edgeFlags == ViewDragHelper.EDGE_TOP)
            val edgeStartBottom = (dragEdge == DRAG_EDGE_TOP
                    && edgeFlags == ViewDragHelper.EDGE_BOTTOM)
            if (edgeStartLeft || edgeStartRight || edgeStartTop || edgeStartBottom) {
                mDragHelper!!.captureChildView(mMainView, pointerId)
            }
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (mMode == MODE_SAME_LEVEL) {
                if (dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                    mSecondaryView?.offsetLeftAndRight(dx)
                } else {
                    mSecondaryView?.offsetTopAndBottom(dy)
                }
            }
            val isMoved =
                mMainView.getLeft() !== mLastMainLeft || mMainView.getTop() !== mLastMainTop
            if (mSwipeListener != null && isMoved) {
                if (mMainView.getLeft() === mRectMainClose.left && mMainView.getTop() === mRectMainClose.top) {
                    mSwipeListener!!.onClosed(this@SwipeRevealLayout)
                } else if (mMainView.getLeft() === mRectMainOpen.left && mMainView.getTop() === mRectMainOpen.top) {
                    mSwipeListener!!.onOpened(this@SwipeRevealLayout)
                } else {
                    mSwipeListener!!.onSlide(this@SwipeRevealLayout, slideOffset)
                }
            }
            mLastMainLeft = mMainView.getLeft()
            mLastMainTop = mMainView.getTop()
            ViewCompat.postInvalidateOnAnimation(this@SwipeRevealLayout)
        }

        private val slideOffset: Float
            private get() {
                return when (dragEdge) {
                    DRAG_EDGE_LEFT -> (mMainView.getLeft() - mRectMainClose.left) as Float / mSecondaryView!!.getWidth()
                    DRAG_EDGE_RIGHT -> ((mRectMainClose.left - mMainView.left) / mSecondaryView!!.width).toFloat()
                    DRAG_EDGE_TOP -> (mMainView.getTop() - mRectMainClose.top) as Float / mSecondaryView!!.getHeight()
                    DRAG_EDGE_BOTTOM -> (mRectMainClose.top - mMainView.getTop()) as Float / mSecondaryView!!.getHeight()
                    else -> 0F
                }
            }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            val prevState = mState
            when (state) {
                ViewDragHelper.STATE_DRAGGING -> mState = STATE_DRAGGING
                ViewDragHelper.STATE_IDLE ->
                    // drag edge is left or right
                    mState = if (dragEdge == DRAG_EDGE_LEFT || dragEdge == DRAG_EDGE_RIGHT) {
                        if (mMainView.getLeft() === mRectMainClose.left) {
                            STATE_CLOSE
                        } else {
                            STATE_OPEN
                        }
                    } else {
                        if (mMainView.getTop() === mRectMainClose.top) {
                            STATE_CLOSE
                        } else {
                            STATE_OPEN
                        }
                    }
            }
            if (mDragStateChangeListener != null && !mAborted && prevState != mState) {
                mDragStateChangeListener!!.onDragStateChanged(mState)
            }
        }
    }

    private fun pxToDp(px: Int): Int {
        val resources: Resources = context.resources
        val metrics: DisplayMetrics = resources.getDisplayMetrics()
        return (px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        val resources: Resources = context.resources
        val metrics: DisplayMetrics = resources.getDisplayMetrics()
        return (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    companion object {
        // These states are used only for ViewBindHelper
        const val STATE_CLOSE = 0
        const val STATE_CLOSING = 1
        const val STATE_OPEN = 2
        const val STATE_OPENING = 3
        const val STATE_DRAGGING = 4
        const val DEFAULT_MIN_FLING_VELOCITY = 300 // dp per second
        private const val DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT = 1 // dp
        const val DRAG_EDGE_LEFT = 0x1
        const val DRAG_EDGE_RIGHT = 0x1 shl 1
        const val DRAG_EDGE_TOP = 0x1 shl 2
        const val DRAG_EDGE_BOTTOM = 0x1 shl 3

        /**
         * The secondary view will be under the main view.
         */
        const val MODE_NORMAL = 0

        /**
         * The secondary view will stick the edge of the main view.
         */
        const val MODE_SAME_LEVEL = 1
        fun getStateString(state: Int): String {
            return when (state) {
                STATE_CLOSE -> "state_close"
                STATE_CLOSING -> "state_closing"
                STATE_OPEN -> "state_open"
                STATE_OPENING -> "state_opening"
                STATE_DRAGGING -> "state_dragging"
                else -> "undefined"
            }
        }
    }
}
