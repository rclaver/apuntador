package cat.tron.apuntador.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.GestorDeVeu
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.FragmentConfiguracioBinding

class ConfiguracioFragment : Fragment() {

   private var _binding: FragmentConfiguracioBinding? = null
   private val binding get() = _binding!!

   private lateinit var formContainer: LinearLayout
   private val opcRegistre = (3..20).map { (it * 0.1).toString().substring(0,3) }.toTypedArray()
   private val opcVelocitat = Array<String>(6) { n -> (0.9f + (n+1).toFloat()/10).toString() }
   private lateinit var botoDesar: Button
   private lateinit var botoInstruccions: Button
   private lateinit var instruccions: TextView
   private lateinit var espera: ProgressBar
   private val opcionsVeu = GestorDeVeu.objVeus.getList(null)
   private lateinit var selectorIdioma: Spinner
   private val opcionsIdioma = arrayOf("Català", "English", "Español")
   private var idiomaItemSelected = false

   data class VistaDadesActors(
      val actor: TextView,
      val seleccioVeu: Spinner,
      val seleccioRegistre: Spinner,
      val seleccioVelocitat: Spinner
   )
   private val formulariActors = mutableListOf<VistaDadesActors>()

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      _binding = FragmentConfiguracioBinding.inflate(inflater, container, false)
      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      initProperties()

      viewLifecycleOwner.lifecycleScope.launchWhenStarted {
         espera.visibility = View.VISIBLE
         Utilitats.verificaDadesCompanyia(requireContext())
         espera.visibility = View.INVISIBLE

         if (Utilitats.objCompanyia.getDisponible() and !Utilitats.objEnFagmentSeleccio.get()) {
            Utilitats.canviaIdioma(Utilitats.objCompanyia.getIdioma(), requireContext())
            findNavController().navigate(R.id.action_ConfiguracioFragment_to_SeleccioFragment)
         } else {
            Utilitats.objEnFagmentSeleccio.set(false)
            creaFormulariConfiguracio(requireContext())
         }
      }

      selectorIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
         override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (idiomaItemSelected) {
               idiomaItemSelected = false
               val idioma = selectorIdioma.selectedItem.toString().substring(0, 2).lowercase()
               GestorDeVeu.objVeus.setIdioma(idioma)
            }else {
               idiomaItemSelected = true
            }
         }
         override fun onNothingSelected(parent: AdapterView<*>) {}
      }

      botoDesar.setOnClickListener {
         val idioma = selectorIdioma.selectedItem.toString().substring(0,2).lowercase()
         val dadesActors = mutableMapOf<String, Map<String,Any>>()
         for (camp in formulariActors) {
            val actor = camp.actor.text.toString()
            val veu = camp.seleccioVeu.selectedItem.toString()
            val registre = camp.seleccioRegistre.selectedItem.toString()
            val velocitat = camp.seleccioVelocitat.selectedItem.toString()
            dadesActors[actor] = mapOf("idioma" to idioma, "veu" to veu, "registre" to registre, "velocitat" to velocitat)
         }
         Utilitats.objCompanyia.setDadesActors(dadesActors)

         Utilitats.objCompanyia.setIdioma(idioma)
         GestorDeVeu.objVeus.setIdioma(idioma)
         Utilitats.canviaIdioma(idioma, requireContext())

         if (Utilitats.desaJsonArxiu(null, null, requireContext())) {
            findNavController().navigate(R.id.action_ConfiguracioFragment_to_SeleccioFragment)
         }
      }

      botoInstruccions.setOnClickListener {
         instruccions.visibility = if (instruccions.isVisible) View.INVISIBLE else View.VISIBLE
      }

   }

   /*
   Crea els elements del formulari, pel panell ConfiguracioFragment, per poder establir
   els paràmetres de veu de cada actor
   */
   private fun creaFormulariConfiguracio(context: Context) {
      // Establir opcions pel selector d'idioma
      var idiomes: Array<String> = arrayOf()
      opcionsIdioma.forEach { idiomes += it.substring(0,2).lowercase() }
      selectorIdioma.adapter = ArrayAdapter(context, R.layout.spinner, opcionsIdioma)
      // Seleccionar, si existeix, l'opció previament desada
      val index = idiomes.indexOf(Utilitats.objCompanyia.getIdioma())
      if (index >= 0) { selectorIdioma.setSelection(index) }

      // Crear els elements de formulari per a cada actor
      var llistaDadesActors = Utilitats.objCompanyia.getDadesActors()
      if (llistaDadesActors.isNotEmpty()) {
         for (dadesActor in llistaDadesActors) { afegirCampsActor(dadesActor, context) }
      }else {
         var llistaActors = Utilitats.objCompanyia.getActors()
         if (llistaActors?.size == 0) {
            Utilitats.obtenirDadesCompanyia()
            llistaActors = Utilitats.objCompanyia.getActors()
         }
         var mapaTemp = mutableMapOf<String, Map<String,Any>>()
         llistaActors!!.forEach { mapaTemp[it] = mapOf<String, Any>() }
         if (mapaTemp.isNotEmpty()) {
            for (dActor in mapaTemp) { afegirCampsActor(dActor, context) }
         }
      }
   }

   private fun afegirCampsActor(dades: MutableMap.MutableEntry<String, Map<String, Any>>, context: Context) {
      val inflater = LayoutInflater.from(context)
      val fila = inflater.inflate(R.layout.fila_formulari, formContainer, false)

      val actor = fila.findViewById<TextView>(R.id.actor)
      actor.text = dades.key.toString().capitalize()

      val seleccioVeu = fila.findViewById<Spinner>(R.id.selectorVeus)
      seleccioVeu.adapter = ArrayAdapter(context, R.layout.spinner, opcionsVeu)
      // Seleccionar, si existeix, l'opció previament desada
      dades.value["veu"].let { elem ->
         val index = opcionsVeu.indexOf(elem)
         if (index >= 0) { seleccioVeu.setSelection(index) }
      }

      val seleccioRegistre = fila.findViewById<Spinner>(R.id.selectorRegistre)
      seleccioRegistre.adapter = ArrayAdapter(context, R.layout.spinner, opcRegistre)
      dades.value["registre"].let { elem ->
         val index = opcRegistre.indexOf(elem)
         if (index >= 0) { seleccioRegistre.setSelection(index) }
      }

      val seleccioVelocitat = fila.findViewById<Spinner>(R.id.selectorVelocitat)
      seleccioVelocitat.adapter = ArrayAdapter(context, R.layout.spinner, opcVelocitat)
      dades.value["velocitat"].let { elem ->
         val index = opcVelocitat.indexOf(elem)
         if (index >= 0) { seleccioVelocitat.setSelection(index) }
      }

      val botoPlay = fila.findViewById<ImageButton>(R.id.botoPlay)
      botoPlay.setOnClickListener {
         val veu = seleccioVeu.selectedItem.toString()
         val registre = seleccioRegistre.selectedItem.toString()
         val velocitat = seleccioVelocitat.selectedItem.toString()
         val llengua = selectorIdioma.selectedItem.toString().substring(0, 2).lowercase()
         GestorDeVeu.canta(veu, registre, velocitat, llengua)
      }

      formContainer.addView(fila)

      formulariActors.add(
         VistaDadesActors(
            actor = actor,
            seleccioVeu = seleccioVeu,
            seleccioRegistre = seleccioRegistre,
            seleccioVelocitat = seleccioVelocitat
         )
      )
   }

   private fun initProperties() {
      formContainer = binding.formContainer
      selectorIdioma = binding.selectorIdioma
      botoDesar = binding.botoDesar
      botoInstruccions = binding.botoInstruccions
      instruccions = binding.instruccions
      espera = binding.espera
   }

   override fun onDestroyView() {
      super.onDestroyView()
      _binding = null
   }
}
