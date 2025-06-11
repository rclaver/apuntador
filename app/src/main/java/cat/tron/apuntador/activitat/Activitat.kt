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
   private var enPausa = false
   private var stop = false
   private var pendentEscolta = false
   private val regexPersonatge = """^(\w*?\s?)(:\s?)(.*$)""".toRegex()
   private val regexNarrador = """([^\(]*)(\(.*?\))(.*)""".toRegex()
   private val patroEscena = Regex("""\(.*\)""")

   private var personatges = mutableMapOf<String, String>()
   private val narrador = GestorDeVeu.objVeus.getNarrador()

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

   fun canviEstat(stat: String) {
      estat = stat
      enPausa = (estat == "pausa")
      stop = (estat == "stop")
      if (estat == "primer_inici") {
         iniciAssaig()
      }
   }

   fun iniciAssaig() {
      GestorDeVeu.objTTS.inici()

      CoroutineScope(Dispatchers.Main).launch {
         withContext(Dispatchers.Main) {
            frgAssaig.escenaActual.text = ""
            frgAssaig.narracio.text = ""
         }

         if (objActor.esObraSencera()) {
            val escena = Utilitats.obraSencera("${titol}.txt")
            processaEscena(escena)
         } else {
            val escenes = Utilitats.llistaFragmentsObra(
               "${titol}.*\\.txt",
               "${titol}-${actor.lowercase()}-[0-9]*.txt",
               "${titol}.txt"
            ).sortedBy { it.name }
            val nEscenes = escenes.size
            var i = 0
            while (i <= nEscenes) {
               if (stop) {
                  break
               } else if (estat == "anterior" && i > 0) {
                  i--
                  estat = "inici"
               } else if (estat == "següent" && i < nEscenes) {
                  i++
                  estat = "inici"
               }
               processaEscena(escenes[i], i, nEscenes)
               i++
            }
         }
      }
   }

   private suspend fun processaEscena(fitxerEscena: DocumentFile? = null, i: Int = 0, nEscenes:Int = 0) {

      if (fitxerEscena?.exists() == true ) {
         val sentencies = Utilitats.llegeixArxiu(ctxAssaig, fitxerEscena).split('\n')

         for (sentencia in sentencies) {
            var ret = ""
            var nar = ""
            if (sentencia.isNotEmpty()) {
               try {
                  val ma = regexPersonatge.find(sentencia)!!
                  val personatge = ma.groupValues[1]
                  nar = processaFragment(personatge, narrador, ":", true)
                  val veu = personatges[personatge] ?: narrador
                  try {
                     val mb = regexNarrador.find(ma.groupValues[3])!!
                     if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                        ret += processaFragment(mb.groupValues[1], veu, " ")
                        nar += processaFragment(mb.groupValues[2], narrador, " ", true)
                        ret += processaFragment(mb.groupValues[3], veu, "\n")
                     } else if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty()) {
                        ret += processaFragment(mb.groupValues[1], veu, " ")
                        nar += processaFragment(mb.groupValues[2], narrador, "\n", true)
                     } else if (mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                        nar += processaFragment(mb.groupValues[2], narrador, " ", true)
                        ret += processaFragment(mb.groupValues[3], veu, "\n")
                     }
                  } catch (e: Exception) {
                     ret += processaFragment(ma.groupValues[3], veu, "\n")
                  }
               } catch (e: Exception) {
                  nar += processaFragment(sentencia, narrador, "\n", true)
               }
               withContext(Dispatchers.Main) {
                  if (nar.isEmpty()) {
                     frgAssaig.escenaActual.text = ret
                  }else {
                     frgAssaig.narracio.text = nar
                  }
               }
               delay(50) // Espera per donar temps a l'usuari (i a la UI)
            }
            if (stop || (estat=="anterior" && i>0) || (estat=="següent" && i<nEscenes)) {
               break  // detenir la lectura
            }
            while (enPausa) {delay(50) } // esperar mentre estigui en pausa
         }
      }
   }

   private suspend fun processaFragment(text: String, veu: String, ends: String, esNarracio: Boolean = false): String {
      var ret = ""
      val subText = patroEscena.replace(text, "")

      if (subText.lowercase() == actor.lowercase()) {
         pendentEscolta = !objActor.esObraSencera()
         ret = mostraSentencia(subText, ends, esNarracio)
      } else if (pendentEscolta) {
         pendentEscolta = false
         frgAssaig.escenaActual.text = subText  //mostra el text de l'actor
         ret = escoltaActor(subText, esNarracio)
      } else {
         ret += GestorDeVeu.textToAudio(subText, veu, ends, esNarracio, objActor.esObraSencera(), this)
         delay(100)
      }
      return ret
   }

   suspend fun mostraSentencia(text: String, ends: String, esNarracio: Boolean = false): String {
      val ret = "${text}${ends}"
      withContext(Dispatchers.Main) {
         if (esNarracio || ends == ":") {
            frgAssaig.narracio.text = ret
            //delay(100)
         } else {
            frgAssaig.escenaActual.text = patroEscena.replace(ret, "")
         }
      }
      //delay(100)
      return ret
   }

   private suspend fun mostraError(text: String) {
      withContext(Dispatchers.Main) {
         frgAssaig.error.text = text
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
            //Utilitats.espera(5000)
         }
      }else {
         mostraError(cR.getString(R.string.error_no_escolto_res))
      }
      if (encert < 80) {
         delay(100)
         GestorDeVeu.textToAudio(originalText, personatges[actor] ?: narrador, "\n", esNarracio, objActor.esObraSencera(), this)
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
         personatges.clear()
         for ((actor, params) in llista) {
            personatges.plus(actor to mapOf(
               "veu" to GestorDeVeu.objVeus.get(params["veu"].toString()),
               "registre" to params["registre"],
               "velocitat" to params["velocitat"]
               )
            )
         }
      }
   }

}
