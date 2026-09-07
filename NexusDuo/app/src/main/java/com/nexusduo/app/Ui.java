package com.nexusduo.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public final class Ui {
    private Ui(){}

    public static void setupLightSystemBars(Activity a, View root, int left, int top, int right, int bottom){
        Window w=a.getWindow();
        w.setStatusBarColor(Color.WHITE);
        w.setNavigationBarColor(Color.WHITE);
        if(Build.VERSION.SDK_INT>=30){
            WindowInsetsController c=w.getInsetsController();
            if(c!=null)c.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            );
        }else{
            int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if(Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        }
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int l=0,t=0,r=0,b=0;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                l=i.left;t=i.top;r=i.right;b=i.bottom;
            }else{
                l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left+l,top+t,right+r,bottom+b);
            return insets;
        });
        root.requestApplyInsets();
    }
}
