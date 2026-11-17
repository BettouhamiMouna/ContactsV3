package com.example.contactsv3.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.contactsv3.ContactsViewModel
import com.example.contactsv3.Navigateur
import com.example.contactsv3.R
import com.example.contactsv3.databinding.FragmentCarteContactBinding
import com.example.contactsv3.model.Contact
import com.example.contactsv3.model.ContactValidator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Écran 3 : position enregistrée du contact sur Google Maps.
 * Aucune permission de localisation n'est nécessaire : on n'affiche que des coordonnées connues.
 */
class CarteContactFragment : Fragment(R.layout.fragment_carte_contact), OnMapReadyCallback {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()

    private var contact: Contact? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCarteContactBinding.bind(view)
        binding.btnRetour.setOnClickListener { (requireActivity() as Navigateur).retour() }

        val id = requireArguments().getInt(ARG_CONTACT_ID, -1)
        val c = viewModel.getContact(id)
        contact = c

        when {
            c == null -> afficherMessage(getString(R.string.msg_contact_introuvable))
            c.latitude == null || c.longitude == null ->
                afficherMessage(getString(R.string.msg_pas_coordonnees))
            !ContactValidator.latitudeValide(c.latitude) || !ContactValidator.longitudeValide(c.longitude) ->
                afficherMessage(getString(R.string.msg_coordonnees_invalides,
                    c.latitude.toString(), c.longitude.toString()))
            else -> {
                binding.layoutMessage.isVisible = false
                val carte = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
                carte?.getMapAsync(this)
            }
        }
    }

    private fun afficherMessage(message: String) {
        binding.textMessage.text = message
        binding.layoutMessage.isVisible = true
        binding.mapContainer.isVisible = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (_binding == null) return            // vue déjà détruite
        val c = contact ?: return
        val position = LatLng(c.latitude ?: return, c.longitude ?: return)

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.clear()
        val marqueur = googleMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(c.nomComplet)
                .snippet(c.telephone)
        )
        marqueur?.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM))
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.titre_carte, contact?.nomComplet.orEmpty())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CONTACT_ID = "contact_id"
        private const val ZOOM = 14f

        fun newInstance(contactId: Int) = CarteContactFragment().apply {
            arguments = bundleOf(ARG_CONTACT_ID to contactId)
        }
    }
}
