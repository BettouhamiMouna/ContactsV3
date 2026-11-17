package com.example.contactsv3.model

/**
 * Modèle d'un contact.
 * [id] est un identifiant stable : c'est lui (et non la position dans la liste)
 * qui est transmis entre fragments.
 * [latitude] / [longitude] sont nulles lorsque aucune position n'est définie.
 */
data class Contact(
    val id: Int,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val nomComplet: String get() = "$prenom $nom".trim()

    val aDesCoordonnees: Boolean get() = latitude != null && longitude != null
}
