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
         "ca" to arrayOf(
            Voice("ca-es-x-caf-local", Locale("ca_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null)
         ),
         "es" to arrayOf(
            Voice("es-ES-language", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),   /*dona greu 0*/
            Voice("es-es-x-eea-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*dona greu 1*/
            Voice("es-es-x-eec-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*dona greu 2*/
            Voice("es-es-x-eed-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*home greu 1*/
            Voice("es-es-x-eef-local", Locale("es_ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*home greu 2*/
            Voice("es-US-language", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),   /*dona US greu 0*/
            Voice("es-us-x-sfb-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*dona US greu 1*/
            Voice("es-us-x-esd-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null),/*home US greu 0*/
            Voice("es-us-x-esf-local", Locale("es_US"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, null) /*home US greu 1*/
         )
      )
      private val aVeus: Map<String, Map<String, Any>> = mapOf(
         "Vukei"  to mapOf("veu" to veu, "registre" to 0.3, "velocitat" to 1.0),
         "Brasde" to mapOf("veu" to veu, "registre" to 0.6, "velocitat" to 1.1),
         "Elkide" to mapOf("veu" to veu, "registre" to 0.8, "velocitat" to 1.1),
         "Hetia"  to mapOf("veu" to veu, "registre" to 1.0, "velocitat" to 1.2),
         "Narde" to mapOf("veu" to veu, "registre" to 1.2, "velocitat" to 1.2),
         "Koeni"  to mapOf("veu" to veu, "registre" to 1.4, "velocitat" to 1.3),
         "Moani"  to mapOf("veu" to veu, "registre" to 1.8, "velocitat" to 1.3),
         "Sukele" to mapOf("veu" to veu, "registre" to 2.4, "velocitat" to 1.4)
      )
      fun get(v: String?): Map<String, Any> = aVeus[v ?: 1]!!
      fun getVeu(idioma: String, elem:Int): Voice = iVeus[idioma]!![elem]
      fun getList(): Array<String> { return aVeus.keys.toTypedArray() }
      fun getNarrador(): String = "Narde"
   }

   suspend fun textToAudio(text: String, veuPersonatge: String, ends: String, esNarracio: Boolean = false, esObraSencera: Boolean = false, ac: Activitat): String {
      ac.mostraSentencia(text, ends, esNarracio)
      if (esObraSencera or (ends != ":" && !esNarracio)) {
         val tts = objTTS.get()
         val veuParams = objVeus.get(veuPersonatge)
         val veu: Voice = veuParams["veu"] as Voice
         val registre = veuParams["registre"] ?: 1.0
         val velocitat = veuParams["velocitat"] ?: 1.0

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
      val regexVeu = """.*?_([0-9]+)""".toRegex()
      val iVeu = regexVeu.find(veuSeleccionada)!!.groupValues[1].toInt()
      val veu = objVeus.getVeu(llengua, iVeu)

      tts?.setPitch(registre)
      tts?.setSpeechRate(velocitat)
      tts?.speak(text[llengua], TextToSpeech.QUEUE_FLUSH, null, null)
      tts?.voice = veu
      while (tts?.isSpeaking==true) { true }
   }

}
