package com.example.contactsv3.model

import java.text.Normalizer
import java.util.Locale

/**
 * Filtrage des contacts : nom, prénom, nom complet et téléphone.
 * - insensible à la casse (et aux accents : « helene » trouve « Hélène ») ;
 * - pour le téléphone, une saisie composée de chiffres est comparée sans les espaces,
 *   points ou tirets (« 0612 » trouve « 06 12 34 56 78 »).
 * Fonction pure, testable sans Android (voir ContactFilterTest).
 */
object ContactFilter {

    private val ACCENTS = Regex("\\p{Mn}+")

    fun normaliser(texte: String): String =
        Normalizer.normalize(texte.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(ACCENTS, "")
            .replace(Regex("\\s+"), " ")

    private fun chiffres(texte: String): String = texte.filter { it.isDigit() }

    private fun ressembleAUnNumero(requete: String): Boolean =
        requete.any { it.isDigit() } && requete.all { it.isDigit() || it in " +.-()" }

    fun correspond(contact: Contact, requete: String): Boolean {
        val q = normaliser(requete)
        if (q.isEmpty()) return true

        val champsTexte = listOf(
            contact.nom,
            contact.prenom,
            "${contact.prenom} ${contact.nom}",
            "${contact.nom} ${contact.prenom}",
            contact.telephone
        )
        if (champsTexte.any { normaliser(it).contains(q) }) return true

        if (ressembleAUnNumero(q)) {
            val qChiffres = chiffres(q)
            if (qChiffres.isNotEmpty() && chiffres(contact.telephone).contains(qChiffres)) return true
        }
        return false
    }

    fun filtrer(contacts: List<Contact>, requete: String): List<Contact> =
        if (normaliser(requete).isEmpty()) contacts else contacts.filter { correspond(it, requete) }
}
