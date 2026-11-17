package com.example.contactsv3.model

/** Jeu de données de démonstration (collection en mémoire, pas de stockage permanent). */
object ContactsDemo {
    fun liste(): List<Contact> = listOf(
        Contact(1, "Benali", "Yasmine", "+33 6 12 34 56 78", "yasmine.benali@example.com",
            latitude = 48.8584, longitude = 2.2945),          // Paris – Tour Eiffel
        Contact(2, "Martin", "Lucas", "06 98 76 54 32", "lucas.martin@example.com",
            latitude = 45.7640, longitude = 4.8357),          // Lyon
        Contact(3, "Haddad", "Karim", "+213 555 12 34 56", "karim.haddad@example.com",
            latitude = 36.7538, longitude = 3.0588),          // Alger
        Contact(4, "Trabelsi", "Sarra", "+216 98 765 432", "sarra.trabelsi@example.com",
            latitude = 36.8065, longitude = 10.1815),         // Tunis
        Contact(5, "El Amrani", "Omar", "+212 6 61 23 45 67", "omar.elamrani@example.com",
            latitude = 33.5731, longitude = -7.5898),         // Casablanca
        Contact(6, "Dubois", "Hélène", "07 11 22 33 44", "helene.dubois@example.com"),   // sans position
        Contact(7, "Rossi", "Marco", "+39 333 123 4567", "marco.rossi@example.com",
            latitude = 41.9028, longitude = 12.4964),         // Rome
        Contact(8, "Nguyen", "Linh", "06 45 67 89 01", "linh.nguyen@example.com")       // sans position
    )
}
