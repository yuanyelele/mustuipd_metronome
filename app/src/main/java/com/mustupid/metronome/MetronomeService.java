package com.mustupid.metronome;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class MetronomeService extends Service {

    private final IBinder mBinder = new MetronomeBinder();
    private Metronome mMetronome;

    @Override
    public void onCreate() {
        mMetronome = new Metronome(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startMetronome(int tempo, int beats) {
        mMetronome.play(tempo, beats);
    }

    public void stopMetronome() {
        mMetronome.stop();
    }

    public void updateMetronome(int tempo, int beats) {
        mMetronome.update(tempo, beats);
    }

    class MetronomeBinder extends Binder {
        MetronomeService getService() {
            return MetronomeService.this;
        }
    }
}
