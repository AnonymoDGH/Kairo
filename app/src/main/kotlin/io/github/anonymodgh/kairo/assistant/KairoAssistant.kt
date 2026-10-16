package io.github.anonymodgh.kairo.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import io.github.anonymodgh.kairo.MainActivity

/**
 * Permite elegir Kairo como "Asistente digital" del sistema (Ajustes → Apps → Apps predeterminadas).
 * El gesto de asistente (mantener encendido / deslizar desde la esquina) abre Kairo encima de la
 * app actual, con esa app desenfocada detrás.
 */
class KairoVoiceInteractionService : VoiceInteractionService()

class KairoSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = KairoSession(this)
}

private class KairoSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startAssistantActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        hide()
    }
}

/**
 * Android exige un servicio de reconocimiento junto al asistente. Kairo no ofrece el suyo a otras
 * apps: su STT es local y propio (KAIRO_PLAN §6).
 */
class KairoRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback?) = Unit
    override fun onStopListening(listener: Callback?) = Unit
}
