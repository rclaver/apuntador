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
      private val veu = Voice("ca-es-x-caf-local", Locale("ca_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null)
      private val iVeus: Map<String, Array<Voice>> = mapOf(
         "ca" to arrayOf(Voice("ca-es-x-caf-local", Locale("ca_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null))
      )
      private val aVeus: Map<String, Map<String, Any>> = mapOf(
         "Vukei"  to mapOf("veu" to veu, "velocitat" to 1.0, "registre" to 0.3),
         "Brasde" to mapOf("veu" to veu, "velocitat" to 1.1, "registre" to 0.6),
         "Elkide" to mapOf("veu" to veu, "velocitat" to 1.1, "registre" to 0.8),
         "Hetia"  to mapOf("veu" to veu, "velocitat" to 1.2, "registre" to 1.0),
         "Narde" to mapOf("veu" to veu, "velocitat" to 1.2, "registre" to 1.2),
         "Koeni"  to mapOf("veu" to veu, "velocitat" to 1.3, "registre" to 1.4),
         "Moani"  to mapOf("veu" to veu, "velocitat" to 1.3, "registre" to 1.8),
         "Sukele" to mapOf("veu" to veu, "velocitat" to 1.4, "registre" to 2.4)
      )
      fun get(v: String?): Map<String, Any> = aVeus[v ?: 1]!!
      fun getList(): Array<String> { return aVeus.keys.toTypedArray() }
      fun getNarrador(): String = "Narde"
   }

   suspend fun textToAudio(text: String, veuPersonatge: String, ends: String, esNarracio: Boolean = false, esObraSencera: Boolean = false, ac: Activitat): String {
      ac.mostraSentencia(text, ends, esNarracio)
      if (esObraSencera or (ends != ":" && !esNarracio)) {
         val tts = objTTS.get()
         val veuParams = objVeus.get(veuPersonatge)
         val veu: Voice = veuParams["veu"] as Voice
         val velocitat = veuParams["velocitat"] ?: 1.0
         val registre = veuParams["registre"] ?: 1.0

         tts?.setSpeechRate(velocitat.toString().toFloat()) // 1.0 = normal, >1 més ràpid, <1 més lent
         tts?.setPitch(registre.toString().toFloat())       // 1.0 = normal, >1 més agut, <1 més greu
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
      /*val paraules = text.trim().split("\\s+".toRegex()).size
      val segonsPerParaula = 1.5
      return (paraules * segonsPerParaula * 1000).toLong()*/
      return  (text.length * 100).toLong()
   }

}
