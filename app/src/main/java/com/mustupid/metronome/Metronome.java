package com.mustupid.metronome;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Metronome {

    private final short[] mTickData;
    private final short[] mBellData;
    private boolean mPlaying = false;
    private int mTempo;
    private int mBeats;
    private int mCurrentBeat;
    private final ExecutorService mExecutor;

    Metronome(Context context) {
        int[] array = context.getResources().getIntArray(R.array.tick_pcm);
        mTickData = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            mTickData[i] = (short) array[i];
        }
        array = context.getResources().getIntArray(R.array.bell_pcm);
        mBellData = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            mBellData[i] = (short) array[i];
        }
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Updates the metronome with the given tempo and beats pattern.
     *
     * @param tempo Beats per minute that the metronome will click
     * @param beats Number of consecutive beats it will click for in one cycle
     */
    void update(int tempo, int beats) {
        mTempo = tempo;
        mBeats = beats;
        mCurrentBeat = 0;
    }

    /**
     * Play the metronome at the given tempo and beats.
     *
     * @param tempo Tempo in beats per minute of the metronome
     * @param beats Number of consecutive beats it will click for in one cycle
     */
    void play(int tempo, int beats) {
        if (mPlaying)
            return;
        update(tempo, beats);
        mPlaying = true;
        Clicker clicker = new Clicker();
        mExecutor.execute(clicker);
    }

    void stop() {
        mPlaying = false;
    }

    /**
     * Runnable class that keeps looping through the cycle clicking as specified by the pattern array.
     */
    class Clicker implements Runnable {

        private static final int SAMPLE_RATE = 48000;
        private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
        private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private final int BUFFER_SIZE;
        private final short[] ZEROS = new short[SAMPLE_RATE * 60 / MetronomeActivity.MIN_TEMPO];
        private final AudioTrack mTrack;

        Clicker() {
            BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
            mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG,
                    ENCODING, BUFFER_SIZE, AudioTrack.MODE_STREAM);
        }

        /**
         * Write the next beat in the pattern, a tick or bell to the AudioTrack and updates data to
         * keep track of where we are in the pattern. Assumes that mTickData.length == mBellData.length.
         */
        private void writeNextBeatOfPattern() {
            if (mBeats == 0 || mCurrentBeat > 0) {
                mTrack.write(mTickData, 0, mTickData.length);
            } else {
                mTrack.write(mBellData, 0, mBellData.length);
            }
            if (mBeats > 0) {
                mCurrentBeat++;
                mCurrentBeat %= mBeats;
            }
        }

        /**
         * Start the clicking of the metronome by writing the tick or bell data or zeros in between.
         */
        public void run() {
            mTrack.play();
            while (mPlaying) {
                writeNextBeatOfPattern();
                int frames_left_to_wait = 60 * SAMPLE_RATE / mTempo - mTickData.length;
                mTrack.write(ZEROS, 0, frames_left_to_wait);
            }
            mTrack.stop();
            mTrack.release();
        }
    }

}
