package com.elyastech.elyasfun;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

/** V4 control screen with genuine service status and last unlocked event. */
public final class MainActivity extends Activity {
    private ThreeDSceneView preview;
    private TextView status,diagnostics;
    private final int BG=0xFF0B1120,WHITE=0xFFF5F8FF,PALE=0xFFC5D3ED;
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable back(int a,int b,int r){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
        g.setCornerRadius(dp(r));return g;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);
        v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return v;
    }
    private void addText(LinearLayout root,String s,int size,int color,boolean bold,int bottom){
        TextView v=text(s,size,color,bold);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);
        root.addView(v,p);
    }
    private void btn(LinearLayout root,String s,boolean primary,View.OnClickListener onClick){
        TextView b=text(s,15,WHITE,true);
        b.setBackground(primary?back(0xFF365DE4,0xFF8B56CE,15):back(0xFF222C46,0xFF1B273E,15));
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.bottomMargin=dp(9);
        root.addView(b,p);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(21),dp(18),dp(24));
        body.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(body);setContentView(scroll);
        addText(body,"ELYAS  •  FUN  •  3D",13,0xFF7EBBFF,true,8);
        addText(body,"مغامرة تنظيف الشاشة",25,WHITE,true,6);
        addText(body,"V4.0  |  Realme UI 7  |  مجسمات OpenGL حقيقية",12,0xFFA6B7D3,false,12);
        FrameLayout frame=new FrameLayout(this);
        frame.setBackground(back(0xFF33425F,0xFF151C31,18));
        frame.setClipToOutline(true);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(222),dp(369));
        fp.bottomMargin=dp(14);body.addView(frame,fp);
        preview=new ThreeDSceneView(this,true);
        frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        status=text("",15,WHITE,true);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.bottomMargin=dp(7);
        body.addView(status,sp);
        diagnostics=text("",12,0xFF9EB1CE,false);diagnostics.setLineSpacing(dp(3),1f);
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,-2);dpv.bottomMargin=dp(12);
        body.addView(diagnostics,dpv);
        btn(body,"▶ تجربة المشهد ثلاثي الأبعاد",true,v->{preview.play();SoundFX.door(this);});
        btn(body,"✦ اختبار الظهور فوق الشاشة",false,v->activate(true));
        btn(body,"● تفعيل التشغيل بعد فتح القفل",true,v->activate(false));
        btn(body,"■ إيقاف الخدمة",false,v->deactivate());
        btn(body,"⚙ إعدادات الصلاحيات والبطارية",false,v->appSettings());
        btn(body,"↻ تحديث حالة الخدمة",false,v->updateStatus());
        LinearLayout soundRow=new LinearLayout(this);soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);soundRow.setPadding(dp(14),dp(6),dp(10),dp(6));
        soundRow.setBackground(back(0xFF24344F,0xFF15243C,13));
        TextView label=text("صوت فتح الباب فقط",15,WHITE,true);
        soundRow.addView(label,new LinearLayout.LayoutParams(0,dp(46),1));
        Switch sound=new Switch(this);sound.setChecked(getSharedPreferences("elyas_fun",MODE_PRIVATE)
                .getBoolean("door_sound",true));
        sound.setThumbTintList(ColorStateList.valueOf(0xFFE3EEFF));
        sound.setOnCheckedChangeListener((b,checked)->getSharedPreferences("elyas_fun",MODE_PRIVATE)
                .edit().putBoolean("door_sound",checked).apply());
        soundRow.addView(sound);
        LinearLayout.LayoutParams sr=new LinearLayout.LayoutParams(-1,-2);sr.bottomMargin=dp(13);
        body.addView(soundRow,sr);
        addText(body,"القصة: يفتح الباب وهو متضايق شويّة، يمشي ويحمل سلّماً قابلاً للطي، يصعد وينظف مرتين بمكانين مختلفين، وبعدها ينزل ويلتفت ويسويلك باي باي ويرجع.",13,PALE,false,10);
        addText(body,"هذه نسخة 3D إجرائية للتجربة؛ ليست فيديو بمستوى فيلم Shaun the Sheep. التشغيل بعد فتح القفل يحتاج صلاحية العرض فوق التطبيقات وخدمة مستمرة، وقد تمنعه قيود Android 16.",12,0xFF91A3BD,false,10);
        updateStatus();
    }
    private boolean overlayAllowed(){return Settings.canDrawOverlays(this);}
    private boolean requestOverlay(){
        if(overlayAllowed())return true;
        Toast.makeText(this,"اسمح بالظهور فوق التطبيقات ثم ارجع واضغط التشغيل",Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())));
        return false;
    }
    private void permission(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},71);
        }
    }
    private void activate(boolean show){
        if(!requestOverlay())return;
        permission();
        getSharedPreferences("elyas_fun",MODE_PRIVATE).edit().putBoolean("enabled",true).apply();
        Intent i=new Intent(this,FunOverlayService.class);
        if(show)i.setAction(FunOverlayService.ACTION_SHOW);
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
        catch(RuntimeException error){
            getSharedPreferences("elyas_fun",MODE_PRIVATE).edit()
                    .putString("last_error",error.getClass().getSimpleName()).apply();
            Toast.makeText(this,"فشل تشغيل الخدمة: "+error.getClass().getSimpleName(),Toast.LENGTH_LONG).show();
        }
        status.postDelayed(this::updateStatus,750);
    }
    private void deactivate(){
        getSharedPreferences("elyas_fun",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();
        stopService(new Intent(this,FunOverlayService.class));
        status.postDelayed(this::updateStatus,250);
    }
    private void appSettings(){
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:"+getPackageName())));
    }
    private void updateStatus(){
        if(status==null)return;
        boolean enabled=getSharedPreferences("elyas_fun",MODE_PRIVATE).getBoolean("enabled",false);
        boolean running=FunOverlayService.isRunning;
        String msg=running?"● الخدمة تعمل وتنتظر فتح القفل":(enabled?"⚠ الخيار مفعّل، لكن الخدمة متوقفة":"○ التشغيل التلقائي متوقف");
        status.setText(msg);status.setTextColor(running?0xFF77E6B1:enabled?0xFFFFC972:PALE);
        String last=getSharedPreferences("elyas_fun",MODE_PRIVATE).getString("last_event","لا توجد أحداث بعد");
        String reason=getSharedPreferences("elyas_fun",MODE_PRIVATE).getString("last_trigger","-");
        String error=getSharedPreferences("elyas_fun",MODE_PRIVATE).getString("last_error","");
        String stamp=getSharedPreferences("elyas_fun",MODE_PRIVATE).getString("last_play","");
        String time="-";
        try{if(!stamp.isEmpty())time=DateFormat.getDateTimeInstance().format(new Date(Long.parseLong(stamp)));}
        catch(NumberFormatException ignored){}
        diagnostics.setText("آخر حدث: "+last+"\nآخر تشغيل: "+time+"  ("+reason+")"
                +"\nالصلاحية: "+(overlayAllowed()?"مسموحة":"غير مسموحة")
                +(error.isEmpty()?"":"\nآخر خطأ: "+error));
    }
    @Override protected void onResume(){super.onResume();if(preview!=null){preview.onResume();preview.play();}updateStatus();}
    @Override protected void onPause(){if(preview!=null)preview.onPause();super.onPause();}
}
