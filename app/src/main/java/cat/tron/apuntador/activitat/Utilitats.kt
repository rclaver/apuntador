package cat.tron.apuntador.activitat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import cat.tron.apuntador.databinding.FragmentConfiguracioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

object Utilitats {

   const val REQUEST_CODE_OPEN_DIRECTORY = 101
   private const val STORAGE_PERMISSION_CODE = 100
   private const val arxiuParametres = "parametres.json"

   object objEnFagmentSeleccio {
      private var estat: Boolean = false
      fun set(e: Boolean) { estat = e }
      fun get(): Boolean = estat
   }

   object DirectoriDescarregues {
      private var dir: DocumentFile? = null
      fun set(d: DocumentFile?) { dir = d }
      fun get(): DocumentFile? = dir
   }

   object objCompanyia {
      private var llistaActors = mutableListOf<String>()
      private var dadesActors = mutableMapOf<String, Map<String,Any>>()
      private var titol: String? = null
      private var idioma: String = "ca"

      fun set(json: JSONObject?) {
         json?.let {
            titol = it.optString("titolDeLobra", "")
            idioma = it.optString("idioma", "")

            val jsonLlista = it.optJSONObject("llistatDactors")
            if (jsonLlista != null) {
               val dadesTemp = mutableMapOf<String, Map<String, Any>>()
               llistaActors = mutableListOf<String>()
               jsonLlista.keys().forEach { actor ->
                  llistaActors.add(actor)
                  val parametres = jsonLlista.getJSONObject(actor)
                  val mapa = mutableMapOf<String, Any>()
                  parametres.keys().forEach { k ->
                     mapa[k] = parametres.get(k)
                  }
                  dadesTemp[actor] = mapa
               }
               dadesActors = dadesTemp
            }
         }
      }

      fun get(): JSONObject {
         val json = JSONObject()
         json.put("titolDeLobra", titol)
         json.put("idioma", idioma)
         val jsonActors = JSONObject()
         if (dadesActors.isNotEmpty()) {
            for ((actor, parametres) in dadesActors) {
               val paramsJson = JSONObject()
               for ((k,v) in parametres) {
                  paramsJson.put(k,v)
               }
               jsonActors.put(actor, paramsJson)
            }
            json.put("llistatDactors", jsonActors)
         }else if (llistaActors.isNotEmpty())  {
            json.put("llistatDactors", JSONArray(llistaActors))
         }
         return json
      }

      fun setTitol(t: String) { titol = t }
      fun setIdioma(i: String) { idioma = i }
      fun setActors(a: MutableList<String>) { llistaActors = a }
      fun setDadesActors(d: MutableMap<String, Map<String,Any>>) { dadesActors = d }

      fun getTitol(): String = titol.orEmpty()
      fun getIdioma(): String = idioma
      fun getActors(): MutableList<String>? {
         if (llistaActors.isEmpty()) {
            dadesActors.forEach { llistaActors.add(it.key) }
         }
         return llistaActors
      }
      fun getDadesActors(): MutableMap<String, Map<String,Any>> = dadesActors
      fun getDisponible(): Boolean = dadesActors.isNotEmpty()
   }

   fun obraSencera(nomArxiu: String): DocumentFile? {
      val dir = DirectoriDescarregues.get()
      if (dir?.exists() == true) {
         dir.listFiles().forEach { file ->
            if (file.isFile && file.name == nomArxiu)
               return file
         }
      }
      return null
   }

   fun llistaFragmentsObra(patroBase: String, patroActor: String, omissio: String): List<DocumentFile>  {
      val arxius = llistaDirectoriDescarregues(patroBase.toRegex())
      var ret = arxius.filter { it.name.toString().matches(Regex(patroActor)) }
      if (ret.isEmpty()) {
         ret = arxius.filter { it.name.toString().matches(Regex(omissio)) }
      }
      return ret
   }

   fun llistaDirectoriDescarregues(patro: Regex): List<DocumentFile> {
      var llistaArxiusDescarregues = mutableListOf<DocumentFile>()
      val dir = DirectoriDescarregues.get()
      if (dir?.exists() == true) {
         dir.listFiles().forEach { file ->
            if (file.isFile && patro.containsMatchIn(file.name.toString())) {
               llistaArxiusDescarregues.add(file)
            }
         }
      }
      return llistaArxiusDescarregues
   }

   fun llegeixArxiu(context: Context, document: DocumentFile): String {
      return try {
         context.contentResolver.openInputStream(document.uri)?.use { inputStream ->
            inputStream.bufferedReader().use { reader ->
               reader.readText()
            }
         } ?: "No he pogut obrir el fitxer"
      } catch (e: Exception) {
         "Error llegint el fitxer: ${e.message}"
      }
   }

   fun comparaSequenciesDeText(text1: String, text2: String): Int {
      // normalitza el text original
      val replace = "[.,!¡¿?()]".toRegex()
      val text = text1.replace(replace, " ").replace("\\s+".toRegex(), " ").trim().lowercase()

      val arrText1: List<String> = text.split(" ")
      val arrText2: List<String> = text2.split(" ")
      val encertDesplacament: Int = comparaPerDesplacament(arrText1, arrText2)
      val encertPosicio: Int = comparaPerPosicio(arrText1, arrText2)
      return maxOf(encertDesplacament, encertPosicio)
   }

