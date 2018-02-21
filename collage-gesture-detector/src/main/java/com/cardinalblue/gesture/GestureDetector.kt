// Copyright Feb 2017-present CardinalBlue
//
// Author: boy@cardinalblue.com
//         jack.huang@cardinalblue.com
//         yolung.lu@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.cardinalblue.gesture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.ViewConfiguration

import com.cardinalblue.gesture.state.BaseGestureState
import com.cardinalblue.gesture.state.DragState
import com.cardinalblue.gesture.state.IdleState
import com.cardinalblue.gesture.state.MultipleFingersPressingState
import com.cardinalblue.gesture.state.PinchState
import com.cardinalblue.gesture.state.SingleFingerPressingState

/**
 * Creates a GestureDetector with the supplied listener.
 * You may only use this constructor from a [android.os.Looper] thread.
 *
 * @param context the application's context
 * @param touchSlop The range where a gesture is identified if the finger
 * movement is beyond.
 * @param tapSlop The range that a TAP is identified if the finger movement is
 * still within.
 * @param minFlingVec The lower bound of the finger movement for a FLING.
 * @param maxFlingVec The upper bound of the finger movement for a FLING.
 *
 * @see android.os.Handler
 */
class GestureDetector(context: Context,
                      touchSlop: Float,
                      tapSlop: Float,
                      minFlingVec: Float,
                      maxFlingVec: Float)
    : Handler.Callback,
      IGestureStateOwner {

    private var mTouchSlopSquare: Int = 0
    private var mTapSlopSquare: Int = 0
    private var mMinFlingVelocity: Int = 0
    private var mMaxFlingVelocity: Int = 0

    override val listener: IAllGesturesListener? = ListenerBridge()
    override val handler: Handler by lazy { GestureHandler(this) }

    private var mState: BaseGestureState? = null
    private val mIdleState: IdleState
    private val mSingleFingerPressingState: SingleFingerPressingState
    private val mMultipleFingersPressingState: MultipleFingersPressingState
    private val mDragState: DragState
    private val mPinchState: PinchState

    init {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException(
                "The detector should be always initialized on the main thread.")
        }

        // Init properties like touch slop square, tap slop square, ..., etc.
        init(context, touchSlop, tapSlop, minFlingVec, maxFlingVec)

        // TODO: Configure the policy of factory of the factor.

        // Internal states.
        mIdleState = IdleState(this)
        mSingleFingerPressingState = SingleFingerPressingState(
            this,
            mTapSlopSquare.toLong(), mTouchSlopSquare.toLong(),
            TAP_TIMEOUT.toLong(), LONG_PRESS_TIMEOUT.toLong())
        mMultipleFingersPressingState = MultipleFingersPressingState(this, mTouchSlopSquare)
        mDragState = DragState(this, mMinFlingVelocity, mMaxFlingVelocity)
        mPinchState = PinchState(this)

        // Init IDLE state.
        mState = mIdleState
    }

    override fun issueStateTransition(newState: IGestureStateOwner.State,
                                      event: MotionEvent,
                                      target: Any?,
                                      context: Any?) {
        val oldState = mState
        oldState?.onExit(event, target, context)

        // TODO: Use a factor to produce reusable internal states.
        mState = when (newState) {
            IGestureStateOwner.State.STATE_IDLE -> mIdleState
            IGestureStateOwner.State.STATE_SINGLE_FINGER_PRESSING -> mSingleFingerPressingState
            IGestureStateOwner.State.STATE_DRAG -> mDragState
            IGestureStateOwner.State.STATE_MULTIPLE_FINGERS_PRESSING -> mMultipleFingersPressingState
            IGestureStateOwner.State.STATE_PINCH -> mPinchState
            else -> mIdleState
        }

        // Enter new state.
        mState!!.onEnter(event, target, context)
    }

    override fun handleMessage(msg: Message): Boolean {
        // Delegate to state.
        return mState!!.onHandleMessage(msg)
    }

    fun setIsTapEnabled(enabled: Boolean) {
        mSingleFingerPressingState.isTapEnabled = enabled
    }

    /**
     * Set whether long-press is enabled, if this is enabled when a user
     * presses and holds down you get a long-press event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * long-press is enabled.
     *
     * @param enabled whether long-press should be enabled.
     */
    fun setIsLongPressEnabled(enabled: Boolean) {
        mSingleFingerPressingState.isTapEnabled = enabled
    }

    fun setIsMultitouchEnabled(enabled: Boolean) {
        mSingleFingerPressingState.setIsTransitionToMultiTouchEnabled(enabled)
        mIdleState.setIsTransitionToMultiTouchEnabled(enabled)
        mDragState.setIsTransitionToMultiTouchEnabled(enabled)
    }

    fun resetConfig() {
        mSingleFingerPressingState.isLongPressEnabled = true
        mSingleFingerPressingState.isTapEnabled = true
        mSingleFingerPressingState.setIsTransitionToMultiTouchEnabled(true)
        mIdleState.setIsTransitionToMultiTouchEnabled(true)
        mDragState.setIsTransitionToMultiTouchEnabled(true)
    }

//    var gestureLifecycleListener: IGestureLifecycleListener?
//        get() = getListenerBridge().lifecycleListener
//        set(value) {
//            getListenerBridge().lifecycleListener = value
//        }

    var tapGestureListener: ITapGestureListener?
        get() = getListenerBridge().tapListener
        set(value) {
            getListenerBridge().tapListener = value
        }

    var dragGestureListener: IDragGestureListener?
        get() = getListenerBridge().dragListener
        set(value) {
            getListenerBridge().dragListener = value
        }

    var pinchGestureListener: IPinchGestureListener?
        get() = getListenerBridge().pinchListener
        set(value) {
            getListenerBridge().pinchListener = value
        }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the [IAllGesturesListener]
     * supplied.
     *
     * @param event The current motion event.
     * @return true if the [IAllGesturesListener] consumed
     * the event, else false.
     */
    fun onTouchEvent(event: MotionEvent,
                     target: Any?,
                     context: Any?): Boolean {
        mState!!.onDoing(event, target, context)

        return mSingleFingerPressingState.isTapEnabled ||
               mSingleFingerPressingState.isLongPressEnabled
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun init(context: Context,
                     touchSlop: Float,
                     tapSlop: Float,
                     minFlingVec: Float,
                     maxFlingVec: Float) {
        val configuration = ViewConfiguration.get(context)
        val newTouchSlop = Math.min(touchSlop, configuration.scaledTouchSlop.toFloat())
        val newTapSlop = Math.min(tapSlop, configuration.scaledDoubleTapSlop.toFloat())

        mTouchSlopSquare = (newTouchSlop * newTouchSlop).toInt()
        mTapSlopSquare = (newTapSlop * newTapSlop).toInt()

        mMinFlingVelocity = Math.max(minFlingVec,
                                     configuration.scaledMinimumFlingVelocity.toFloat()).toInt()
        mMaxFlingVelocity = Math.max(maxFlingVec,
                                     configuration.scaledMaximumFlingVelocity.toFloat()).toInt()
    }

    private fun getListenerBridge(): ListenerBridge {
        return this.listener as ListenerBridge
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private class GestureHandler internal constructor(callback: Handler.Callback)
        : Handler(callback)

    companion object {

        /**
         * Defines the default duration in milliseconds before a press turns into
         * a long press.
         */
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
        /**
         * The duration in milliseconds we will wait to see if a touch event is a
         * tap or a scroll. If the user does not move within this interval, it is
         * considered to be a tap.
         */
        private val TAP_TIMEOUT = Math.max(150, ViewConfiguration.getTapTimeout())
    }
}
