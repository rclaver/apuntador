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

   lateinit var escenaActual: TextView
   lateinit var btnInici: ImageView
   lateinit var btnPausa: ImageView
   lateinit var btnStop: ImageView
   lateinit var btnAnterior: ImageView
   lateinit var btnSeguent: ImageView

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      _binding = FragmentAssaigBinding.inflate(inflater, container, false)
      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      initProperties()

      escenaActual.text = String.format(getString(R.string.inici_assaig), Activitat.objActor.get())

      btnInici.setOnClickListener {
         val estat = estatIniciat ?: "primer_inici"
         if (estatIniciat == null) {
            activitat.setUp(binding, view.context.applicationContext)
         }
         estatIniciat = "inici"
         btnInici.visibility = View.INVISIBLE
         btnPausa.visibility = View.VISIBLE
         activitat.canviEstat(estat)
      }

      btnPausa.setOnClickListener {
         btnInici.visibility = View.VISIBLE
         btnPausa.visibility = View.INVISIBLE
         activitat.canviEstat("pausa")
      }

      btnStop.setOnClickListener {
         btnInici.visibility= View.VISIBLE
         btnPausa.visibility= View.INVISIBLE
         activitat.canviEstat("stop")
         findNavController().navigate(R.id.action_AssaigFragment_to_SeleccioFragment)
      }

      btnAnterior.setOnClickListener {
         activitat.canviEstat("anterior")
      }

      btnSeguent.setOnClickListener {
         activitat.canviEstat("següent")
      }
   }

   private fun initProperties() {
      activitat = Activitat()
      escenaActual = binding.escenaActual
      btnInici = binding.inici
      btnPausa = binding.pausa
      btnStop = binding.stop
      btnAnterior = binding.anterior
      btnSeguent = binding.seguent

      btnInici.visibility= View.VISIBLE
      btnPausa.visibility= View.INVISIBLE
   }

   override fun onDestroyView() {
      super.onDestroyView()
      _binding = null
   }
}
