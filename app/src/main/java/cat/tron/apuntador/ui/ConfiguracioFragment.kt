package cat.tron.apuntador.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.GestorDeVeu.objVeus
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.FragmentConfiguracioBinding

class ConfiguracioFragment : Fragment() {

   private var _binding: FragmentConfiguracioBinding? = null
   private val binding get() = _binding!!

   private lateinit var formContainer: LinearLayout
   private lateinit var selectorIdioma: Spinner
   private lateinit var selectorRegistre: NumberPicker
   private val opcRegistre = (3..20).map { (it * 0.1).toString().substring(0,3) }.toTypedArray()
   private var registreSelectedItem = ""
   private lateinit var selectorVelocitat: NumberPicker
   private val opcVelocitat = Array<String>(6) { n -> (0.9f + (n+1).toFloat()/10).toString() }
   private var velocitatSelectedItem = ""
   private lateinit var botoDesar: Button
   private lateinit var botoInstruccions: Button
   private lateinit var instruccions: TextView
   private lateinit var espera: ProgressBar
   private val opcionsVeu = objVeus.getList()
   private val opcionsIdioma = arrayOf("Català", "English", "Español")

   data class VistaDadesActors(
      val actor: TextView,
      val seleccioVeu: Spinner,
      val seleccioRegistre: NumberPicker,
      val seleccioVelocitat: NumberPicker
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

      botoDesar.setOnClickListener {
         val dadesActors = mutableMapOf<String, String>()
         for (camps in formulariActors) {
            val actor = camps.actor.text.toString()
            val veu = camps.seleccioVeu.selectedItem.toString()
            dadesActors.put(actor, veu)
         }
         Utilitats.objCompanyia.setDadesActors(dadesActors)

         val idioma = selectorIdioma.selectedItem.toString().substring(0,2).lowercase()
         Utilitats.objCompanyia.setIdioma(idioma)
         Utilitats.canviaIdioma(idioma, requireContext())

         if (Utilitats.desaJsonArxiu(null, null, requireContext())) {
            findNavController().navigate(R.id.action_ConfiguracioFragment_to_SeleccioFragment)
         }
      }

      botoInstruccions.setOnClickListener {
         if (instruccions.isVisible) {
            instruccions.visibility = View.INVISIBLE
         }else {
            instruccions.visibility = View.VISIBLE
         }
      }

   }

   /*
   Crea els elements del formulari, pel panell ConfiguracioFragment, per poder establir
   els paràmetres de veu de cada actor
   */
   private fun creaFormulariConfiguracio(context: Context) {
      // establir opcions pel selector d'idioma
      var idiomes: Array<String> = arrayOf()
      opcionsIdioma.forEach {
         idiomes += it.substring(0,2).lowercase()
      }
      val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, opcionsIdioma)
      selectorIdioma.adapter = adapter
      // Seleccionar, si existeix, l'opció previament desada
      val index = idiomes.indexOf(Utilitats.objCompanyia.getIdioma())
      if (index >= 0) { selectorIdioma.setSelection(index) }

      // crear els elements de formulari per a cada actor
      var llistaDadesActors = Utilitats.objCompanyia.getDadesActors()
      if (llistaDadesActors.isNotEmpty()) {
         for ((actor, veu) in llistaDadesActors) { afegirCampsActor(mapOf("actor" to actor, "veu" to veu), context) }
      }else {
         var llistaActors = Utilitats.objCompanyia.getActors()
         if (llistaActors?.size == 0) {
            Utilitats.obtenirDadesCompanyia()
            llistaActors = Utilitats.objCompanyia.getActors()
         }
         llistaActors!!.forEach { afegirCampsActor(mapOf("actor" to it, "veu" to ""), context) }
      }
   }

   private fun afegirCampsActor(dades: Map<String, String>, context: Context) {
      val alt = Utilitats.dpToPx(context, 30).toInt()
      val actor = TextView(context).apply {
         text = dades["actor"].toString().capitalize()
         textSize = 16f
         setTypeface(null, Typeface.BOLD)
         setPadding(40, 2, 60, 2)
         layoutParams = LinearLayout.LayoutParams(0, alt, 1f)
      }
      val seleccioVeu = Spinner(context).apply {
         adapter = ArrayAdapter(context, R.layout.spinner, opcionsVeu)
         setPadding(40, 2, 0, 2)
         // Seleccionar, si existeix, l'opció previament desada
         dades["veu"].let { veuDesada ->
            val index = opcionsVeu.indexOf(veuDesada)
            if (index >= 0) { setSelection(index) }
         }
      }

      selectorRegistre.minValue = 0
      selectorRegistre.maxValue = opcRegistre.size - 1
      selectorRegistre.displayedValues = opcRegistre
      selectorRegistre.wrapSelectorWheel = false

      selectorVelocitat.minValue = 0
      selectorVelocitat.maxValue = opcRegistre.size - 1
      selectorVelocitat.displayedValues = opcVelocitat
      selectorVelocitat.wrapSelectorWheel = false

      val fila = LinearLayout(context).apply {
         orientation = LinearLayout.HORIZONTAL
         layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
         ).apply {
            setMargins(8, 2, 8, 2)
            gravity = View.TEXT_ALIGNMENT_VIEW_START
         }
         addView(actor)
         addView(seleccioVeu)
      }
      formContainer.addView(fila)

      formulariActors.add(
         VistaDadesActors(
            actor = actor,
            seleccioVeu = seleccioVeu,
            seleccioRegistre = selectorRegistre,
            seleccioVelocitat = selectorRegistre
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
