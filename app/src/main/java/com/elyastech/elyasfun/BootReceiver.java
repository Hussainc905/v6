package com.elyastech.elyasfun;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
        if(!context.getSharedPreferences("elyas_fun",Context.MODE_PRIVATE).getBoolean("enabled",false))return;
        if(!Settings.canDrawOverlays(context))return;
        try{
            Intent service=new Intent(context,FunOverlayService.class);
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
            else context.startService(service);
        }catch(RuntimeException error){
            context.getSharedPreferences("elyas_fun",Context.MODE_PRIVATE).edit()
                .putString("last_error","Autostart blocked: "+error.getClass().getSimpleName()).apply();
        }
    }
}
