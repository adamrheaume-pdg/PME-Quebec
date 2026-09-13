package quebec.culture.donnees;
import android.content.Context; import android.graphics.*; import android.view.View;
public class DataBarView extends View{
 private int good=80,warn=15,bad=5; private final Paint p=new Paint(1);
 public DataBarView(Context c){super(c); setMinimumHeight(56);} public void setValues(int g,int w,int b){good=g;warn=w;bad=b;invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),total=Math.max(1,good+warn+bad),x=0; p.setColor(Color.rgb(32,145,86));float a=w*good/total;c.drawRoundRect(x,8,x+a,h-8,12,12,p);x+=a;p.setColor(Color.rgb(245,166,35));float d=w*warn/total;c.drawRect(x,8,x+d,h-8,p);x+=d;p.setColor(Color.rgb(205,64,64));c.drawRoundRect(x,8,w,h-8,12,12,p);}
}
