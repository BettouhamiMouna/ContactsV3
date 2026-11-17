package com.example.contactsv3

import com.example.contactsv3.model.ContactValidator
import com.example.contactsv3.model.ContactValidator.Champ
import com.example.contactsv3.model.ContactValidator.Erreur
import com.example.contactsv3.model.ContactValidator.Saisie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactValidatorTest {

    private fun saisie(lat: String = "", lon: String = "", email: String = "a.b@example.com",
                       tel: String = "06 12 34 56 78", nom: String = "Doe", prenom: String = "Jane") =
        Saisie(nom, prenom, tel, email, lat, lon)

    @Test fun saisieValideSansPosition() {
        val r = ContactValidator.valider(saisie())
        assertTrue(r.estValide)
        assertNull(r.latitude); assertNull(r.longitude)
    }

    @Test fun coordonneesValidesAvecVirgule() {
        val r = ContactValidator.valider(saisie("48,85", "2.29"))
        assertTrue(r.estValide)
        assertEquals(48.85, r.latitude!!, 1e-9)
    }

    @Test fun limitesIncluses() = assertTrue(ContactValidator.valider(saisie("-90", "180")).estValide)

    @Test fun latitudeHorsLimites() =
        assertEquals(Erreur.LATITUDE_HORS_LIMITES, ContactValidator.valider(saisie("90.1", "0")).erreurs[Champ.LATITUDE])

    @Test fun longitudeHorsLimites() =
        assertEquals(Erreur.LONGITUDE_HORS_LIMITES, ContactValidator.valider(saisie("0", "-181")).erreurs[Champ.LONGITUDE])

    @Test fun nonNumerique() {
        val r = ContactValidator.valider(saisie("abc", "NaN"))
        assertEquals(Erreur.NON_NUMERIQUE, r.erreurs[Champ.LATITUDE])
        assertEquals(Erreur.NON_NUMERIQUE, r.erreurs[Champ.LONGITUDE])
    }

    @Test fun positionIncomplete() =
        assertEquals(Erreur.GPS_INCOMPLET, ContactValidator.valider(saisie("10", "")).erreurs[Champ.LONGITUDE])

    @Test fun champsObligatoires() {
        val r = ContactValidator.valider(saisie(nom = " ", prenom = "", tel = ""))
        assertEquals(Erreur.OBLIGATOIRE, r.erreurs[Champ.NOM])
        assertEquals(Erreur.OBLIGATOIRE, r.erreurs[Champ.PRENOM])
        assertEquals(Erreur.OBLIGATOIRE, r.erreurs[Champ.TELEPHONE])
    }

    @Test fun emailEtTelephoneInvalides() {
        val r = ContactValidator.valider(saisie(email = "pas-un-mail", tel = "abc"))
        assertEquals(Erreur.EMAIL_INVALIDE, r.erreurs[Champ.EMAIL])
        assertEquals(Erreur.TELEPHONE_INVALIDE, r.erreurs[Champ.TELEPHONE])
    }

    @Test fun emailFacultatif() = assertTrue(ContactValidator.valider(saisie(email = "")).estValide)
}
