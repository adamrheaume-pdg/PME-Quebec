package quebec.social360;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

final class Ui {
    static final int BG = Color.rgb(7,17,31);
    static final int SURFACE = Color.rgb(14,28,46);
    static final int TEXT = Color.rgb(248,250,252);
    static final int MUTED = Color.rgb(167,180,198);
    static final int BLUE = Color.rgb(23,139,255);

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView v = new TextView(c); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8)); return v;
    }
    static Button button(Context c, String s) {
        Button b = new Button(c); b.setText(s); b.setTextColor(TEXT); b.setTextSize(14); b.setAllCaps(false);
        b.setBackgroundColor(BLUE); return b;
    }
    static LinearLayout col(Context c){ LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c){ LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static Space space(Context c,int h){Space s=new Space(c);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(c,h)));return s;}
    static int dp(Context c,int x){return (int)(x*c.getResources().getDisplayMetrics().density+.5f);}
}
