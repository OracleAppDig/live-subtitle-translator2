package com.example.livepersiansubtitle;
import android.app.*;import android.content.*;import android.media.projection.MediaProjectionManager;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.Gravity;import android.widget.*;
public class MainActivity extends Activity{
 static final int CAP=77;
 public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(30,40,30,30);l.setGravity(Gravity.CENTER);
  TextView t=new TextView(this);t.setText("زیرنویس زنده فارسی\n\nنسخه رایگان روی دستگاه");t.setTextSize(24);t.setGravity(Gravity.CENTER);l.addView(t);
  TextView n=new TextView(this);n.setText("مدل تشخیص گفتار و ترجمه روی گوشی اجرا می‌شوند. اینترنت فقط برای دانلود مدل‌ها لازم است.");n.setTextSize(16);l.addView(n);
  Button ov=new Button(this);ov.setText("فعال‌سازی نمایش شناور");ov.setOnClickListener(v->{if(!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));});l.addView(ov);
  Button st=new Button(this);st.setText("شروع ترجمه زنده");st.setOnClickListener(v->{if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"اول نمایش شناور را فعال کن",Toast.LENGTH_LONG).show();return;}MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),CAP);});l.addView(st);
  Button sp=new Button(this);sp.setText("توقف");sp.setOnClickListener(v->stopService(new Intent(this,CaptureService.class)));l.addView(sp);
  setContentView(l);
 }
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==CAP&&c==RESULT_OK&&d!=null){Intent i=new Intent(this,CaptureService.class);i.putExtra("code",c);i.putExtra("data",d);startForegroundService(i);}}
}