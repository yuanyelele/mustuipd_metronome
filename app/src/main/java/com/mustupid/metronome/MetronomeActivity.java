package com.mustupid.metronome;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mustupid.metronome.MetronomeService.MetronomeBinder;

public class MetronomeActivity extends AppCompatActivity {
    static final int MIN_TEMPO = 40;
    private static final int MAX_TEMPO = 208;
    private static final int DEFAULT_TEMPO = 120;

    private static final int[] VALID_BEATS = {0, 2, 3, 4, 6};
    private static final int DEFAULT_BEATS = 0;
    private ToggleButton mStartStopButton;
    private boolean mRunning;
    private int mTempo;
    private int mBeats;
    private VerticalSeekBar mTempoSeekBar;
    private SharedPreferences mPreferences;
    private MetronomeService mMetronomeService;
    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MetronomeBinder binder = (MetronomeBinder) service;
            mMetronomeService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.metronome);

        // Make volume button always control just the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Load stored persistent data
        mPreferences = getSharedPreferences("Metronome", Activity.MODE_PRIVATE);
        mTempo = mPreferences.getInt("tempo", DEFAULT_TEMPO);
        mBeats = mPreferences.getInt("beats", DEFAULT_BEATS);

        setUpStartStopButton();
        setUpBeatsControls();
        setUpTempoControls();
        TextView mTempoDisplay = findViewById(R.id.tempo_display);
        mTempoDisplay.setText(Integer.toString(mTempo));
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(
                new Intent(MetronomeActivity.this, MetronomeService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt("tempo", mTempo);
        editor.putInt("beats", mBeats);
        editor.apply();
        final ToggleButton startStopButton = findViewById(R.id.metronome_start_button);
        if (mRunning)
            startStopButton.performClick();
    }

    private void setUpStartStopButton() {
        mStartStopButton = findViewById(R.id.metronome_start_button);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRunning = !mRunning;
                if (mRunning) {
                    mMetronomeService.startMetronome(mTempo, VALID_BEATS[mBeats]);
                    mStartStopButton.setTextColor(getResources().getColor(R.color.stop_red));
                } else {
                    mMetronomeService.stopMetronome();
                    mStartStopButton.setTextColor(getResources().getColor(R.color.start_green));
                }

            }
        });
    }

    private void setUpTempoControls() {
        mTempoSeekBar = findViewById(R.id.tempo_seekbar);
        mTempoSeekBar.setMax(MAX_TEMPO - MIN_TEMPO + 1);
        mTempoSeekBar.setProgress(mTempo - MIN_TEMPO);
        mTempoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTempo(progress + MIN_TEMPO);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateTempo(seekBar.getProgress() + MIN_TEMPO);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setUpBeatsControls() {
        NumberPicker beatsPicker = findViewById(R.id.beats_number_picker);
        beatsPicker.setMaxValue(VALID_BEATS.length - 1);
        String[] string_array = new String[VALID_BEATS.length];
        int i = 0;
        for (int beat : VALID_BEATS) {
            string_array[i] = Integer.toString(beat);
            i++;
        }
        beatsPicker.setDisplayedValues(string_array);
        beatsPicker.setWrapSelectorWheel(false);
        beatsPicker.setValue(mBeats);
        beatsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mBeats = newVal;
                updateService();
            }
        });

    }

    private void updateService() {
        if (mRunning) {
            mMetronomeService.updateMetronome(mTempo, VALID_BEATS[mBeats]);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateTempo(int tempo) {
        mTempo = tempo > MAX_TEMPO ? MAX_TEMPO : tempo;
        mTempo = mTempo < MIN_TEMPO ? MIN_TEMPO : mTempo;

        updateService();

        mTempoSeekBar.setProgress(mTempo - MIN_TEMPO);
        ((TextView) findViewById(R.id.tempo_display)).setText(Integer.toString(mTempo));
    }

}
