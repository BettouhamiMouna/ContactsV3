package com.example.contactsv3

/** Navigation entre fragments, implémentée par MainActivity. */
interface Navigateur {
    fun ouvrirDetail(contactId: Int)
    fun ouvrirCarte(contactId: Int)
    fun retour()
}
