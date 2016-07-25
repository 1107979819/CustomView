package top.wenyl.customview;

/**
 * 作者：Rain Wen
 * 日期：2016/7/25 09:57
 * 功能：
 * 版本：
 * 改进：
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

public class WorksheetView extends View
{
    protected static final int  OVERSCROLL_DISTANCE = 10;
    protected static final int  INVALID_POINTER_ID  = -1;

    private int                 fWorksheetWidth     = 2000;
    private int                 fWorksheetHeight    = 2000;

    private OverScroller        fScroller;
    private VelocityTracker     fVelocityTracker    = null;
    private int                 fMinimumVelocity;

    // The ‘active pointer’ is the one currently moving our object.
    private int                 fTranslatePointerId = INVALID_POINTER_ID;
    private PointF              fTranslateLastTouch = new PointF( );

    private boolean             fInteracting        = false;

    public WorksheetView(Context context, AttributeSet attrs)
    {
        super( context, attrs );
        this.initView( context, attrs );
    }

    public WorksheetView(Context context, AttributeSet attrs, int defStyle)
    {
        super( context, attrs, defStyle );
        this.initView( context, attrs );
    }

    protected void initView(Context context, AttributeSet attrs)
    {
        fScroller = new OverScroller( this.getContext( ) );

        this.setOverScrollMode( OVER_SCROLL_ALWAYS );

        final ViewConfiguration configuration = ViewConfiguration.get( getContext( ) );
        //fTouchSlop = configuration.getScaledTouchSlop( );
        fMinimumVelocity = configuration.getScaledMinimumFlingVelocity( );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if ( fVelocityTracker == null )
        {
            fVelocityTracker = VelocityTracker.obtain( );
        }
        fVelocityTracker.addMovement( event );

        final int action = event.getAction( );
        switch ( action & MotionEvent.ACTION_MASK )
        {
            case MotionEvent.ACTION_DOWN:
            {
                if ( !fScroller.isFinished( ) )
                    fScroller.abortAnimation( );

                final float x = event.getX( );
                final float y = event.getY( );

                fTranslateLastTouch.set( x, y );
                fTranslatePointerId = event.getPointerId( 0 );
                this.startInteracting( );
                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                final int pointerIndexTranslate = event.findPointerIndex( fTranslatePointerId );
                if ( pointerIndexTranslate >= 0 )
                {
                    float translateX = event.getX( pointerIndexTranslate );
                    float translateY = event.getY( pointerIndexTranslate );

                    this.overScrollBy(
                            (int) (fTranslateLastTouch.x - translateX),
                            (int) (fTranslateLastTouch.y - translateY),
                            this.getScrollX( ),
                            this.getScrollY( ),
                            fWorksheetWidth - this.getWidth( ),
                            fWorksheetHeight - this.getHeight( ),
                            OVERSCROLL_DISTANCE,
                            OVERSCROLL_DISTANCE,
                            true );

                    fTranslateLastTouch.set( translateX, translateY );

                    this.invalidate( );
                }

                break;
            }

            case MotionEvent.ACTION_UP:
            {
                final VelocityTracker velocityTracker = fVelocityTracker;
                velocityTracker.computeCurrentVelocity( 1000 );
                //velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialXVelocity = (int) velocityTracker.getXVelocity( );
                int initialYVelocity = (int) velocityTracker.getYVelocity( );

                if ( (Math.abs( initialXVelocity ) + Math.abs( initialYVelocity ) > fMinimumVelocity) )
                {
                    this.fling( -initialXVelocity, -initialYVelocity );
                }
                else
                {
                    if ( fScroller.springBack( this.getScrollX( ), this.getScrollY( ), 0, fWorksheetWidth - this.getWidth( ), 0, fWorksheetHeight - this.getHeight( ) ) )
                        this.invalidate( );

                    this.stopInteracting( );
                }

                if ( fVelocityTracker != null )
                {
                    fVelocityTracker.recycle( );
                    fVelocityTracker = null;
                }


                fTranslatePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
            {
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            {
                final int pointerIndex = (event.getAction( ) & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId( pointerIndex );
                if ( pointerId == fTranslatePointerId )
                {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    fTranslateLastTouch.set( event.getX( newPointerIndex ), event.getY( newPointerIndex ) );
                    fTranslatePointerId = event.getPointerId( newPointerIndex );
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            {
                if ( fScroller.springBack( this.getScrollX( ), this.getScrollY( ), 0, fWorksheetWidth - this.getWidth( ), 0, fWorksheetHeight - this.getHeight( ) ) )
                    this.invalidate( );

                fTranslatePointerId = INVALID_POINTER_ID;
                break;
            }
        }

        return true;
    }

    private void fling(int velocityX, int velocityY)
    {
        int x = this.getScrollX( );
        int y = this.getScrollY( );

        this.startInteracting( );
        //fScroller.setFriction( ViewConfiguration.getScrollFriction( ) );
        fScroller.fling( x, y, velocityX, velocityY, 0, fWorksheetWidth - this.getWidth( ), 0, fWorksheetHeight - this.getHeight( ) );

        this.invalidate( );
    }

    private void startInteracting()
    {
        fInteracting = true;
    }

    private void stopInteracting()
    {
        fInteracting = false;
    }

    @Override
    public void computeScroll()
    {
        if ( fScroller != null && fScroller.computeScrollOffset( ) )
        {
            int oldX = this.getScrollX( );
            int oldY = this.getScrollY( );
            int x = fScroller.getCurrX( );
            int y = fScroller.getCurrY( );

            if ( oldX != x || oldY != y )
            {
                this.overScrollBy(
                        x - oldX,
                        y - oldY,
                        oldX,
                        oldY,
                        fWorksheetWidth - this.getWidth( ),
                        fWorksheetHeight - this.getHeight( ),
                        OVERSCROLL_DISTANCE,
                        OVERSCROLL_DISTANCE,
                        false );
            }

            if ( fScroller.isFinished( ) )
                this.stopInteracting( );

            this.postInvalidate( );
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if ( !fScroller.isFinished( ) )
        {
            super.scrollTo( scrollX, scrollY );

            if ( clampedX || clampedY )
            {
                fScroller.springBack( this.getScrollX( ), this.getScrollY( ), 0, fWorksheetWidth - this.getWidth( ), 0, fWorksheetHeight - this.getHeight( ) );
            }
        }
        else
        {
            super.scrollTo( scrollX, scrollY );
        }
        awakenScrollBars( );
    }

    @Override
    protected int computeHorizontalScrollExtent()
    {
        return this.getWidth( );
    }

    @Override
    protected int computeHorizontalScrollRange()
    {
        return fWorksheetWidth;
    }

    @Override
    protected int computeHorizontalScrollOffset()
    {
        return this.getScrollX( );
    }

    @Override
    protected int computeVerticalScrollExtent()
    {
        return this.getHeight( );
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        return fWorksheetHeight;
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        return this.getScrollY( );
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawColor( Color.BLACK );

        Paint paint = new Paint( );

        if ( fInteracting )
            ;

        paint.setColor( Color.WHITE );
        canvas.drawRect( 0, 0, fWorksheetWidth, fWorksheetHeight, paint );

        paint.setColor( Color.RED );
        for (int i = 0; i < 1500; i += 10)
        {
            canvas.drawLine( i, 0, i + 100, 500, paint );
        }

        canvas.drawRect( fWorksheetWidth - 50, 0, fWorksheetWidth, fWorksheetHeight, paint );
        canvas.drawRect( 0, fWorksheetHeight - 50, fWorksheetWidth, fWorksheetHeight, paint );
    }
}