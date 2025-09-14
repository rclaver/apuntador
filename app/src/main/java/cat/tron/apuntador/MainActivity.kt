package cat.tron.apuntador

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import cat.tron.apuntador.activitat.GestorDeVeu
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale

open class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

   private lateinit var binding: ActivityMainBinding
   private val idioma: Locale = Locale("ca", "ES")
   private var tts: TextToSpeech? = null
   private val carpetaArxius = "app_apuntador_lollipop"
   private val preferencies = "prefs"
   private val engine = "com.google.android.tts" //motor de Google TTS

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivityMainBinding.inflate(layoutInflater)
      setContentView(binding.root)

      //actualitzaConfiguracio(applicationContext)
      Utilitats.demanaPermissos(applicationContext, this)
      //Utilitats.DirectoriDescarregues.setContext(applicationContext)
      val prefs = getSharedPreferences(preferencies, MODE_PRIVATE)
      val uriDesada = prefs.getString(carpetaArxius, null)
      if (uriDesada != null) {
         val uri = uriDesada.toUri()
         val docsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
         val dirUri = docsDir.toUri()
         Utilitats.DirectoriDescarregues.set(DocumentFile.fromTreeUri(this, uri))
      } else {
         Utilitats.demanaAccessDescarregues(this)
      }
      GestorDeVeu.objTTS.set(TextToSpeech(this, this, engine))
      tts = GestorDeVeu.objTTS.get()
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      if (requestCode == Utilitats.REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
         val treeUri = data?.data ?: return
         // Agafem el permís permanent
         try {
            contentResolver.takePersistableUriPermission(
               treeUri,
               Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
         } catch (e: SecurityException) {
            // En Android 5.1, a veces falla takePersistableUriPermission
            print("MainActivity: No se pudieron obtener permisos persistentes: ${e.message}")
         }
         // Desa l'URI com a string
         val prefs = getSharedPreferences(preferencies, MODE_PRIVATE)
         prefs.edit { putString(carpetaArxius, treeUri.toString()) }

         val downloadsDir: DocumentFile? = DocumentFile.fromTreeUri(this, treeUri)
         val docsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
         // Desa el directori perquè sigui accessible des d'altres llocs
         Utilitats.DirectoriDescarregues.set(DocumentFile.fromTreeUri(this, treeUri))
      }
   }

   // TextToSpeech.OnInitListener
   override fun onInit(status: Int) {
      if (status == TextToSpeech.SUCCESS) {
         tts?.setEngineByPackageName(engine)
         val result = tts?.setLanguage(idioma)
         if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            print(R.string.idioma_no_soportat)
            // L'usuari haurà d'instal·lar l'enginy Google TTS
            val installIntent = Intent().apply {
               action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            }
            startActivity(installIntent)         }
      } else {
         print(R.string.error_inici_TTS)
      }
   }

   /*
   fun actualitzaConfiguracio(ctx: Context) {
      if (idioma != Locale("") &&  idioma != Locale("ca")) {
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
