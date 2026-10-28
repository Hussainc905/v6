package com.elyastech.elyasfun;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

/** Only one optional effect is ever emitted: the door opening. No music or steps. */
public final class SoundFX {
    private SoundFX() { }
    public static void door(Context context) {
        if (!context.getSharedPreferences("elyas_fun",Context.MODE_PRIVATE)
                .getBoolean("door_sound",true)) return;
        try {
            MediaPlayer player=MediaPlayer.create(context.getApplicationContext(),R.raw.door_open);
            if (player==null) return;
            player.setOnCompletionListener(MediaPlayer::release);
            player.setOnErrorListener((mp,what,extra)->{mp.release();return true;});
            player.setVolume(.75f,.75f);
            player.start();
        } catch(RuntimeException ignored) {
            // Audio must never interrupt the animation.
        }
    }
}