   private fun comparaPerPosicio(a1: List<String>, a2: List<String>): Int {
      var error = 0
      for ((i, s1) in a1.withIndex()) {
         if (s1 != a2[i]) { error++ }
      }
      return (100 - (error * 100 / a1.size)).toInt()
   }

   private fun comparaPerDesplacament(a1: List<String>, a2: List<String>): Int {
      var arr1: List<String> = a1
      var arr2: List<String> = a2
      var encert = arr1.size
      var error = 0

      for (s1 in arr1) {
         for (s2 in arr2) {
            if (s1 == s2) {
               error = 0
               arr1 = arr1.drop(1)
               break
            }else {
               encert--
               error++
               if (error >= 3) {
                  arr2 = arr2.drop(1)
                  break
               }
            }
         }
      }
      return (encert * 100 / arr1.size).toInt()
   }

   fun demanaPermissos(cntx: Context, aca: AppCompatActivity) {
      try {
         val noPermis = cntx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
               cntx.checkSelfPermission(Manifest.permission.MANAGE_DOCUMENTS) != PackageManager.PERMISSION_GRANTED ||
               cntx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
         if (noPermis) {
            ActivityCompat.requestPermissions(aca,
               arrayOf(
                  Manifest.permission.RECORD_AUDIO,
                  Manifest.permission.MANAGE_DOCUMENTS,
                  Manifest.permission.READ_EXTERNAL_STORAGE
               ),
               STORAGE_PERMISSION_CODE
            )
         }
      }catch(e: UnknownError) {
         println(e)
      }
   }

   // Sol·licitar permisos persistents per accedir als arxius contínuament.
   fun demanaAccessDescarregues(aca: AppCompatActivity) {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
         flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                 Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                 Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      }
      aca.startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
   }

   /*
   Obtenir les dades de l'obra a partir de la llista d'arxius del directori Apuntador
   */
   fun obtenirDadesCompanyia() {
      // Obtenir el titol de l'obra
      var titol = ""
      var arxius: List<DocumentFile> = listOf<DocumentFile>()
      val patroTitol = Regex("""[a-z]+?-?(?=[a-z]*?-?[0-9]*?)\.txt""")
      val arxiusTitol = llistaDirectoriDescarregues(patroTitol)

      for (arxiuT in arxiusTitol) {
         val t = arxiuT.name!!.replace(".txt", "")
         val patroArxius = Regex("""${t}-[a-z]+?-[0-9]+?\.txt""")
         arxius = llistaDirectoriDescarregues(patroArxius)
         if (arxius.isNotEmpty()) {
            titol = t
            break
         }
      }
      // Obtenir la llista d'actors
      if (titol != "") {
         val patroActor = """[a-z]+?-([a-z]+?)-[0-9]+?\.txt""".toRegex()
         var llistaActors: Array<String> = arrayOf()
         for (arxiu in arxius) {
            val m = patroActor.find(arxiu.name.toString())
            if (m != null) {
               llistaActors += m.groupValues[1]
            }
         }
         objCompanyia.setTitol(titol)
         objCompanyia.setActors(llistaActors.distinct().sorted().toMutableList())
      }
   }

   /*
   Verifica si existeix l'arxiu de dades de la Companyia.
   Si existeix el carrega. Si no existeix, obté les dades de la Companyia a partir dels arxius de text.
   */
   suspend fun verificaDadesCompanyia(context: Context, frag: FragmentConfiguracioBinding?): JSONObject? {
      var dades: JSONObject? = null
      withContext(Dispatchers.IO) {
         if (File(context.filesDir, arxiuParametres).exists()) {
            dades = llegeixJsonArxiu(arxiuParametres, context, frag!!)
         }
         if (dades == null) {
            obtenirDadesCompanyia()
         } else {
            objCompanyia.set(dades)
         }
      }
      return dades
   }

   /*
   Escriu a l'arxiu de paràmetres les dades de la Companyia en format JSON
   */
   fun desaJsonArxiu(file: String?, data: JSONObject?, context: Context): Boolean {
      return try {
         val arxiu = file ?: arxiuParametres
         val dades = data ?: objCompanyia.get()
         context.openFileOutput(arxiu, Context.MODE_PRIVATE).use {
            it.write(dades.toString().toByteArray())
         }
         true
      } catch (e: IOException) {
         false
      }
   }

   /*
   Llegeix l'arxiu de paràmetres per obtenir les dades en format JSON
   */
   fun llegeixJsonArxiu(file: String?, context: Context, frag: FragmentConfiguracioBinding?): JSONObject? {
      return try {
         val arxiu = file ?: arxiuParametres  //  /data/data/cat.tron.apuntador/files/parametres.json
         //val jsonString = context.openFileInput(arxiu).bufferedReader().use { it.readText() }
         val jsonString = File(context.filesDir, arxiu).readText()
         if (frag != null) {
            frag.instruccions.visibility = View.VISIBLE
            frag.instruccions.text = jsonString
         }
         JSONObject(jsonString)
      } catch (e: Exception) {
         null
      }
   }

   fun canviaIdioma(idioma: String, context: Context) {
      val displayMetrics = context.resources.displayMetrics
      val configuracio = context.resources.configuration
      configuracio.setLocale(Locale(idioma))
      context.resources.updateConfiguration(configuracio, displayMetrics)
      configuracio.locale = Locale(idioma)
      context.resources.updateConfiguration(configuracio, displayMetrics)
   }

   fun dpToPx(context: Context, valor: Int): Float {
      return valor * context.resources.displayMetrics.density
   }

}
