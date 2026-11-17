package com.example.contactsv3.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsv3.ContactsViewModel
import com.example.contactsv3.Navigateur
import com.example.contactsv3.R
import com.example.contactsv3.databinding.FragmentListeContactsBinding

/** Écran 1 : liste des contacts + recherche en temps réel. */
class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListeContactsBinding.bind(view)

        val adapter = ContactAdapter { contact ->
            (requireActivity() as Navigateur).ouvrirDetail(contact.id)
        }
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        // Filtrage au fil de la saisie, sans bouton de validation.
        // L'icône « effacer » du TextInputLayout vide le champ => tous les contacts réapparaissent.
        binding.editRecherche.doAfterTextChanged { texte ->
            viewModel.definirFiltre(texte?.toString().orEmpty())
        }

        viewModel.contactsFiltres.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            val requete = viewModel.filtre.value.orEmpty().trim()
            binding.textVide.isVisible = liste.isEmpty()
            binding.textVide.text = if (requete.isEmpty()) getString(R.string.liste_vide)
            else getString(R.string.aucun_resultat, requete)
            binding.textCompteur.text = getString(R.string.nb_contacts, liste.size)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.titre_liste)
    }

    override fun onDestroyView() {
        binding.recyclerContacts.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
