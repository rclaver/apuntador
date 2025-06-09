package cat.tron.apuntador.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.ReconeixementDeVeu.objVeus
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.FragmentConfiguracioBinding

class ConfiguracioFragment2 : Fragment() {

   private var _binding: FragmentConfiguracioBinding? = null
   private val binding get() = _binding!!

   private lateinit var formContainer: LinearLayout
   private lateinit var selectorIdioma: Spinner
   private lateinit var botoDesar: Button
   private lateinit var botoInstruccions: Button
   private lateinit var instruccions: TextView
   private lateinit var espera: ProgressBar
   private val opcionsVeu = objVeus.getList()
   private val opcionsIdioma = mapOf("ca" to "Català", "es" to "Castellano")

   data class VistaDadesActors(
      val actor: TextView,
      val seleccioVeu: Spinner,
      val inputVelocitat: EditText,
      val inputRegistre: EditText
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
         val dadesActors = mutableMapOf<String, Map<String,Any>>()
         for (camps in formulariActors) {
            val actor = camps.actor.text.toString()
            val veu = camps.seleccioVeu.selectedItem.toString()
            val velocitatText = camps.inputVelocitat.text.toString()
            val registreText = camps.inputRegistre.text.toString()
            // Validació simple
            val velocitat = velocitatText.toFloatOrNull() ?: 1.0f
            val registre = registreText.toFloatOrNull() ?: 1.0f
            dadesActors.put(actor, mapOf("veu" to veu, "velocitat" to velocitat, "registre" to registre))
         }
         Utilitats.objCompanyia.setDadesActors(dadesActors)

         val idioma = selectorIdioma.selectedItem.toString()
         Utilitats.objCompanyia.setIdioma(idioma.substring(0,2))
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
      var seleccioIdiomes: Array<String> = arrayOf()
      opcionsIdioma.forEach {
         idiomes += it.key
         seleccioIdiomes += "${it.key}-${it.value}"
      }
      val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, seleccioIdiomes)
      selectorIdioma.adapter = adapter
      // Seleccionar, si existeix, l'opció previament desada
      val index = idiomes.indexOf(Utilitats.objCompanyia.getIdioma())
      if (index >= 0) { selectorIdioma.setSelection(index) }

      // crear els elements de formulari per a cada actor
      var llistaDadesActors = Utilitats.objCompanyia.getDadesActors()
      if (llistaDadesActors != null) {
         for (actor in llistaDadesActors) { afegirCampsActor(actor, context) }
      }else {
         var llistaActors = Utilitats.objCompanyia.getActors()
         if (llistaActors?.size == 0) {
            Utilitats.obtenirDadesCompanyia()
            llistaActors = Utilitats.objCompanyia.getActors()
         }
         var ltmp = mutableMapOf<String, Map<String,Any>>()
         llistaActors!!.forEach { ltmp[it] = mapOf<String, Any>() }
         if (ltmp.isNotEmpty()) {
            for (actor in ltmp) { afegirCampsActor(actor, context) }
         }
      }
   }

   private fun afegirCampsActor(dades: MutableMap.MutableEntry<String, Map<String, Any>>, context: Context) {
      val actor = TextView(context).apply {
         text = dades.key.toString().capitalize()
         textSize = 14f
         setTypeface(null, Typeface.BOLD)
         setPadding(0, 4, 0, 4)
         layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
      }
      val seleccioVeu = Spinner(context).apply {
         adapter = ArrayAdapter(context, R.layout.spinner, opcionsVeu)
         // Seleccionar, si existeix, l'opció previament desada
         dades.value["veu"].let { veuDesada ->
            val index = opcionsVeu.indexOf(veuDesada)
            if (index >= 0) { setSelection(index) }
         }
      }
      val v = dades.value["velocitat"] ?: "1.0"
      val inputVelocitat = EditText(context).apply {
         hint = ""
         textSize = 12f
         setText(v.toString())
         inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
         layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
      }
      val r = dades.value["registre"] ?: "1.0"
      val inputRegistre = EditText(context).apply {
         hint = ""
         textSize = 12f
         setText(r.toString())
         inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
         layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
      }
      val fila = LinearLayout(context).apply {
         orientation = LinearLayout.HORIZONTAL
         layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
         ).apply { setMargins(8, 2, 0, 2) }
         addView(actor)
         addView(seleccioVeu)
         addView(inputVelocitat)
         addView(inputRegistre)
      }
      formContainer.addView(fila)

      formulariActors.add(
         VistaDadesActors(
            actor = actor,
            seleccioVeu = seleccioVeu,
            inputVelocitat = inputVelocitat,
            inputRegistre = inputRegistre
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