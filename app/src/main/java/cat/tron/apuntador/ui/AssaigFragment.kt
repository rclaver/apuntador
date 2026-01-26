package cat.tron.apuntador.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.Activitat
import cat.tron.apuntador.databinding.FragmentAssaigBinding

class AssaigFragment : Fragment() {
   private var _binding: FragmentAssaigBinding? = null
   private val binding get() = _binding!!

   private lateinit var activitat: Activitat
   private var estatIniciat: String? = null

   lateinit var narracio: TextView
   lateinit var btnInici: ImageView
   lateinit var btnPausa: ImageView
   lateinit var btnStop: ImageView
   lateinit var btnAnterior: ImageView
   lateinit var btnSeguent: ImageView
   lateinit var btnMenys: ImageView
   lateinit var btnMes: ImageView

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      _binding = FragmentAssaigBinding.inflate(inflater, container, false)
      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      initProperties()

      narracio.text = String.format(getString(R.string.inici_assaig), Activitat.objActor.get())

      btnInici.setOnClickListener {
         val estat = estatIniciat ?: "primer_inici"
         if (estatIniciat == null) {
            activitat.setUp(binding, view.context.applicationContext)
         }
         estatIniciat = "inici"
         btnInici.visibility = View.INVISIBLE
         btnPausa.visibility = View.VISIBLE
         activitat.estatInicial(estat)
      }

      btnPausa.setOnClickListener {
         btnInici.visibility = View.VISIBLE
         btnPausa.visibility = View.INVISIBLE
         Activitat.objControls.set("pausa")
      }

      btnStop.setOnClickListener {
         btnInici.visibility= View.VISIBLE
         btnPausa.visibility= View.INVISIBLE
         Activitat.objControls.set("stop")
         findNavController().navigate(R.id.action_AssaigFragment_to_SeleccioFragment)
      }

      btnAnterior.setOnClickListener {
         Activitat.objControls.set("anterior")
      }

      btnSeguent.setOnClickListener {
         Activitat.objControls.set("següent")
      }

      btnMenys.setOnClickListener {
         Activitat.objControls.set("menys")
      }

      btnMes.setOnClickListener {
         Activitat.objControls.set("més")
      }
   }

   private fun initProperties() {
      activitat = Activitat()
      narracio = binding.narracio
      btnInici = binding.inici
      btnPausa = binding.pausa
      btnStop = binding.stop
      btnAnterior = binding.anterior
      btnSeguent = binding.seguent
      btnMenys = binding.menys
      btnMes = binding.mes

      btnInici.visibility= View.VISIBLE
      btnPausa.visibility= View.INVISIBLE
   }

   override fun onDestroyView() {
      super.onDestroyView()
      _binding = null
   }
}
