package com.nexusduo.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class SplashActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView logo=new ImageView(this);
        logo.setImageResource(com.nexusduo.app.R.drawable.nexus_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(logo,new FrameLayout.LayoutParams(-1,-1));

        setContentView(root);
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            startActivity(new Intent(this,MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            finish();
        },2000);
    }
}
