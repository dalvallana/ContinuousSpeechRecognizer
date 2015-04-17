Android ContinuousSpeechRecognizer plugin for Cordova/Phonegap
===================================
This plugin provides access to the SpeechRecognizer API for Android devices. It allows for continuous listening, so everytime the API detects the user said something, it will send to your javascript what was detected. This plugin also removes the default API's user interface and mutes the sound that it makes.

This plugin is a modification (or extension) of the [SpeechRecognizer](https://github.com/poiuytrez/SpeechRecognizer) plugin.

With this plugin I made [this app](https://play.google.com/store/apps/details?id=com.ionicframework.lumos784229).

Requirements
-------------
Android 2.2 (API level 8) is required  
Compatible with Cordova 3.0.

You must provide the proper permissions in your app's `AndroidManifest.xml` file like this:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

Support
---------------------
For free community support, please use the issue tracker.

Installation for cordova>=3.0.0
-----------------------------------------------------
```
cordova platform add android
cordova plugin add https://github.com/daao87/ContinuousSpeechRecognizer.git
```
  

Usage
-------

#### Start recognition
Show the recognition dialog and get the recognized sentences

    window.continuoussr.startRecognize(success, error, maxMatches, language);
Parameters:
* success : The success callback. It provides a json array with all possible matches. Example: "[hello world,low world,hello walls]".
* error : The error callback.
* maxMaches : Maximum of returned possibles sentences matches.
* language : Language used by the speech recognition engine. Example: "en-US".

#### Supported languages
Get the list of supported languages codes

    window.continuoussr.getSupportedLanguages(success, error);
Parameters:
* success : The success callback. It provides a json array of all the recognized language codes. Example: "[en-US,fr-FR,de-DE]".
* error : The error callback.

Example
----------------
```html
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Continuous Speech Recognizer</title>
        <script src="cordova.js"></script>
    </head>
    <body>
        <script type="text/javascript">

            function onDeviceReady(){
                console.log("Device is ready");
            }

            // Recognize!
            function recognizeSpeech() {
                var maxMatches = 5;
                var language = "en-US"; // Optional
                window.continuoussr.startRecognize(function(result){
                    alert(result);
                }, function(errorMessage){
                    alert("Error message: " + errorMessage);
                }, maxMatches, language);
            }

            // Show the list of the supported languages
            function getSupportedLanguages() {
                window.continuoussr.getSupportedLanguages(function(languages){
                    // display the json array
                    alert(languages);
                }, function(error){
                    alert("Could not retrieve the supported languages : " + error);
                });
            }

            document.addEventListener("deviceready", onDeviceReady, true);
        </script>

        <button onclick="recognizeSpeech();">Start recognition</button>
        <button onclick="getSupportedLanguages();">Get Supported Languages</button>
    </body>
</html>
```
