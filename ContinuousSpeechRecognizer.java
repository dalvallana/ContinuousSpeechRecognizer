package org.apache.cordova.srplugin;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.apache.cordova.CordovaPlugin;
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

    //@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		Boolean isValidAction = true;

    	this.callbackContext= callbackContext;

		// Action selector
    	if ("startRecognize".equals(action)) {
            // recognize speech
            startSpeechRecognitionActivity(args);     
        } else if ("getSupportedLanguages".equals(action)) {
        	getSupportedLanguages();
        } else {
            // Invalid action
        	this.callbackContext.error("Unknown action: " + action);
        	isValidAction = false;
        }
    	
        return isValidAction;

    }

    // Get the list of supported languages
    private void getSupportedLanguages() {
    	if (languageDetailsChecker == null){
    		languageDetailsChecker = new LanguageDetailsChecker(callbackContext);
    	}
    	// Create and launch get languages intent
    	Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
    	cordova.getActivity().sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
		
	}

	/**
     * Fire an intent to start the speech recognition activity.
     *
     * @param args Argument array with the following string args: [req code][number of matches][prompt string]
     */
    private void startSpeechRecognitionActivity(JSONArray args) {
        int maxMatches = 0;
        String prompt = "";
        String language = Locale.getDefault().toString();

        try {
            if (args.length() > 0) {
            	// Maximum number of matches, 0 means the recognizer decides
                String temp = args.getString(0);
                maxMatches = Integer.parseInt(temp);
            }
            if (args.length() > 1) {
            	// Optional text prompt
                prompt = args.getString(1);
            }
            if (args.length() > 2) {
            	// Optional language specified
            	language = args.getString(2);
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, String.format("startSpeechRecognitionActivity exception: %s", e.toString()));
        }

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new Listener());
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        if (maxMatches > 0) {
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxMatches);
        }
        if (!prompt.equals("")) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        }
        sr.startListening(intent);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppStatus.activityResumed();
        sr.startListening(intent);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppStatus.activityPaused();
        sr.stopListening();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
            }
        }, 300);
    }

    @Override
    public void onDestroy() {
        if(sr != null) {
            sr.cancel();
            sr.destroy();
            sr = null;
        }
        super.onDestroy();
    }

    private void returnSpeechResults(ArrayList<String> matches) {
        JSONArray jsonMatches = new JSONArray(matches);
        this.callbackContext.success(jsonMatches);
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
                sr.startListening(intent);
            }
        }
        public void onResults(Bundle results) {
            ArrayList<String> matches = new ArrayList<String>();
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++) {
                matches.add(data.get(i));
            }            
            if(AppStatus.isActivityVisible()) {
                sr.startListening(intent);
            }            
            returnSpeechResults(matches);
        }
        public void onPartialResults(Bundle partialResults) {
        }
        public void onEvent(int eventType, Bundle params) {
        }
    }

    static class AppStatus {
        private static boolean activityVisible;

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
