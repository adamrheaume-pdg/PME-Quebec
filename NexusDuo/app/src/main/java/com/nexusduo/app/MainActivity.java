package com.nexusduo.app;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;

public class MainActivity extends Activity {
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);} 
    TextView tv(String s,int sp,int c){ TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setGravity(Gravity.CENTER);return t; }
    GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(8,10,15));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(22),dp(54),dp(22),dp(22));root.setBackgroundColor(Color.rgb(8,10,15));
        TextView logo=tv("NEXUS",42,Color.WHITE);logo.setTypeface(null,1);root.addView(logo,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView duo=tv("DUO",15,Color.rgb(124,92,252));duo.setLetterSpacing(.35f);root.addView(duo,new LinearLayout.LayoutParams(-1,dp(34)));
        TextView sub=tv("Vos réseaux. Vos espaces. Une seule app.",15,Color.rgb(156,163,175));root.addView(sub,new LinearLayout.LayoutParams(-1,dp(70)));
        root.addView(profile("A","MON ESPACE", "Profil 1",0), new LinearLayout.LayoutParams(-1,dp(120)));
        Space sp=new Space(this);root.addView(sp,new LinearLayout.LayoutParams(1,dp(16)));
        root.addView(profile("M","SON ESPACE", "Profil 2",1), new LinearLayout.LayoutParams(-1,dp(120)));
        Space flex=new Space(this);root.addView(flex,new LinearLayout.LayoutParams(1,0,1));
        TextView foot=tv("PRIVÉ • LOCAL • SANS COMPTE NEXUS",11,Color.rgb(100,107,122));foot.setLetterSpacing(.12f);root.addView(foot,new LinearLayout.LayoutParams(-1,dp(40)));setContentView(root);
    }
    View profile(String letter,String title,String label,int id){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(18),0,dp(18),0);card.setBackground(bg(Color.rgb(21,25,37),22));
        TextView av=tv(letter,25,Color.WHITE);av.setTypeface(null,1);av.setBackground(bg(id==0?Color.rgb(124,92,252):Color.rgb(236,72,153),50));card.addView(av,new LinearLayout.LayoutParams(dp(68),dp(68)));
        LinearLayout text=new LinearLayout(this);text.setOrientation(LinearLayout.VERTICAL);text.setPadding(dp(18),0,0,0);TextView a=tv(title,17,Color.WHITE);a.setGravity(Gravity.LEFT);a.setTypeface(null,1);TextView c=tv(label+"  •  Appuyer pour entrer",12,Color.rgb(156,163,175));c.setGravity(Gravity.LEFT);text.addView(a,new LinearLayout.LayoutParams(-1,dp(34)));text.addView(c,new LinearLayout.LayoutParams(-1,dp(28)));card.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView arr=tv("›",34,Color.rgb(124,92,252));card.addView(arr,new LinearLayout.LayoutParams(dp(35),dp(60)));card.setOnClickListener(v->{getSharedPreferences("nexus",0).edit().putInt("profile",id).apply();startActivity(new Intent(this,DashboardActivity.class));});return card; }
}
