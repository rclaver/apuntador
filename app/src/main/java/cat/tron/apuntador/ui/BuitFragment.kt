package cat.tron.apuntador.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.GestorDeVeu
import cat.tron.apuntador.activitat.Utilitats

class BuitFragment : Fragment() {

   private lateinit var imatge: ImageView

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.fragment_buit, container, false)
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      imatge = view.findViewById(R.id.img_teatre)

      viewLifecycleOwner.lifecycleScope.launchWhenStarted {
         Utilitats.verificaDadesCompanyia(requireContext(), null)
      }

      imatge.setOnClickListener {
         if (Utilitats.objCompanyia.getDisponible()) {
            Utilitats.canviaIdioma(Utilitats.objCompanyia.getIdioma(), requireContext())
            GestorDeVeu.objVeus.setIdioma(Utilitats.objCompanyia.getIdioma())
            findNavController().navigate(R.id.action_BuitFragment_to_SeleccioFragment)
         }else {
            findNavController().navigate(R.id.action_BuitFragment_to_ConfiguracioFragment)
         }
      }
   }

}
