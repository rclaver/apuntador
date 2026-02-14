package cat.tron.apuntador.activitat

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import cat.tron.apuntador.R
import cat.tron.apuntador.databinding.FragmentAssaigBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activitat : AppCompatActivity() {
   private lateinit var ctxAssaig: Context
   private lateinit var frgAssaig: FragmentAssaigBinding
   lateinit var cR: Resources

   private var titol = ""
   private var actor = ""
   private var estat = "inici"
   private var salta = 0
   private var pendentEscolta = false
   private val regexPersonatge = """^(\w*?\s?)(:\s?)(.*$)""".toRegex()
   private val regexNarrador = """([^\(]*)(\(.*?\))(.*)""".toRegex()
   private val patroEscena = Regex("""\(.*?\)""")

   private var personatges = mutableMapOf<String, Map<String,Any>>()
   private val veuNarrador = GestorDeVeu.objVeus.getVeuNarrador()

   object objActor {
      private var actor: String = ""
      private var sencera: Boolean = false
      fun set(act: String, obra: String) {
         actor = act
         sencera = (act == obra.capitalize())
      }
      fun get(): String = actor
      fun esObraSencera(): Boolean = sencera
   }

   fun setControl(control: String) {
      estat = control
      if (control == "primer_inici") iniciAssaig()
   }

   fun iniciAssaig() {
      GestorDeVeu.objTTS.inici()

      CoroutineScope(Dispatchers.Main).launch {
         withContext(Dispatchers.Main) {
            delay(300)
            frgAssaig.escenaActual.text = ""
            frgAssaig.narracio.text = ""
         }

         if (objActor.esObraSencera()) {
            val escena = Utilitats.obraSencera("${titol}.txt")
            processaEscena(escena)
         }else {
            val escenes = Utilitats.llistaFragmentsObra(
               "${titol}.*\\.txt",
               "${titol}-${actor.lowercase()}-[0-9]*.txt",
               "${titol}.txt"
            ).sortedBy { it.name }
            val nEscenes = escenes.size
            var i = 0
            while (i <= nEscenes && estat != "stop") {
               processaEscena(escenes[i], i, nEscenes)
               if (estat == "anterior" ) {
                  if (i > 0) i--
               }else if (i < nEscenes) {
                  i++
               }
               if (estat != "stop") estat = "inici"
            }
         }
      }
   }

   private suspend fun processaEscena(fitxerEscena: DocumentFile? = null, i: Int = 0, nEscenes:Int = 0) {
      if (fitxerEscena?.exists() == true ) {
         val sentencies = Utilitats.llegeixArxiu(ctxAssaig, fitxerEscena).split('\n')

         for (sentencia in sentencies) {
            if (sentencia.isNotEmpty() && estat != "més") {
               try {
                  val ma = regexPersonatge.find(sentencia)!!
                  val personatge = ma.groupValues[1]
                  processaFragment(personatge, veuNarrador, true)
                  val veu = personatges[personatge] ?: veuNarrador
                  try {
                     val mb = regexNarrador.find(ma.groupValues[3])!!
                     if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                        processaFragment(mb.groupValues[1], veu)
                        if (objActor.esObraSencera()) processaFragment(mb.groupValues[2], veuNarrador, true)
                        processaFragment(mb.groupValues[3], veu)
                     } else if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty()) {
                        processaFragment(mb.groupValues[1], veu)
                        if (objActor.esObraSencera()) processaFragment(mb.groupValues[2], veuNarrador, true)
                     } else if (mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                        if (objActor.esObraSencera()) processaFragment(mb.groupValues[2], veuNarrador, true)
                        processaFragment(mb.groupValues[3], veu)
                     }
                  } catch (e: Exception) { //text pla del personatge
                     processaFragment(ma.groupValues[3], veu)
                  }
               } catch (e: Exception) { //text del narrador
                  processaFragment(sentencia, veuNarrador, true)
               }
               delay(150) //espera per donar temps a l'usuari (i a la UI)
            }
            if (estat=="stop" || (estat=="anterior" && i>0) || (estat=="següent" && i<nEscenes)) {
               break  //sortir del bucle de sentències d'aquesta escena
            }
            while (estat=="pausa") delay(50)  //esperar mentre estigui en pausa
            if (estat == "més") {
               if (salta < 2) {
                  salta++
               }else {
                  estat = "inici"
                  salta = 0
               }
            }
         }
      }
   }

   private suspend fun processaFragment(text: String, veu: Map<String, Any>, esNarracio: Boolean = false) {
      val subText = if (esNarracio) text.trim() else patroEscena.replace(text, "").trim()

      if (subText.equals(actor, ignoreCase = true)) {    //subText.lowercase() == actor.lowercase()
         pendentEscolta = !objActor.esObraSencera()
         mostraSentencia(subText, esNarracio)
      } else if (pendentEscolta) {
         pendentEscolta = false
         mostraSentencia(subText, false, 20L)  //mostra el text de l'actor
         escoltaActor(subText, esNarracio)
      } else {
         GestorDeVeu.textToAudio(subText, veu, esNarracio, objActor.esObraSencera(), this)
         delay(100)
      }
   }

   suspend fun mostraSentencia(text: String, esNarracio: Boolean=false, temps: Long=200) {
      withContext(Dispatchers.Main) {
         if (esNarracio) {
            frgAssaig.narracio.text = text
         }else {
            frgAssaig.escenaActual.text = text
         }
         delay(temps)
      }
   }

   private suspend fun mostraError(text: String) {
      withContext(Dispatchers.Main) {
         frgAssaig.error.text = text
         delay(100)
      }
   }

   private suspend fun escoltaActor(text: String, esNarracio: Boolean = false): String {
      val originalText = patroEscena.replace(text, "")
      val nouText = GestorDeVeu.preparaReconeixementDeVeu(ctxAssaig, originalText, frgAssaig)
      var encert = 0
      if (nouText.isNotEmpty()) {
         encert = Utilitats.comparaSequenciesDeText(originalText, nouText)
         if (encert < 80) {
            mostraError(String.format(cR.getString(R.string.encert), encert, originalText, nouText))
         }
      }else {
         mostraError(cR.getString(R.string.error_no_escolto_res))
      }
      if (encert < 80) {
         delay(100)
         GestorDeVeu.textToAudio(originalText, personatges[actor] ?: veuNarrador, esNarracio, objActor.esObraSencera(), this)
         mostraError("")
      }
      return originalText
   }

   fun setUp(fragmentAssaig: FragmentAssaigBinding, contextAssaig: Context) {
      frgAssaig = fragmentAssaig
      ctxAssaig = contextAssaig
      cR = ctxAssaig.resources

      actor = objActor.get()
      titol = Utilitats.objCompanyia.getTitol()
      val llista = Utilitats.objCompanyia.getDadesActors()
      if (llista.isNotEmpty()) {
         for ((actor, params) in llista) {
            val veu = GestorDeVeu.objVeus.getVeu(params["veu"].toString(), params["idioma"].toString())
            val map = mapOf("idioma" to params["idioma"]!!,
                            "veu" to veu,
                            "registre" to params["registre"]!!,
                            "velocitat" to params["velocitat"]!!)
            personatges[actor] = map
         }
      }
   }

}
