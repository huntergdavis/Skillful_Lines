package com.hunterdavis.skillfullines;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

class Panel extends SurfaceView implements SurfaceHolder.Callback {
	private static final float EPS = (float) 0.000001;

	private CanvasThread canvasthread;

	int _x = 0;
	int _y = 0;
	int scoreybuffer = 20;
	int scorexbuffer = 80;
	public Boolean surfaceCreated;
	public Boolean mutex = false;
	public Boolean enableDisableMutex = false;
	public Bitmap backingBitmap = null;
	Boolean drawReady = false;
	Boolean generateBoard = true;
	Boolean scoreChanged = false;
	public int score = 0;
	public int lastConnectedPoint = 5;

	public class lineSegment {
		float x1;
		float y1;
		float x2;
		float y2;

		lineSegment(float xa, float ya, float xb, float yb) {
			x1 = xa;
			y1 = ya;
			y2 = yb;
			x2 = xb;
		}
	}

	private Vector xvalues;
	private Vector yvalues;
	private Vector lineSegmentValues;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			addValues((float) event.getX(), (float) event.getY());
		}
		return true;

	}
	
	public void setMutex(Boolean mtx) {
		mutex = mtx;
	}
	
	public void enableKillMutex() {
		enableDisableMutex = true;
	}

	public int getScore() {
		return score;
	}

	public void reset() {
		generateBoard = true;
	}

	private void addValues(float x, float y) {
		int width = backingBitmap.getWidth();
		int height = backingBitmap.getHeight();
		float newx = x;
		float newy = y;
		
		if( (newx > (width - scorexbuffer)) && (newy >= (height - scoreybuffer))) {
			//newx = width - scorexbuffer;
			//newy = height - scoreybuffer;
			Toast.makeText(getContext(), "Your Current Score is "+score+ " points", Toast.LENGTH_SHORT).show();
			return;
		}
		
		for(int i = 0;i<xvalues.size();i++) {
			if(fdistance(newx,newy,(Float)xvalues.get(i),(Float)yvalues.get(i)) < 10){
				Toast.makeText(getContext(), "Too Close To Existing Point!  Try Again!", Toast.LENGTH_SHORT).show();
				return;
			}
		}

		xvalues.addElement(newx);
		yvalues.addElement(newy);
		drawReady = true;
	}
	
	float fdistance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
	}


	public Panel(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 
		surfaceCreated = false;
		xvalues = new Vector();
		yvalues = new Vector();
		lineSegmentValues = new Vector();

		getHolder().addCallback(this);
		setFocusable(true);
	}

	public void createThread(SurfaceHolder holder) {
		canvasthread = new CanvasThread(getHolder(), this);
		canvasthread.setRunning(true);
		canvasthread.start();
	}

	public void terminateThread() {
		canvasthread.setRunning(false);
		try {
			canvasthread.join();
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(mutex == false) {  
			generateBoard = true;
		}
		else if(enableDisableMutex == true) {
			mutex = false;
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// 
		if (surfaceCreated == false) {
			createThread(holder);
			// Bitmap kangoo = BitmapFactory.decodeResource(getResources(),
			// R.drawable.kangoo);

			surfaceCreated = true;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceCreated = false;

	}

	@Override
	public void onDraw(Canvas canvas) {

		Paint paint = new Paint();

		if (generateBoard == true) {

			backingBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
						Bitmap.Config.ARGB_8888);

			canvas.setBitmap(backingBitmap);
			canvas.drawColor(Color.WHITE);

			// clear score
			score = 0;

			// update score view
			scoreChanged = true;

			// clear all x and y values
			xvalues.clear();
			yvalues.clear();
			lineSegmentValues.clear();

			// last connected point
			lastConnectedPoint = 5;

			// add initial random dots to board
			generateInitialDots(canvas);

			generateBoard = false;
		}

		// canvas.drawBitmap(kangoo, 10, 10, null);
		else if (drawReady == true) {

			canvas.setBitmap(backingBitmap);

			int numItems = xvalues.size();
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			Random myrandom = new Random();
			int oldlastConnectedPoint = lastConnectedPoint;

			// draw then connect all points that have yet to be connected
			for (int i = oldlastConnectedPoint; i < numItems; i++) {

				// retrieve the point, this is the one to draw
				float newx = (Float) xvalues.get(i);
				float newy = (Float) yvalues.get(i);

				// draw the new point!
				paint.setColor(Color.rgb(myrandom.nextInt(255),
						myrandom.nextInt(255), myrandom.nextInt(255)));
				canvas.drawRect(newx, newy + 2, newx + 2, newy, paint);

				// draw and calculate line stats for each point in the stack
				// below this one
				for (int j = 0; j < i; j++) {
					drawAndScoreLines(canvas, i, j);
				}
				lastConnectedPoint++;

			}

			drawReady = false;
		}
		
		if(scoreChanged == true) {
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			paint.setColor(Color.WHITE);
			canvas.drawRect(width - scorexbuffer, height - scoreybuffer, width, height, paint);
			
			
			paint.setColor(Color.BLACK);
			canvas.drawText(String.valueOf(score) + " points", width - scorexbuffer, height-4, paint);
		}

		// update screen
		canvas.drawBitmap(backingBitmap, 0, 0, paint);

		// here we call a canvas operation function to add all the cats
		// drawCatsFromVectors(singleUseCanvas);

		// since we drew to the bitmap, display it
		// canvas.drawBitmap(lastGoodBitmap, 0, 0, null);

	}

	private void drawAndScoreLines(Canvas canvas, int newPosition,
			int oldPosition) {
		float x1 = (Float) xvalues.get(oldPosition);
		float x2 = (Float) xvalues.get(newPosition);
		float y1 = (Float) yvalues.get(oldPosition);
		float y2 = (Float) yvalues.get(newPosition);

		Paint paint = new Paint();
		if (doAnyLinesIntersect(x1, y1, x2, y2) == false) {
			int color = updateScoreAndGetLineColorToDraw(x1, y1, x2, y2);
			paint.setColor(color);
			canvas.drawLine(x1, y1, x2, y2, paint);
			lineSegment mySegment = new lineSegment(x1, y1, x2, y2);
			lineSegmentValues.add(mySegment);
		}

	}

	private Boolean doAnyLinesIntersect(float x1, float y1, float x2, float y2) {

		lineSegment localLineSegment = new lineSegment(x1, y1, x2, y2);
		for (int i = 0; i < lineSegmentValues.size(); i++) {
			lineSegment topLineSegment = (lineSegment) lineSegmentValues.get(i);

			 if (intersects(localLineSegment, topLineSegment) == true) {

			
				return true;
			}
		}

		return false;
	}

	private boolean intersects(lineSegment lineSega, lineSegment lineSegb) {

		// First find Ax+By=C values for the two lines
		float A1 = lineSega.y2 - lineSega.y1;
		float B1 = lineSega.x1 - lineSega.x2;
		float C1 = A1 * lineSega.x1 + B1 * lineSega.y1;
		float A2 = lineSegb.y2 - lineSegb.y1;
		float B2 = lineSegb.x1 - lineSegb.x2;
		if ((A2 == 0) || (B2 == 0) || (B1 == 0) || (A1 == 0)) {
			// here we have a zero slope
			return true;
		}
		float C2 = A2 * lineSegb.x1 + B2 * lineSegb.y1;
		float det = (A1 * B2) - (A2 * B1);
		if (Math.abs(det) < EPS) {
			// Lines are either parallel, are collinear (the exact same
			// segment), or are overlapping partially, but not fully
			// To see what the case is, check if the endpoints of one line
			// correctly satisfy the equation of the other (meaning the two
			// lines have the same y-intercept).
			// If no endpoints on 2nd line can be found on 1st, they are
			// parallel.
			// If any can be found, they are either the same segment,
			// overlapping, or two segments of the same line, separated by some
			// distance.
			// Remember that we know they share a slope, so there are no other
			// possibilities
			// Check if the segments lie on the same line
			// (No need to check both points)
			if ((A1 * lineSegb.x1) + (B1 * lineSegb.y1) == C1) {
				// They are on the same line, check if they are in the same
				// space
				// We only need to check one axis - the other will follow
				if ((Math.min(lineSega.x1, lineSega.x2) < lineSegb.x1)
						&& (Math.max(lineSega.x1, lineSega.x2) > lineSegb.x1))
					return true;
				// One end point is ok, now check the other
				if ((Math.min(lineSega.x1, lineSega.x2) < lineSegb.x2)
						&& (Math.max(lineSega.x1, lineSega.x2) > lineSegb.x2))
					return true;
				// They are on the same line, but there is distance between them
				return false;
			}
			// They are simply parallel
			return false;
		} else {
			// Lines DO intersect somewhere, but do the line segments intersect?
			float x = (B2 * C1 - B1 * C2) / det;
			float y = (A1 * C2 - A2 * C1) / det;
			// Make sure that the intersection is within the bounding box of
			// both segments
			if ((x > Math.min(lineSega.x1, lineSega.x2) && x < Math.max(
					lineSega.x1, lineSega.x2))
					&& (y > Math.min(lineSega.y1, lineSega.y2) && y < Math.max(
							lineSega.y1, lineSega.y2))) {
				// We are within the bounding box of the first line segment,
				// so now check second line segment
				if ((x > Math.min(lineSegb.x1, lineSegb.x2) && x < Math.max(
						lineSegb.x1, lineSegb.x2))
						&& (y > Math.min(lineSegb.y1, lineSegb.y2) && y < Math
								.max(lineSegb.y1, lineSegb.y2))) {
					// The line segments do intersect
					return true;
				}
			}
			// The lines do intersect, but the line segments do not
			return false;
		}
	}

	private int updateScoreAndGetLineColorToDraw(float x1, float y1, float x2, float y2) {
		// calculate distance
		float distance = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1)
				* (y2 - y1));
		score += (int) distance;
		scoreChanged = true;
		int red = 0;
		int blue = 0;
		int green = 0;
		
		// if we have a straight line, it's a bonus line and can pass through!
		if((Math.abs(x1 - x2) < EPS) || (Math.abs(y2 - y1) < EPS))
		{
			red = 245;
			green = 129;
			blue = 27;
		}

		if (distance < 10) {
			red = 122;
			blue = 104;
			green = 104;
		} else if (distance < 20) {
			red = 186;
			blue = 114;
			green = 114;
		} else if (distance < 40) {
			red = 222;
			blue = 87;
			green = 87;
		} else if (distance < 60) {
			red = 156;
			blue = 140;
			green = 98;
		} else if (distance < 80) {
			red = 179;
			blue = 53;
			green = 82;
		} else if (distance < 120) {
			red = 99;
			blue = 173;
			green = 114;
		} else if (distance < 160) {
			red = 32;
			blue = 232;
			green = 72;
		} else if (distance < 210) {
			red = 92;
			green = 141;
			blue = 150;
		} else if (distance < 310) {
			red = 92;
			green = 150;
			blue = 143;
		} else if (distance < 510) {
			red = 92;
			green = 150;
			blue = 193;
		} else if (distance < 710) {
			red = 183;
			green = 189;
			blue = 66;
		} 

		return Color.rgb(red, green, blue);
	}

	private void generateInitialDots(Canvas canvas) {
		Paint paint = new Paint();

		Random myrandom = new Random();
		float newx = 0;
		float newy = 0;
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		for (int i = 0; i < 5; i++) {
			newx = myrandom.nextInt((width));
			newy = myrandom.nextInt((height));
			
			if( (newx > (width - scorexbuffer)) && (newy >= (height - scoreybuffer))) {
				newx = width - scorexbuffer - myrandom.nextInt(5);
				newy = height - scoreybuffer - myrandom.nextInt(5);
			}
			
			
			int red = myrandom.nextInt(255);
			int green = myrandom.nextInt(255);
			int blue = myrandom.nextInt(255);

			paint.setColor(Color.rgb(red, green, blue));
			canvas.drawRect(newx, newy + 2, newx + 2, newy, paint);
			// localcanvas.drawPoint(newx, newy, paint);
			xvalues.add(newx);
			yvalues.add(newy);
		}

	}
	
	public Boolean saveImage(Context context, View v) {

		// terminate the running thread and join the data
		terminateThread();

		// now save out the file holmes!
		OutputStream outStream = null;
		String newFileName = "Skillful-Lines-Score-"+score+".png";
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();

		if (newFileName != null) {
			File file = new File(extStorageDirectory, newFileName);
			try {
				outStream = new FileOutputStream(file);

				// here we save out our last known good bitmap
				// Panel mypanel = (Panel) findViewById(R.id.SurfaceView01);

				// int left = getLeft();
				// int right = getRight();
				// int top = getTop();
				// int bottom = getBottom();

				// mypanel.setDrawingCacheEnabled(true);
				// mypanel.onLayout(false, left, top, right, bottom);
				// lastGoodBitmap = Bitmap.createBitmap( getWidth(),
				// getHeight(), Bitmap.Config.ARGB_8888);
				// Canvas mycanv = new Canvas(lastGoodBitmap);
				// View view = (View) findViewById(R.id.SurfaceView01);
				// view.draw(mycanv);
				// setDrawingCacheEnabled(true);
				// onLayout(true, left, top, right, bottom);

				// draw(mycanv);
				// lastGoodBitmap = getDrawingCache();
				// setDrawingCacheEnabled(false);

				backingBitmap.compress(Bitmap.CompressFormat.PNG, 100,
						outStream);

				try {
					outStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					createThread(getHolder());
					return false;
				}
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					createThread(getHolder());
					return false;
				}

				Toast.makeText(context, "Saved " + newFileName,
						Toast.LENGTH_LONG).show();
				new SingleMediaScanner(context, file);

			} catch (FileNotFoundException e) {
				// do something if errors out?

				createThread(getHolder());
				return false;
			}
		}

		createThread(getHolder());
		return true;

	}


} // end class