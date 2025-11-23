package cat.tron.apuntador

//import cat.tron.apuntador.activitat.SharedViewModel
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import cat.tron.apuntador.activitat.GestorDeVeu
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale

const val REQUEST_CODE_CHECK_TTS = 1001
const val REQUEST_CODE_INSTALL_TTS = 1002

open class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
   private lateinit var binding: ActivityMainBinding
   //private lateinit var comparticio: SharedViewModel
   private val idioma: Locale = Locale("ca", "ES")
   private var tts: TextToSpeech? = null
   private val directoriEscenes = "apuntador_lollipop"
   //private val arxiuPreferencies = "preferencies"
   private val engine = "com.google.android.tts" //motor de Google TTS

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivityMainBinding.inflate(layoutInflater)
      setContentView(binding.root)

      //comparticio = ViewModelProvider(this).get(SharedViewModel::class.java)

      inicialitzarTTS()
      //actualitzaConfiguracio(applicationContext)
      /*
      Utilitats.demanaPermissos(applicationContext, this)
      val prefs = getSharedPreferences(arxiuPreferencies, MODE_PRIVATE)
      val uriDesada = prefs.getString(directoriEscenes, null)
      if (uriDesada != null) {
         Utilitats.DirectoriDocuments.setTreeUri(DocumentFile.fromTreeUri(this, uriDesada.toUri())!!)
      } else {
         Utilitats.demanaAccessDescarregues(this)
      }
      */
      val dirDocs: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      Utilitats.DirectoriDocuments.set(dirDocs)
      GestorDeVeu.objTTS.set(tts)
      GestorDeVeu.inicialitzarTTS(this) { status ->
         if (status == TextToSpeech.SUCCESS) {
            GestorDeVeu.objTTS.inici()
         } else {
            Toast.makeText(this, "Error inicialitzant TTS", Toast.LENGTH_LONG).show()
         }
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      when (requestCode) {
         REQUEST_CODE_CHECK_TTS -> {
            when (resultCode) {
               TextToSpeech.Engine.CHECK_VOICE_DATA_PASS -> {
                  // Datos de voz disponibles, inicializar con motor de Google
                  tts = TextToSpeech(this, this, engine)
                  //comparticio.enviaNota("Datos de voz disponibles, inicializar con motor de Google\n$tts")
               }
               TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL,
               TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA -> {
                  // Datos de voz no disponibles, instalar
                  //comparticio.enviaNota("Datos de voz no disponibles, instalar")
                  instalarDadesTTS()
               }
               else -> {
                  // Usar TTS por defecto como fallback
                  tts = TextToSpeech(this, this)
                  //comparticio.enviaNota("Usar TTS por defecto\n$tts")
               }
            }
         }
         REQUEST_CODE_INSTALL_TTS -> {
            // Después de intentar instalar, verificar nuevamente
            //comparticio.enviaNota("Después de intentar instalar, inicialitzarTTS nuevamente")
            inicialitzarTTS()
         }
         Utilitats.REQUEST_CODE_OPEN_DIRECTORY -> {
            if (resultCode == RESULT_OK) {
               val treeUri = data?.data ?: return
               // Agafem el permís permanent
               try {
                  contentResolver.takePersistableUriPermission(
                     treeUri,
                     Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                  )
               } catch (e: SecurityException) {
                  // En Android 5.1, a veces 'takePersistableUriPermission' falla
                  //Toast.makeText(this, "MainActivity: No se pudieron obtener permisos persistentes: ${e.message}", Toast.LENGTH_LONG).show()
               }
               // Desa l'URI com a string
               /*
               val prefs = getSharedPreferences(arxiuPreferencies, MODE_PRIVATE)
               prefs.edit {
                  putString(directoriEscenes, treeUri.toString())
                  apply()
               }
               // Desa el directori perquè sigui accessible des d'altres llocs
               Utilitats.DirectoriDocuments.setTreeUri(DocumentFile.fromTreeUri(this, treeUri)!!)
               //Toast.makeText(this, "treeUri:$treeUri", Toast.LENGTH_LONG).show()
               */
            }
         }
      }
   }

   // TextToSpeech.OnInitListener
   override fun onInit(status: Int) {
      if (status == TextToSpeech.SUCCESS) {
         tts?.setEngineByPackageName(engine)
         val result = tts?.setLanguage(idioma)
         if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            //comparticio.enviaNota(R.string.idioma_no_soportat.toString())
            //Toast.makeText(this, R.string.idioma_no_soportat, Toast.LENGTH_LONG).show()
            // L'usuari hauria d'instal·lar l'enginy Google TTS
            instalarDadesTTS()
            /*val installIntent = Intent().apply {
               action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            }
            startActivity(installIntent)*/
         }
      } else {
         //comparticio.enviaNota(R.string.error_inici_TTS.toString())
         //Toast.makeText(this, R.string.error_inici_TTS, Toast.LENGTH_LONG).show()
      }
   }

   private fun inicialitzarTTS() {
      try {
         // Verificar si el motor de Google TTS está disponible
         val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
         startActivityForResult(checkIntent, REQUEST_CODE_CHECK_TTS)
         //comparticio.enviaNota("motor de Google TTS disponible")
      } catch (e: Exception) {
         // Si no está disponible, usar TTS por defecto
         tts = TextToSpeech(this, this)
         //comparticio.enviaNota("motor de Google TTS no está disponible, usar TTS por defecto\n$tts")
      }
   }

   private fun instalarDadesTTS() {
      try {
         val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
         installIntent.setPackage("com.google.android.tts") // Especificar paquete
         startActivityForResult(installIntent, REQUEST_CODE_INSTALL_TTS)
      } catch (e: Exception) {
         // Si falla, redirigir a Google Play
         //comparticio.enviaNota("Ha fallat la instal·lació de dades TTS. visitaGooglePlay()")
         visitaGooglePlay()
      }
   }

   private fun visitaGooglePlay() {
      try {
         throw Exception("Not available in Play Store for Android API 22")
         /*
         val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=com.google.android.tts".toUri()
            setPackage("com.android.vending")
         }
         startActivity(playStoreIntent)
         */
      } catch (e: Exception) {
         // Si Google Play no está disponible, usar navegador web
         val webIntent = Intent(Intent.ACTION_VIEW).apply {
            //data = "https://play.google.com/store/apps/details?id=com.google.android.tts".toUri() //No disponible per a Android API 22
            data= "https://www.apkmirror.com/apk/google-inc/google-text-to-speech-engine/google-text-to-speech-engine-24-9-361717975-release/google-text-to-speech-24-9-361717975-2-android-apk-download/download/?key=3b1ed97369ae6e49e945346558a0dba47d01560e".toUri()
         }
         startActivity(webIntent)
      }
   }

   /*
   fun actualitzaConfiguracio(ctx: Context) {
      if (idioma != Locale("") && idioma != Locale("ca")) {
         Locale.setDefault(idioma)
         val configuration = resources.configuration
         configuration.setLocale(idioma)
         ctx.resources.updateConfiguration(configuration, ctx.resources.displayMetrics)
      }
   }
   */
   override fun onDestroy() {
      tts?.stop()
      tts?.shutdown()
      super.onDestroy()
   }

}
