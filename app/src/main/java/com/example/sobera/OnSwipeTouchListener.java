package com.example.sobera;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnSwipeTouchListener implements OnTouchListener {

    private final GestureDetector gestureDetector;

    public OnSwipeTouchListener (Context ctx){
        gestureDetector = new GestureDetector(ctx, new MyGestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public class MyGestureListener implements GestureDetector.OnGestureListener{

        private static final long VELOCITY_THRESHOLD = 3000;

        @Override
        public boolean onDown(final MotionEvent e){ return false; }

        @Override
        public void onShowPress(final MotionEvent e){ }

        @Override
        public boolean onSingleTapUp(final MotionEvent e){ return onSimpleClick(); }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
                                final float distanceY){ return false; }

        @Override
        public void onLongPress(final MotionEvent e){ }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                               final float velocityX,
                               final float velocityY){

            if(Math.abs(velocityX) < VELOCITY_THRESHOLD
                    && Math.abs(velocityY) < VELOCITY_THRESHOLD){
                return false;//if the fling is not fast enough then it's just like drag
            }
            boolean result = false;
            //if velocity in X direction is higher than velocity in Y direction,
            //then the fling is horizontal, else->vertical
            if(Math.abs(velocityX) > Math.abs(velocityY)){
                if(velocityX >= 0){
                    //Log.wtf("TAG", "swipe right");
                    result = onSwipeRight();
                }else{//if velocityX is negative, then it's towards left
                    //Log.wtf("TAG", "swipe left");
                    result = onSwipeLeft();
                }
            }
            /*else{
                if(velocityY >= 0){
                    Log.wtf("TAG", "swipe down");
                    result = onSwipeBottom();
                }else{
                    Log.wtf("TAG", "swipe up");
                    result = onSwipeTop();
                }
            }*/

            return result;
        }
    }

    public boolean onSwipeRight() {
        return false;
    }

    public boolean onSwipeLeft() {
        return false;
    }

    public boolean onSimpleClick() {
        return false;
    }

    /*
    //If further implementations need those behaviors, there are here.

    public boolean onSwipeTop() {
        return false;
    }

    public boolean onSwipeBottom() {
        return false;
    }*/


}