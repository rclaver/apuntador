package cat.tron.apuntador.ui

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cat.tron.apuntador.MainActivity
import cat.tron.apuntador.R
import cat.tron.apuntador.activitat.GestorDeVeu
import cat.tron.apuntador.activitat.Utilitats
import java.io.File


class BuitFragment : Fragment() {

   private lateinit var imatge: ImageView
   private lateinit var notaVersio: TextView
   private var _bindMainActivity: MainActivity? = null
   private val bindMainActivity get() = _bindMainActivity!!

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.fragment_buit, container, false)
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      imatge = view.findViewById(R.id.img_teatre)
      notaVersio = view.findViewById(R.id.notaVersio)

      viewLifecycleOwner.lifecycleScope.launchWhenStarted {
         notaVersio.text = mostraVersio()
         Utilitats.verificaDadesCompanyia(requireContext())
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
      notaVersio.setOnClickListener {
         notaVersio.text = mostraVersio(bindMainActivity.directoriDocuments)
      }
   }

   fun mostraVersio(nota:String=""): String {
      var ret = nota
      val documentsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      /*var listDir = ""
      downloadsDir.listFiles()!!.forEach { file ->
         listDir += "${file}\n"
      }
      ret += "directori documents: ${documentsDir}\n" + listDir*/
      ret += "directori documents:\n  ${documentsDir}\n"
      ret += "${Build.MANUFACTURER} ${Build.MODEL}\n" +
             "ver. Android: ${Build.VERSION.RELEASE}"
      return ret
   }

}
