package cat.tron.apuntador.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.Activitat
import cat.tron.apuntador.activitat.Utilitats
import cat.tron.apuntador.databinding.FragmentSeleccioBinding
import java.util.Locale
import kotlin.system.exitProcess

class SeleccioFragment : Fragment() {
   private var _binding: FragmentSeleccioBinding? = null
   private val binding get() = _binding!!

   private lateinit var radioGrupActors: RadioGroup
   private lateinit var botoConfigurar: Button
   private lateinit var botoTancar: Button

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      _binding = FragmentSeleccioBinding.inflate(inflater, container, false)
      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      initProperties()

      viewLifecycleOwner.lifecycleScope.launchWhenStarted {
         Utilitats.verificaDadesCompanyia(requireContext(), null)
      }

      creaBotonsDeRadio(requireContext(), radioGrupActors, getString(R.string.obra_sencera))

      radioGrupActors.setOnCheckedChangeListener { group, checkedId ->
         val checkedRadioButtonId = radioGrupActors.checkedRadioButtonId
         val radioBtn: RadioButton = view.findViewById(checkedRadioButtonId)
         val nomActor = radioBtn.text.toString()
         Activitat.objActor.set(nomActor, getString(R.string.obra_sencera))

         findNavController().navigate(R.id.action_SeleccioFragment_to_AssaigFragment)
      }

      botoConfigurar.setOnClickListener {
         Utilitats.objEnFagmentSeleccio.set(true)
         findNavController().navigate(R.id.action_SeleccioFragment_to_ConfiguracioFragment)
      }

      botoTancar.setOnClickListener {
         val frAct = this.activity
         frAct?.finish()
         exitProcess(0)
      }
   }

   /*
   Crea els elements botó de radio, pel panell SeleccioFragment, per seleccionar el conjunt
   de fragments corresponents a cada actor o l'obra sencera
   */
   private fun creaBotonsDeRadio(context: Context, radioGrupActors: RadioGroup, obra: String) {
      val alt = Utilitats.dpToPx(context, 44).toInt()
      var llistaActors = Utilitats.objCompanyia.getActors()

      if (llistaActors != null) {
         llistaActors.add(obra)

         var i = 180
         llistaActors.forEach { actor ->
            val radioButton = RadioButton(context).apply {
               text = actor.toString().capitalize(Locale.ROOT)
               id = i++
               height = alt
               textSize = 14f
            }
            radioGrupActors.addView(radioButton)
         }
      }
   }

   private fun initProperties() {
      radioGrupActors = binding.rgActors
      botoConfigurar = binding.botoConfigurar
      botoTancar = binding.botoTancar
   }

   override fun onDestroyView() {
      super.onDestroyView()
      _binding = null
   }

}
