// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.mobile.sample.passportreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.SurfaceView;

import com.abbyy.mobile.rtr.IDataCaptureService;

// Surface View, отрисовывающий рамку и границы результатов
// Surface View displaying frame and results boundaries
class SurfaceViewWithOverlay extends SurfaceView {

	private float framePadding = 20;
	private float frameThickness;
	private Point[] fieldsQuads;
	private Rect areaOfInterest;
	private int scaleNominatorX = 1;
	private int scaleDenominatorX = 1;
	private int scaleNominatorY = 1;
	private int scaleDenominatorY = 1;
	private Paint lineBoundariesPaint;
	private Paint framePaint;
	private Paint stableFramePaint;
	private boolean isStable = false;
	private boolean hasPhoto = true;

	// Pre-allocated objects, used in drawing
	Path path = new Path();

	public SurfaceViewWithOverlay( Context context )
	{
		super( context );
		this.setWillNotDraw( false );

		frameThickness = context.getResources().getDimension( R.dimen.frame_thickness );

		lineBoundariesPaint = new Paint();
		lineBoundariesPaint.setStyle( Paint.Style.STROKE );
		lineBoundariesPaint.setColor( context.getResources().getColor( R.color.line_boundaries ) );

		// Стабильность результата влияет на цвет рамки, поэтому два цвета
		// The results stability influences the frame color. That's why two paints are used.
		framePaint = new Paint();
		framePaint.setStyle( Paint.Style.FILL_AND_STROKE );
		framePaint.setColor( context.getResources().getColor( R.color.frame_not_stable ) );

		stableFramePaint = new Paint();
		stableFramePaint.setStyle( Paint.Style.FILL_AND_STROKE );
		stableFramePaint.setColor( context.getResources().getColor( R.color.frame_stable ) );

		isStable = false;

	}

	public void setScaleX( int nominator, int denominator )
	{
		scaleNominatorX = nominator;
		scaleDenominatorX = denominator;
	}

	public void setScaleY( int nominator, int denominator )
	{
		scaleNominatorY = nominator;
		scaleDenominatorY = denominator;
	}

	public void setAreaOfInterest( Rect newValue )
	{
		areaOfInterest = newValue;
		invalidate();
	}

	public Rect getAreaOfInterest()
	{
		return areaOfInterest;
	}

	public void setLines( IDataCaptureService.DataField fields[] )
	{
		if( fields != null && scaleDenominatorX > 0 && scaleDenominatorY > 0 ) {
			fieldsQuads = new Point[fields.length * 4];
			for( int i = 0; i < fields.length; i++ ) {
				IDataCaptureService.DataField field = fields[i];
				for( int j = 0; j < 4; j++ ) {
					fieldsQuads[i * 4 + j] = transformPoint( field.Quadrangle[j] );
				}
			}

		}

		this.invalidate();
	}

	public boolean isStable()
	{
		return isStable;
	}

	public void setStable( boolean stable )
	{
		isStable = stable;
	}

	public boolean hasPhoto()
	{
		return hasPhoto;
	}

	public void setDrawPhotoInPreview( boolean hasPhoto )
	{
		this.hasPhoto = hasPhoto;
	}

	public void setFramePaddingInPreview( float framePadding )
	{
		this.framePadding = framePadding;
	}

	// Преобразует точку в координаты поверхности
	// Transforms point to canvas coordinates
	private Point transformPoint( Point point )
	{
		return new Point(
			( scaleNominatorX * point.x ) / scaleDenominatorX,
			( scaleNominatorY * point.y ) / scaleDenominatorY
		);
	}

	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );
		canvas.save();

		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		if( areaOfInterest != null ) {
			left = ( areaOfInterest.left * scaleNominatorX ) / scaleDenominatorX;
			right = ( areaOfInterest.right * scaleNominatorX ) / scaleDenominatorX;
			top = ( areaOfInterest.top * scaleNominatorY ) / scaleDenominatorY;
			bottom = ( areaOfInterest.bottom * scaleNominatorY ) / scaleDenominatorY;

			Paint paint = isStable ? stableFramePaint : framePaint;

			canvas.drawRect( left + framePadding, top + framePadding, right - framePadding,
				top + framePadding + frameThickness, paint );
			canvas.drawRect( left + framePadding, bottom - frameThickness - framePadding, right - framePadding,
				bottom - framePadding, paint );
			canvas.drawRect( left + framePadding, top + framePadding + frameThickness,
				left + framePadding + frameThickness, bottom - framePadding - frameThickness, paint );
			canvas.drawRect( right - framePadding - frameThickness, top + framePadding + frameThickness,
				right - framePadding, bottom - framePadding - frameThickness, paint );

			if( hasPhoto ) {
				float photoY = ( bottom + top ) / 2;
				float photoHeight = photoY - top;
				float photoWidth = photoHeight * 3.0f / 4;
				float photoXstart = left + framePadding + 2 * frameThickness;

				canvas.drawRect( photoXstart, photoY - photoHeight / 2,
					photoXstart + photoWidth, photoY + photoHeight / 2, paint );
			}

			float numberWidth = 2 * frameThickness;
			float numberRight = right - framePadding - 1.5f * frameThickness;
			float numberYPadding = framePadding + 1.5f * frameThickness;
			canvas.drawRect( numberRight - numberWidth, top + numberYPadding,
				numberRight, bottom - numberYPadding, paint );
		}

		// If there is any result
		if( fieldsQuads != null ) {
			for( int i = 0; i < fieldsQuads.length; i += 4 ) {
				drawBoundary( canvas, i, fieldsQuads );
			}
		}
		canvas.restore();
	}

	private void drawBoundary( Canvas canvas, int j, Point[] quads )
	{
		path.reset();
		Point p = quads[j + 0];
		path.moveTo( p.x, p.y );
		p = quads[j + 1];
		path.lineTo( p.x, p.y );
		p = quads[j + 2];
		path.lineTo( p.x, p.y );
		p = quads[j + 3];
		path.lineTo( p.x, p.y );
		path.close();
		canvas.drawPath( path, lineBoundariesPaint );
	}

}