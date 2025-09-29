package com.example.homie.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandManager implements RecognitionListener {
    private static final String TAG = "VoiceCommandManager";

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private VoiceCommandListener listener;
    private boolean isListening = false;

    public interface VoiceCommandListener {
        void onCommandRecognized(String command);

        void onListeningStateChanged(boolean isListening);

        void onError(String error);
    }

    public VoiceCommandManager(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(this);

            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    context.getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        }
    }

    public void setVoiceCommandListener(VoiceCommandListener listener) {
        this.listener = listener;
    }

    public void startListening() {
        if (speechRecognizer != null && !isListening) {
            try {
                speechRecognizer.startListening(recognizerIntent);
                isListening = true;
                if (listener != null) {
                    listener.onListeningStateChanged(true);
                }
                Log.d(TAG, "Started listening for voice commands");
            } catch (Exception e) {
                Log.e(TAG, "Error starting speech recognition: " + e.getMessage());
                if (listener != null) {
                    listener.onError("Failed to start voice recognition");
                }
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            if (listener != null) {
                listener.onListeningStateChanged(false);
            }
            Log.d(TAG, "Stopped listening for voice commands");
        }
    }

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Ready for speech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Can be used to show voice input level
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // Audio buffer received
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech");
        isListening = false;
        if (listener != null) {
            listener.onListeningStateChanged(false);
        }
    }

    @Override
    public void onError(int error) {
        String errorMessage = getErrorText(error);
        Log.e(TAG, "Speech recognition error: " + errorMessage);
        isListening = false;
        if (listener != null) {
            listener.onListeningStateChanged(false);
            listener.onError(errorMessage);
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String command = matches.get(0).toLowerCase().trim();
            Log.d(TAG, "Voice command recognized: " + command);

            if (listener != null) {
                listener.onCommandRecognized(command);
            }

            processVoiceCommand(command);
        }
        isListening = false;
        if (listener != null) {
            listener.onListeningStateChanged(false);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            Log.d(TAG, "Partial result: " + matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        // Handle recognition events
    }

    private void processVoiceCommand(String command) {
        // Process the recognized voice command
        if (command.contains("turn on") || command.contains("turn off")) {
            handleDeviceToggle(command);
        } else if (command.contains("set temperature") || command.contains("temperature")) {
            handleTemperatureControl(command);
        } else if (command.contains("brightness") || command.contains("dim") || command.contains("bright")) {
            handleBrightnessControl(command);
        } else if (command.contains("volume")) {
            handleVolumeControl(command);
        } else {
            Log.d(TAG, "Unrecognized command: " + command);
        }
    }

    private void handleDeviceToggle(String command) {
        boolean turnOn = command.contains("turn on");
        String deviceName = extractDeviceName(command);

        Log.d(TAG, "Device toggle - Device: " + deviceName + ", Turn on: " + turnOn);
        // Here you would integrate with your smart home device control system
    }

    private void handleTemperatureControl(String command) {
        String temperature = extractNumber(command);
        String deviceName = extractDeviceName(command);

        Log.d(TAG, "Temperature control - Device: " + deviceName + ", Temperature: " + temperature);
        // Here you would integrate with your thermostat control system
    }

    private void handleBrightnessControl(String command) {
        String brightness = extractNumber(command);
        String deviceName = extractDeviceName(command);

        Log.d(TAG, "Brightness control - Device: " + deviceName + ", Brightness: " + brightness);
        // Here you would integrate with your lighting control system
    }

    private void handleVolumeControl(String command) {
        String volume = extractNumber(command);
        String deviceName = extractDeviceName(command);

        Log.d(TAG, "Volume control - Device: " + deviceName + ", Volume: " + volume);
        // Here you would integrate with your audio device control system
    }

    private String extractDeviceName(String command) {
        // Simple device name extraction
        if (command.contains("living room") || command.contains("living")) {
            return "living room lights";
        } else if (command.contains("bedroom")) {
            return "bedroom lights";
        } else if (command.contains("kitchen")) {
            return "kitchen lights";
        } else if (command.contains("thermostat") || command.contains("temperature")) {
            return "thermostat";
        } else if (command.contains("fan")) {
            return "ceiling fan";
        } else if (command.contains("tv") || command.contains("television")) {
            return "tv";
        } else if (command.contains("speaker") || command.contains("music")) {
            return "speaker";
        }
        return "unknown";
    }

    private String extractNumber(String command) {
        // Extract numbers from the command
        String[] words = command.split(" ");
        for (String word : words) {
            if (word.matches("\\d+")) {
                return word;
            }
        }

        // Handle written numbers
        if (command.contains("one"))
            return "1";
        if (command.contains("two"))
            return "2";
        if (command.contains("three"))
            return "3";
        if (command.contains("four"))
            return "4";
        if (command.contains("five"))
            return "5";
        if (command.contains("ten"))
            return "10";
        if (command.contains("twenty"))
            return "20";
        if (command.contains("fifty"))
            return "50";
        if (command.contains("hundred"))
            return "100";

        return "0";
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech input matched";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Recognition error";
        }
    }
}