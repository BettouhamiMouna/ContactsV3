package com.example.contactsv3

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.contactsv3.model.Contact
import com.example.contactsv3.model.ContactFilter
import com.example.contactsv3.model.ContactsDemo

/**
 * ViewModel PARTAGÉ (portée : MainActivity, obtenu avec `activityViewModels()`).
 *
 * Choix de communication : la liste en mémoire est la source unique de vérité.
 * Le fragment Détail appelle [mettreAJour] ; la liste, qui observe [contactsFiltres],
 * est actualisée automatiquement à son retour. Le ViewModel survit à la rotation,
 * donc ni les modifications ni le filtre en cours ne sont perdus.
 */
class ContactsViewModel : ViewModel() {

    private val _contacts = MutableLiveData(ContactsDemo.liste())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _filtre = MutableLiveData("")
    val filtre: LiveData<String> = _filtre

    /** Liste affichée = contacts filtrés, recalculée dès que les contacts OU le filtre changent. */
    val contactsFiltres: LiveData<List<Contact>> = MediatorLiveData<List<Contact>>().apply {
        val recalculer = {
            value = ContactFilter.filtrer(_contacts.value.orEmpty(), _filtre.value.orEmpty())
        }
        addSource(_contacts) { recalculer() }
        addSource(_filtre) { recalculer() }
    }

    fun definirFiltre(requete: String) {
        if (requete != _filtre.value) _filtre.value = requete
    }

    /** Recherche par identifiant (jamais par position dans la liste affichée). */
    fun getContact(id: Int): Contact? = _contacts.value?.firstOrNull { it.id == id }

    fun mettreAJour(contact: Contact) {
        _contacts.value = _contacts.value.orEmpty().map { if (it.id == contact.id) contact else it }
    }
}
