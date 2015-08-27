package com.nathanhaze.weathercamera;

import android.app.Application;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class GlobalVar extends Application {

	private int screenWidth;
	private int screenHeight;
	
	private int speed = 0;
	private int altitude = 0;
	
	public int getAltitude(){
		return altitude;
	}
	
	public void setAltutude(int value){
		this.altitude = value;
	}

	public void setScreenWidth(int sw){
		this.screenWidth = sw;
	}
	
	public int getScreenWidth(){
		return screenWidth;
	}
	
	public void setScreenHeight(int sh){
		this.screenHeight =sh;
	}
	
	public int getScreenHeight(){
		return screenHeight;
	}
	
	public void setSpeed(int speed){
		this.speed = speed;
	}
	
	public int getSpeed(){
		return speed;
	}
	
	public Paint getPaint(){
        //    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/GoodDog.otf");
		    Paint paint = new Paint();
		  //  paint.setTypeface(font);
		    paint.setAntiAlias(true);
		    paint.setColor(Color.GREEN);
		    paint.setStyle(Style.FILL);
		    paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
		    return paint;
	}
}
