package com.example.assistantapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.util.Locale


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainPage(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isSettingsVisible by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    // Add speech recognition for voice commands
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }
    
    // Voice activation state
    var isListening by remember { mutableStateOf(false) }
    
    // Function to start voice recognition - moved here before first use
    fun startVoiceRecognition() {
        if (!isListening) {
            try {
                speechRecognizer.startListening(speechIntent)
                isListening = true
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
                
                val voices = tts?.voices
                voices?.find { it.name.contains("female", ignoreCase = true) && 
                               it.name.contains("en-us", ignoreCase = true) }?.let {
                    tts?.setVoice(it)
                }
                
                // Announce that voice commands are available
                tts?.speak(
                    "Welcome. You can activate blind mode by saying 'open blind mode', activate reading mode by saying 'reading mode', or identify money by saying 'currency detection'.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }
        
        // Setup speech recognition listener
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase().trim()

                    // Require more specific phrases to avoid accidental triggers
                    val blindModePhrases = listOf(
                        "blind mode",
                        "open blind mode",
                        "start blind mode",
                        "activate blind mode",
                        "blind navigation",
                        "open blind navigation"
                    )
                    
                    // Check for blind mode activation commands
                    if (blindModePhrases.any { phrase -> spokenText.contains(phrase) }) {
                        
                        // Provide feedback and navigate
                        tts?.speak(
                            "Activating blind mode",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                        
                        // Short delay to allow TTS to speak before navigation
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navController.navigate("blindMode")
                        }, 1000)
                    }
                    // Check for reading mode activation commands
                    else if (spokenText.contains("reading mode") || 
                             spokenText.contains("read mode") || 
                             spokenText.contains("start reading") ||
                             spokenText.contains("read this for me") ||
                             spokenText.contains("read for me")) {
                        
                        // Provide feedback and navigate
                        tts?.speak(
                            "Activating reading mode. Point camera at text to read.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                        
                        // Short delay to allow TTS to speak before navigation
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navController.navigate("readingMode")
                        }, 1000)
                    }
                    // Check for currency detection mode activation commands
                    else if (spokenText.contains("currency detection") || 
                             spokenText.contains("detect currency") ||
                             spokenText.contains("identify money") ||
                             spokenText.contains("money detection") ||
                             spokenText.contains("cash detection")) {
                        
                        // Provide feedback and navigate
                        tts?.speak(
                            "Activating currency detection mode. Point camera at money bills to identify their value. Automatic detection is enabled.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                        
                        // Short delay to allow TTS to speak before navigation
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navController.navigate("currencyDetection")
                        }, 1000)
                    }
                }
                
                // Restart listening
                isListening = false
                startVoiceRecognition()
            }
            
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Keep microphone active continuously while app is open
                isListening = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startVoiceRecognition()
                }, 300)
            }
            override fun onError(error: Int) {
                // Restart listening after a short delay
                isListening = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startVoiceRecognition()
                }, 1000)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // Ignore partial results for navigation to avoid accidental activations
                // Final decisions are handled in onResults() only.
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        // Start voice recognition
        startVoiceRecognition()

        onDispose {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6D4AFF), // top purple
                        Color(0xFF3E8EFF)  // bottom blue
                    )
                )
            )
    ) {
        // Background blur when instructions panel is visible
        if (isSettingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        }

        // Voice recognition indicator at top-right (same behaviour)
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFF4CAF50) else Color(0xFFB0BEC5))
                    .clickable {
                        if (!isListening) {
                            startVoiceRecognition()
                            tts?.speak(
                                "Voice recognition activated. Say 'open blind mode' for navigation or 'reading mode' to read text",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
            )
        }

        // Center content: logo, title, subtitle, and feature buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            // Logo in white circle
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Saarthi logo inside white circle
                Image(
                    painter = painterResource(id = R.drawable.ic_saarthi_logo),
                    contentDescription = "Saarthi logo",
                    // Increase size so logo appears larger inside the circle
                    modifier = Modifier.size(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SAARTHI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Empowering the visually impaired with AI Powered Smart Assistance",
                fontSize = 14.sp,
                color = Color(0xFFECEFF1),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            val buttonShape = RoundedCornerShape(50)

            // Blind Navigation button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("blindMode")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = buttonShape,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF388E3C), Color(0xFF2E7D32))
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Blind Navigation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Currency Detection button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("currencyDetection")
                    tts?.speak(
                        "Currency detection mode activated. Point camera at money bills to identify their value. Automatic detection is enabled.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = buttonShape,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF388E3C), Color(0xFF2E7D32))
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Currency Detection",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reading Mode button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("readingMode")
                    tts?.speak(
                        "Reading mode activated. Point camera at text to read.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = buttonShape,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF388E3C), Color(0xFF2E7D32))
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Reading Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Assistance button and instructions panel (same functionality as before)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            // Assistance button (temporarily disabled as requested)
//            Button(
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    isSettingsVisible = !isSettingsVisible
//                    if (isSettingsVisible) {
//                        tts?.speak(
//                            "To Master the navigation through this App, follow these instructions below. " +
//                                    "Double Tap on the Navigation screen to stop the Navigation or vice versa. " +
//                                    "Right after, speak any query about anything or the environment around you. " +
//                                    "You also can use your earbuds or airpods by double tapping to stop or resume the navigation. " +
//                                    "You can also visit the YouTube video to see detailed navigation on the App. " +
//                                    "By following these, you can teach an impaired person how to use this app.",
//                            TextToSpeech.QUEUE_FLUSH,
//                            null,
//                            null
//                        )
//                    } else {
//                        tts?.stop()
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth(0.7f)
//                    .height(52.dp),
//                shape = RoundedCornerShape(50),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF2E7D32),
//                    contentColor = Color(0xFFB39DDB)
//                )
//            ) {
//                Text(
//                    text = "Assistance",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = isSettingsVisible,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) { isVisible ->
                Icon(
                    imageVector = if (isVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isVisible) "Close Instructions" else "Open Instructions",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isSettingsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(top = 12.dp)
                ) {
                    val annotatedText = buildAnnotatedString {
                        append(" ⭕ To Master the navigation through this App, follow these instructions below 😎.\n\n")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("1️⃣ Double Tap ")
                        }
                        append("on the Navigation screen to ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Enter")
                        }
                        append(" in the Assistant Mode ⚡. and again double tap to exit the Navigation mode. \n\n")

                        append("2️⃣ While in Navigation Mode long press the screen enter the reading mode ☝ . \n\n")

                        append("You can also visit the ")
                        pushStringAnnotation(
                            tag = "URL",
                            annotation = "https://www.youtube.com/watch?v=GD4iuPCIXTc&t=116s"
                        )
                        withStyle(
                            style = SpanStyle(
                                color = Color.Blue,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("YouTube video ")
                        }
                        pop()
                        append("to see detailed navigation on the App. By following these, you can ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("teach an impaired person ")
                        }
                        append("how to use this app.")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            ClickableText(
                                text = annotatedText,
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations(
                                        tag = "URL",
                                        start = offset,
                                        end = offset
                                    )
                                        .firstOrNull()?.let { annotation ->
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            context.startActivity(intent)
                                        }
                                },
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
