package com.latin.continuoussr;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.os.Bundle;

/**
 * Style and such borrowed from the TTS and PhoneListener plugins
 */
public class ContinuousSpeechRecognizer extends CordovaPlugin {
    private static final String LOG_TAG = ContinuousSpeechRecognizer.class.getSimpleName();
    private static int REQUEST_CODE = 1001;

    private CallbackContext callbackContext;
    private LanguageDetailsChecker languageDetailsChecker;

    private Camera cam;
    private Parameters p;
    private boolean isFlashOn;

    private SpeechRecognizer sr;
    private Intent intent;
    private AudioManager mAudioManager;
    private int mStreamVolume = 0;

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Boolean isValidAction = true;
        this.callbackContext = callbackContext;
        if ("startRecognize".equals(action)) {
            startSpeechRecognitionActivity(args);     
        } else if ("getSupportedLanguages".equals(action)) {
            getSupportedLanguages();
        } else {
            this.callbackContext.error("Unknown action: " + action);
            isValidAction = false;
        }       
        return isValidAction;
    }

    private void getSupportedLanguages() {
        if (languageDetailsChecker == null){
            languageDetailsChecker = new LanguageDetailsChecker(callbackContext);
        }
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        cordova.getActivity().sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
        
    }

    /**
     * Fire an intent to start the speech recognition activity.
     *
     * @param args Argument array with the following string args: [req code][number of matches]
     */
    private void startSpeechRecognitionActivity(JSONArray args) {
        int maxMatches = 0;
        String language = Locale.getDefault().toString();

        try {
            if (args.length() > 0) {
                String temp = args.getString(0);
                maxMatches = Integer.parseInt(temp);
            }
            if (args.length() > 1) {
                language = args.getString(1);
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, String.format("startSpeechRecognitionActivity exception: %s", e.toString()));
        }

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        if (maxMatches > 0) {
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxMatches);
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                sr = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity().getBaseContext());
                sr.setRecognitionListener(new Listener());                    
                sr.startListening(intent);
            }
        });
        
        mAudioManager = (AudioManager) cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        muteStreamVolume();
    }

    @Override
    public void onResume(boolean b) {
        super.onResume(b);
        AppStatus.activityResumed();
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                sr.startListening(intent);
            }
        });
        muteStreamVolume();
    }

    @Override
    public void onPause(boolean b) {
        super.onPause(b);
        AppStatus.activityPaused();
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                sr.stopListening();
            }
        });
        setStreamVolumeBack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(sr != null) {
                    sr.cancel();
                    sr.destroy();
                    sr = null;
                }
            }
        });
        setStreamVolumeBack();
    }

    private void muteStreamVolume() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    private void setStreamVolumeBack() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
            }
        }, 300);
    }

    private void returnSpeechResults(ArrayList<String> matches) {
        JSONArray jsonMatches = new JSONArray(matches);
        this.callbackContext.success(jsonMatches);
    }

    private void returnProgressResults(ArrayList<String> matches) {
        JSONArray jsonMatches = new JSONArray(matches);

        PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonMatches);
        progressResult.setKeepCallback(true);
        callbackContext.sendPluginResult(progressResult);
    }

    class Listener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params) {
        }
        public void onBeginningOfSpeech() {
        }
        public void onRmsChanged(float rmsdB) {
        }
        public void onBufferReceived(byte[] buffer) {
        }
        public void onEndOfSpeech() {
        }
        public void onError(int error) {
            if(AppStatus.isActivityVisible()) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        sr.startListening(intent);
                    }
                });
            }
        }
        public void onResults(Bundle results) {
            ArrayList<String> matches = new ArrayList<String>();
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++) {
                matches.add((String) data.get(i));
            }            
            if(AppStatus.isActivityVisible()) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        sr.startListening(intent);
                    }
                });
            }            
            returnProgressResults(matches);
        }
        public void onPartialResults(Bundle partialResults) {
        }
        public void onEvent(int eventType, Bundle params) {
        }
    }

    static class AppStatus {
        private static boolean activityVisible = true;

        public static boolean isActivityVisible() {
            return activityVisible;
        }

        public static void activityResumed() {
            activityVisible = true;
        }

        public static void activityPaused() {
            activityVisible = false;
        }
    }
    
}