package com.example.contactsv3

import com.example.contactsv3.model.ContactFilter
import com.example.contactsv3.model.ContactsDemo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFilterTest {

    private val contacts = ContactsDemo.liste()

    private fun ids(requete: String) = ContactFilter.filtrer(contacts, requete).map { it.id }

    @Test fun filtreParNom() = assertEquals(listOf(2), ids("martin"))

    @Test fun filtreParPrenom() = assertEquals(listOf(3), ids("Karim"))

    @Test fun insensibleALaCasse() = assertEquals(ids("benali"), ids("BeNaLi"))

    @Test fun insensibleAuxAccents() = assertEquals(listOf(6), ids("helene"))

    @Test fun filtreParTelephoneAvecOuSansEspaces() {
        assertEquals(listOf(2), ids("06 98"))
        assertEquals(listOf(2), ids("0698"))
    }

    @Test fun filtreParNomComplet() = assertEquals(listOf(5), ids("omar el amrani"))

    @Test fun aucunResultat() = assertTrue(ids("zzzz").isEmpty())

    @Test fun filtreVideRendTout() {
        assertEquals(contacts.size, ids("").size)
        assertEquals(contacts.size, ids("   ").size)
    }
}
