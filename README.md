# TP Android — Gestion des contacts V3

Application Kotlin / XML : **une seule activité** (`MainActivity`) et trois fragments dynamiques
(`ListeContactsFragment`, `DetailContactFragment`, `CarteContactFragment`), RecyclerView filtrable
et affichage de la position GPS des contacts sur Google Maps.


## Clé Google Maps (jamais publiée)

1. Google Cloud Console → créer un projet → activer **Maps SDK for Android** → créer une clé API.
2. Restreindre la clé : *Applications Android*, nom de package `com.example.contactsv3`
   + empreinte SHA-1 (`./gradlew signingReport`), et API limitée à *Maps SDK for Android*.
3. Créer à la racine du projet le fichier **`secrets.properties`** (ignoré par Git) :

   ```properties
   MAPS_API_KEY=AIza...votre_cle...
   ```

Le **Secrets Gradle Plugin** injecte cette valeur dans le manifeste (`${MAPS_API_KEY}`).
Le fichier versionné `local.defaults.properties` ne contient qu'une valeur factice
(`DEFAULT_API_KEY`) pour que le projet compile chez l'enseignant sans clé.
Avant chaque push : `git status` ne doit jamais lister `secrets.properties`.

## Architecture

```
MainActivity (FragmentContainerView, implémente Navigateur)
 ├─ ListeContactsFragment   recherche + RecyclerView (ContactAdapter / ListAdapter + DiffUtil)
 ├─ DetailContactFragment   formulaire, validation, Appeler, E-mail, Galerie, Caméra, Voir sur la carte
 └─ CarteContactFragment    SupportMapFragment enfant, marqueur + centrage, messages d'erreur
ContactsViewModel (partagé, portée activité) : liste en mémoire + filtre → contactsFiltres
model/ : Contact, ContactsDemo, ContactFilter (filtrage), ContactValidator (validation)
```

Tests unitaires JVM : `./gradlew test` (filtrage et validation des coordonnées).
