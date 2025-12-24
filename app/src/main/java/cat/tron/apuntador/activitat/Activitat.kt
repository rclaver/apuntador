package cat.tron.apuntador.activitat

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import cat.tron.apuntador.R
import cat.tron.apuntador.databinding.FragmentAssaigBinding

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

   private var personatges = mutableMapOf<String, Map<String,Any>>()
   private val narrador = GestorDeVeu.objVeus.getVeuNarrador()

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

   object objSentencia {
      private var sentencies = mutableListOf<String>()
      fun set(e: MutableList<String>) {sentencies = e}
      fun get(i: Int): String = sentencies[i]
      fun getSize(): Int = sentencies.size
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

      frgAssaig.escenaActual.text = ""
      frgAssaig.narracio.text = ""

      if (objActor.esObraSencera()) {
         val escenes = Utilitats.obraSencera("${titol}.txt")
         processaEscena(escenes[0])
      } else {
         val escenes = Utilitats.llistaFragmentsObra(
            "${titol}.*\\.txt",
            "${titol}-${actor.lowercase()}-[0-9]*.txt",
            "${titol}.txt"
         ).sortedBy { it.name }
         val nEscenes = escenes.size
         var i = 0
         while (i <= nEscenes && ! stop) {
            processaEscena(escenes[i])
            if (estat == "anterior" ) {
               if (i > 0) i--
            }else if (i < nEscenes) {
               i++
               estat = "inici"
            }
            estat = "inici"
         }
      }
   }

   private fun processaEscena(fitxerEscena:DocumentFile? = null) {
      if (fitxerEscena?.exists() == true) {
         val sentencies = Utilitats.llegeixArxiu(ctxAssaig, fitxerEscena).split('\n')
         objSentencia.set(sentencies as MutableList<String>)
         processaSentencies(0)
      }
   }

   private fun processaSentencies(i: Int = 0) {
      if (objSentencia.getSize() < i) return
      val sentencia = objSentencia.get(i)
      var ret = ""
      var nar = ""
      if (sentencia.isNotEmpty()) {
         try {
            val ma = regexPersonatge.find(sentencia)!!
            val personatge = ma.groupValues[1]
            processaFragment(personatge, narrador, ":", true) {r -> nar = r}
            val veu = personatges[personatge] ?: narrador
            try {
               val mb = regexNarrador.find(ma.groupValues[3])!!
               if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                  processaFragment(mb.groupValues[1], veu, " ") {r -> ret += r}
                  processaFragment(mb.groupValues[2], narrador, " ", true) {r -> nar += r}
                  processaFragment(mb.groupValues[3], veu, "\n") {r -> ret += r}
               } else if (mb.groupValues[1].isNotEmpty() && mb.groupValues[2].isNotEmpty()) {
                  processaFragment(mb.groupValues[1], veu, " ") {r -> ret += r}
                  processaFragment(mb.groupValues[2], narrador, "\n", true) {r -> nar += r}
               } else if (mb.groupValues[2].isNotEmpty() && mb.groupValues[3].isNotEmpty()) {
                  processaFragment(mb.groupValues[2], narrador, " ", true) {r -> nar += r}
                  processaFragment(mb.groupValues[3], veu, "\n") {r -> ret += r}
               }
            } catch (e: Exception) {
               processaFragment(ma.groupValues[3], veu, "\n") {r -> ret += r}
            }
         } catch (e: Exception) {
            processaFragment(sentencia, narrador, "\n", true) {r -> nar += r}
         }
         if (nar.isEmpty()) {
            frgAssaig.escenaActual.text = ret
         }else {
            frgAssaig.narracio.text = nar
         }
      }
      while (enPausa) { true }
      if (! stop ) processaSentencies(i+1)
   }

   private fun processaFragment(text: String,
                                veu: Map<String, Any>,
                                ends: String,
                                esNarracio: Boolean = false,
                                onFinish: (String) -> Unit) {
      val subText = patroEscena.replace(text, "")

      if (subText.equals(actor, ignoreCase = true)) { //subText.lowercase() == actor.lowercase()
         pendentEscolta = !objActor.esObraSencera()
         mostraSentencia(subText, ends, esNarracio) { result -> onFinish(result) }
      } else if (pendentEscolta) {
         pendentEscolta = false
         frgAssaig.escenaActual.text = subText  //mostra el text de l'actor
         val originalText = patroEscena.replace(subText, "")
         escoltaActor(originalText) { result ->
            onFinish(evaluaActor(originalText, result, esNarracio) {}.toString())
         }
      } else {
         GestorDeVeu.textToAudio(subText, veu, ends, esNarracio, objActor.esObraSencera(), this)
         onFinish(subText)
      }
   }

   private fun escoltaActor(text: String, onFinish: (String) -> Unit) {
      GestorDeVeu.preparaReconeixementDeVeu(
         this, text, frgAssaig,
         onResultat = { result ->
            if (result.isEmpty()) {
               mostraError(cR.getString(R.string.error_no_escolto_res)) {}
            }
            onFinish(result)
         },
         onError = { error ->
            mostraError(error) {}
            onFinish("")
         }
      )
   }

   private fun evaluaActor(originalText: String, nouText: String, esNarracio: Boolean = false, onFinish: (String) -> Unit) {
      var encert = 0
      if (nouText.isNotEmpty()) {
         encert = Utilitats.comparaSequenciesDeText(originalText, nouText)
         if (encert < 80) {
            mostraError(String.format(cR.getString(R.string.encert), encert, originalText, nouText)){}
         }
      }else {
         mostraError(cR.getString(R.string.error_no_escolto_res)) {}
      }
      if (encert < 80) {
         GestorDeVeu.textToAudio(originalText, personatges[actor] ?: narrador, "\n", esNarracio, objActor.esObraSencera(), this)
         mostraError {}
      }
      onFinish(originalText)
   }

   fun mostraSentencia(text: String, ends: String, esNarracio: Boolean = false, onFinish: (String) -> Unit) {
      val result = "${text}${ends}"
      if (esNarracio || ends == ":") {
         frgAssaig.narracio.text = result
      } else {
         frgAssaig.escenaActual.text = patroEscena.replace(result, "")
      }
      onFinish(result)
   }

   private fun mostraError(text: String = "", onFinish: () -> Unit) {
      frgAssaig.error.text = text
      onFinish()  //modelo asíncrono: espera a que el proceso finalice
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
