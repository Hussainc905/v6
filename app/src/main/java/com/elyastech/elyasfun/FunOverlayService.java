package com.elyastech.elyasfun;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

/** Foreground listener. No Accessibility Service, no lockscreen bypass, no wake locks. */
public final class FunOverlayService extends Service {
    public static final String ACTION_SHOW = "com.elyastech.elyasfun.SHOW";
    public static final String ACTION_STOP = "com.elyastech.elyasfun.STOP";
    private static final String CHANNEL = "elyas_fun_v4_silent_service";
    private static final int NOTIFICATION_ID = 404;
    private static final long MIN_REPLAY_MS = 4500L;
    public static volatile boolean isRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private KeyguardManager keyguard;
    private PowerManager power;
    private BroadcastReceiver unlockListener;
    private ThreeDSceneView animation;
    private Runnable removeTask, unlockTask;
    private boolean playedSinceScreenOff;
    private long lastPlayUptime = -MIN_REPLAY_MS;

    private void event(String key,String value){
        getSharedPreferences("elyas_fun",MODE_PRIVATE).edit().putString(key,value).apply();
    }
    @Override public void onCreate(){
        super.onCreate();
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        power=(PowerManager)getSystemService(POWER_SERVICE);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"ELYAS FUN V4 — خدمة فتح القفل",NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null);channel.enableVibration(false);channel.enableLights(false);
        manager.createNotificationChannel(channel);
        PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("ELYAS FUN V4 3D")
                .setContentText("بانتظار فتح القفل — يمكن الإيقاف من التطبيق")
                .setOngoing(true).setContentIntent(pi).build();
        try {
            startForeground(NOTIFICATION_ID,notification);
        } catch(RuntimeException error){
            event("last_error","Foreground: "+error.getClass().getSimpleName());
            stopSelf();return;
        }
        unlockListener=new BroadcastReceiver(){
            @Override public void onReceive(Context context,Intent received){
                if(received==null||received.getAction()==null)return;
                String action=received.getAction();
                if(Intent.ACTION_SCREEN_OFF.equals(action)){
                    event("last_event","SCREEN_OFF");
                    playedSinceScreenOff=false;
                    cancelCheck();removeOverlay();
                }else if(Intent.ACTION_SCREEN_ON.equals(action)){
                    event("last_event","SCREEN_ON");
                    beginCheck("SCREEN_ON");
                }else if(Intent.ACTION_USER_PRESENT.equals(action)){
                    event("last_event","USER_PRESENT");
                    beginCheck("USER_PRESENT");
                }
            }
        };
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(unlockListener,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(unlockListener,filter);
        isRunning=true;
        event("last_error","");
        event("last_event","SERVICE_STARTED");
    }
    @Override public int onStartCommand(Intent command,int flags,int startId){
        if(command!=null&&ACTION_STOP.equals(command.getAction())){
            stopSelf();return START_NOT_STICKY;
        }
        if(!getSharedPreferences("elyas_fun",MODE_PRIVATE).getBoolean("enabled",false)){
            stopSelf();return START_NOT_STICKY;
        }
        if(command!=null&&ACTION_SHOW.equals(command.getAction()))displayAnimation("MANUAL_TEST");
        return START_STICKY;
    }
    private void beginCheck(String reason){
        cancelCheck();
        if(playedSinceScreenOff)return;
        if(!getSharedPreferences("elyas_fun",MODE_PRIVATE).getBoolean("enabled",false))return;
        final int[] tries={0};
        unlockTask=new Runnable(){
            @Override public void run(){
                if(!isRunning || playedSinceScreenOff)return;
                if(power!=null&&!power.isInteractive())return;
                if(keyguard==null||!keyguard.isKeyguardLocked()){
                    long now=SystemClock.uptimeMillis();
                    if(now-lastPlayUptime>MIN_REPLAY_MS){
                        if(displayAnimation("UNLOCK_"+reason))playedSinceScreenOff=true;
                    }
                    return;
                }
                // Poll for up to 20 seconds because biometric unlock may delay ACTION_USER_PRESENT.
                if(++tries[0]<50)handler.postDelayed(this,400);
                else event("last_error","انتهى انتظار فتح القفل بعد 20 ثانية");
            }
        };
        handler.postDelayed(unlockTask,180);
    }
    private void cancelCheck(){if(unlockTask!=null)handler.removeCallbacks(unlockTask);unlockTask=null;}
    private boolean displayAnimation(String reason){
        if(!Settings.canDrawOverlays(this)){
            event("last_error","صلاحية العرض فوق التطبيقات غير مفعّلة");return false;
        }
        removeOverlay();
        ThreeDSceneView next=new ThreeDSceneView(this,false);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.START|Gravity.TOP;
        try{
            windows.addView(next,params);
            next.onResume();next.play();
            animation=next;
            lastPlayUptime=SystemClock.uptimeMillis();
            event("last_trigger",reason);
            event("last_play",String.valueOf(System.currentTimeMillis()));
            event("last_error","");
            SoundFX.door(this);
            removeTask=this::removeOverlay;
            handler.postDelayed(removeTask,ThreeDSceneView.DURATION_MS+250L);
            return true;
        }catch(RuntimeException error){
            event("last_error","Overlay: "+error.getClass().getSimpleName()+" "+error.getMessage());
            try{windows.removeViewImmediate(next);}catch(RuntimeException ignored){}
            next.stop();next.onPause();
            animation=null;
            return false;
        }
    }
    private void removeOverlay(){
        if(removeTask!=null)handler.removeCallbacks(removeTask);
        removeTask=null;
        if(animation==null)return;
        animation.stop();animation.onPause();
        try{windows.removeViewImmediate(animation);}catch(RuntimeException ignored){}
        animation=null;
    }
    @Override public void onDestroy(){
        isRunning=false;
        cancelCheck();removeOverlay();
        if(unlockListener!=null){try{unregisterReceiver(unlockListener);}catch(RuntimeException ignored){}}
        unlockListener=null;
        event("last_event","SERVICE_STOPPED");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
