package cat.tron.apuntador.activitat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import cat.tron.apuntador.R
import cat.tron.apuntador.databinding.FragmentAssaigBinding
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

object GestorDeVeu {

   object objTTS {
      private var tts: TextToSpeech? = null
      fun set(t: TextToSpeech?) { tts = t }
      fun get(): TextToSpeech? = tts
      fun inici() { tts?.language = Locale("ca_ES") }
   }

   object objVeus {
      private var idioma = "ca"
      private val iVeus: Map<String, Map<String, Voice>> = mapOf(
         "ca" to mapOf(
            "dona" to Voice("ca-es-x-caf-local", Locale("ca_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null)
         ),
         "es" to mapOf(
            "des0" to Voice("es-ES-language", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "des1" to Voice("es-es-x-eea-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "des2" to Voice("es-es-x-eec-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "hes3" to Voice("es-es-x-eed-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "hes4" to Voice("es-es-x-eef-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null)
         ),
         "us" to mapOf(
            "dus0" to Voice("es-US-language", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "dus1" to Voice("es-us-x-sfb-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "hus2" to Voice("es-us-x-esd-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),
            "hus3" to Voice("es-us-x-esf-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null)
         )
      )
      fun setIdioma(i: String) {idioma = i}
      fun getVeu(elem: String, llengua: String?): Voice {
         val veus = iVeus[llengua ?: idioma]!!
         var ret = veus.values.toTypedArray()[0]
         for (v in veus) {
            if (v.key == elem) ret = v.value
         }
         return ret
      }
      fun getList(llengua: String?): Array<String> = iVeus[llengua ?: idioma]!!.keys.toTypedArray()
      fun getVeuNarrador(): Map<String, Any> = mapOf("veu" to getVeu("", idioma), "registre" to 1.0, "velocitat" to 1.0)
   }

   suspend fun textToAudio(text: String, actor: String, ends: String, esNarracio: Boolean = false, esObraSencera: Boolean = false, ac: Activitat): String {
      ac.mostraSentencia(text, ends, esNarracio)
      if (esObraSencera or (ends != ":" && !esNarracio)) {
         var veu: Voice
         var registre: Float
         var velocitat: Float
         val tts = objTTS.get()
         if (actor == "narrador") {
            val veuN = objVeus.getVeuNarrador()
            veu = veuN["veu"] as Voice
            registre = veuN["registre"] as Float
            velocitat = veuN["velocitat"] as Float
         }else {
            val veuActor = Utilitats.objCompanyia.getDadesActors()[actor]
            veu = objVeus.getVeu(veuActor!!["veu"].toString(), veuActor["idioma"].toString())
            registre = veuActor["registre"] as Float
            velocitat = veuActor["velocitat"] as Float
         }
         tts?.setPitch(registre.toString().toFloat())       // 1.0 = normal, >1 més agut, <1 més greu
         tts?.setSpeechRate(velocitat.toString().toFloat()) // 1.0 = normal, >1 més ràpid, <1 més lent
         tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
         tts?.voice = veu
         while (tts?.isSpeaking==true) { true }
      }
      return text
   }

   /*
   Aquesta funció activa el micròfon, recull l'audio amb detecció de veu i el transcriu a text
    */
   fun iniciaReconeixement(context: Context,
                            tempsMaxim: Long,
                            onPreparat: () -> Unit = {},
                            onParlant: () -> Unit = {},
                            onFiDeParla: () -> Unit = {},
                            onResultat: (String) -> Unit,
                            onError: (String) -> Unit) {

      val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
         putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
         putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ca-ES")
         putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // opcional, per tenir text parcial
      }

      val cR = context.resources
      val handler = Handler(Looper.getMainLooper())
      val cancelRunnable = Runnable { recognizer.stopListening() }

      recognizer.setRecognitionListener(object : RecognitionListener {
         override fun onReadyForSpeech(params: Bundle?) {
            onPreparat() // L'usuari pot començar a parlar
            handler.postDelayed(cancelRunnable, tempsMaxim) // inicia el compte enrere
         }
         override fun onResults(results: Bundle?) {
            handler.removeCallbacks(cancelRunnable)
            val paraules = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = paraules?.get(0) ?: ""
            onResultat(text)
         }
         override fun onError(error: Int) {
            handler.removeCallbacks(cancelRunnable)
            val missatge = when (error) {
               SpeechRecognizer.ERROR_AUDIO -> cR.getString(R.string.error_audio)
               SpeechRecognizer.ERROR_NO_MATCH -> cR.getString(R.string.error_no_escolto_res)
               else -> String.format(cR.getString(R.string.error_desconegut), error)
            }
            onError(missatge)
         }
         override fun onBeginningOfSpeech() {
            onParlant()
         }
         override fun onEndOfSpeech() {
            handler.removeCallbacks(cancelRunnable)
            onFiDeParla()
         }
         override fun onRmsChanged(rmsdB: Float) {}
         override fun onBufferReceived(buffer: ByteArray?) {}
         override fun onPartialResults(partialResults: Bundle?) {}
         override fun onEvent(eventType: Int, params: Bundle?) {}
      })
      recognizer.startListening(intent)
   }

   suspend fun preparaReconeixementDeVeu(context: Context, text: String, frgAssaig: FragmentAssaigBinding): String = suspendCancellableCoroutine {
      cont ->
      iniciaReconeixement(
         context,
         calculaTemps(text),
         onPreparat = {frgAssaig.narracio.text = context.resources.getString(R.string.escoltant)},
         onParlant = {frgAssaig.error.text = ""},
         onFiDeParla = {frgAssaig.narracio.text = ""},
         onResultat = { cont.resume(it) { cause, _, _ -> } },
         onError = { cont.resume("") { cause, _, _ -> } }
      )
   }

   private fun calculaTemps(text: String): Long {
      return  (text.length * 100).toLong()
   }

   fun canta(veuSeleccionada: String, registre: Float, velocitat: Float, llengua: String) {
      val tts = objTTS.get()
      val text = mapOf(
         "ca" to "Aquest és un text de prova que mostra el model de veu que es genera segons els paràmetres seleccionats.",
         "en" to "This is a test text showing the voice model that is generated based on the selected parameters.",
         "es" to "Este es un texto de prueba que muestra el modelo de voz que se genera según los parámetros seleccionados."
      )
      tts?.setPitch(registre)
      tts?.setSpeechRate(velocitat)
      tts?.speak(text[llengua], TextToSpeech.QUEUE_FLUSH, null, null)
      tts?.voice = objVeus.getVeu(veuSeleccionada, llengua)
      while (tts?.isSpeaking==true) { true }
   }

}
