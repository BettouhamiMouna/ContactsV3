package com.example.contactsv3.model

/**
 * Validation des champs de la fiche contact (fonction pure, testable sans Android).
 */
object ContactValidator {

    enum class Champ { NOM, PRENOM, TELEPHONE, EMAIL, LATITUDE, LONGITUDE }

    enum class Erreur { OBLIGATOIRE, TELEPHONE_INVALIDE, EMAIL_INVALIDE, NON_NUMERIQUE,
        LATITUDE_HORS_LIMITES, LONGITUDE_HORS_LIMITES, GPS_INCOMPLET }

    data class Saisie(
        val nom: String,
        val prenom: String,
        val telephone: String,
        val email: String,
        val latitude: String,
        val longitude: String
    )

    data class Resultat(
        val erreurs: Map<Champ, Erreur>,
        val latitude: Double?,
        val longitude: Double?
    ) {
        val estValide: Boolean get() = erreurs.isEmpty()
    }

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val TELEPHONE = Regex("^\\+?[0-9 .()-]+$")

    const val LAT_MIN = -90.0
    const val LAT_MAX = 90.0
    const val LON_MIN = -180.0
    const val LON_MAX = 180.0

    /** Accepte la virgule ou le point décimal ; refuse NaN / infini. */
    fun lireCoordonnee(texte: String): Double? =
        texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    fun latitudeValide(lat: Double?): Boolean = lat != null && lat in LAT_MIN..LAT_MAX
    fun longitudeValide(lon: Double?): Boolean = lon != null && lon in LON_MIN..LON_MAX

    fun telephoneValide(tel: String): Boolean {
        val t = tel.trim()
        return TELEPHONE.matches(t) && t.count { it.isDigit() } in 4..15
    }

    fun emailValide(email: String): Boolean = EMAIL.matches(email.trim())

    fun valider(s: Saisie): Resultat {
        val erreurs = linkedMapOf<Champ, Erreur>()

        if (s.nom.isBlank()) erreurs[Champ.NOM] = Erreur.OBLIGATOIRE
        if (s.prenom.isBlank()) erreurs[Champ.PRENOM] = Erreur.OBLIGATOIRE
        when {
            s.telephone.isBlank() -> erreurs[Champ.TELEPHONE] = Erreur.OBLIGATOIRE
            !telephoneValide(s.telephone) -> erreurs[Champ.TELEPHONE] = Erreur.TELEPHONE_INVALIDE
        }
        if (s.email.isNotBlank() && !emailValide(s.email)) erreurs[Champ.EMAIL] = Erreur.EMAIL_INVALIDE

        // Coordonnées : les deux vides => pas de position ; sinon les deux sont exigées.
        var lat: Double? = null
        var lon: Double? = null
        val latVide = s.latitude.isBlank()
        val lonVide = s.longitude.isBlank()
        if (!(latVide && lonVide)) {
            if (latVide) erreurs[Champ.LATITUDE] = Erreur.GPS_INCOMPLET
            else {
                lat = lireCoordonnee(s.latitude)
                when {
                    lat == null -> erreurs[Champ.LATITUDE] = Erreur.NON_NUMERIQUE
                    !latitudeValide(lat) -> erreurs[Champ.LATITUDE] = Erreur.LATITUDE_HORS_LIMITES
                }
            }
            if (lonVide) erreurs[Champ.LONGITUDE] = Erreur.GPS_INCOMPLET
            else {
                lon = lireCoordonnee(s.longitude)
                when {
                    lon == null -> erreurs[Champ.LONGITUDE] = Erreur.NON_NUMERIQUE
                    !longitudeValide(lon) -> erreurs[Champ.LONGITUDE] = Erreur.LONGITUDE_HORS_LIMITES
                }
            }
        }
        return Resultat(erreurs, lat, lon)
    }
}
