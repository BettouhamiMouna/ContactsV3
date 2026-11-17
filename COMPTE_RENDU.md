---
title: "TP Android — Gestion des contacts V3"
subtitle: "Compte rendu — Fragments, RecyclerView, filtrage, Google Maps"
author: "Nom Prénom — Groupe"
---

# 1. Objectif

Faire évoluer l'application de contacts V2 vers une architecture **mono-activité** : une
`MainActivity` héberge trois fragments dynamiques (liste, détail, carte). La liste devient une
`RecyclerView` filtrable en temps réel, et chaque contact peut posséder une position GPS affichée
sur Google Maps. Toutes les fonctionnalités de la V2 sont conservées (modification, Appeler,
E-mail, Galerie, Caméra, Annuler, validation).

Dépôt GitHub : `https://github.com/<compte>/ContactsV3`

# 2. Architecture des fragments

| Élément | Rôle |
|---|---|
| `MainActivity` | Seule activité. Contient un `FragmentContainerView` et implémente l'interface `Navigateur` (ouvrirDetail, ouvrirCarte, retour). |
| `ListeContactsFragment` | Champ de recherche + `RecyclerView`, message « aucun résultat », compteur. |
| `DetailContactFragment` | Formulaire (nom, prénom, téléphone, e-mail, latitude, longitude), photo, boutons Appeler / E-mail / Galerie / Caméra / Voir sur la carte / Annuler / Enregistrer. |
| `CarteContactFragment` | `SupportMapFragment` enfant : marqueur au nom du contact, caméra centrée ; message explicite si position absente ou invalide. |
| `ContactsViewModel` | ViewModel partagé (portée activité) : liste en mémoire, filtre courant, liste filtrée. |
| `model/` | `Contact`, `ContactsDemo`, `ContactFilter` (filtrage), `ContactValidator` (validation). |

**Navigation.** Chaque changement d'écran est une `FragmentTransaction` :
`setReorderingAllowed(true)`, `replace(R.id.fragment_container, fragment, tag)` puis
`addToBackStack(tag)`. Le bouton Retour système et la flèche de la barre d'action
(`onSupportNavigateUp`) dépilent la transaction : Carte → Détail → Liste. Les boutons Annuler
et Enregistrer appellent aussi `popBackStack()`.

**Passage de paramètres.** `DetailContactFragment.newInstance(id)` et
`CarteContactFragment.newInstance(id)` placent **uniquement l'identifiant** du contact dans le
`Bundle` des arguments (`bundleOf("contact_id" to id)`). Le fragment relit ensuite le contact dans
le ViewModel par cet identifiant.

**Rotation / recréation.** Dans `MainActivity.onCreate`, le fragment liste n'est ajouté que si
`savedInstanceState == null` ; ensuite c'est le `FragmentManager` qui restaure lui-même les
fragments et la pile de retour, donc aucun fragment n'est empilé en double. Les champs de saisie
restaurent leur texte automatiquement ; la photo choisie et le fichier caméra en attente sont
sauvegardés dans `onSaveInstanceState`. Un anti-double-clic (400 ms) et la vérification
`isStateSaved` évitent d'ouvrir deux fois le même écran.

# 3. Choix de communication : ViewModel partagé

Deux solutions étaient proposées : ViewModel partagé ou Fragment Result API. J'ai choisi le
**ViewModel partagé** (`by activityViewModels()`), pour quatre raisons :

- La liste des contacts est une **donnée partagée par les trois écrans**, pas un résultat ponctuel.
  Le ViewModel en est la source unique de vérité.
- Après `Enregistrer`, le détail appelle `viewModel.mettreAJour(contact)`. La liste observe
  `contactsFiltres` (LiveData) : elle se met à jour **automatiquement** au retour, sans code de
  rafraîchissement.
- Le ViewModel **survit à la rotation**. Les modifications enregistrées et le filtre en cours ne
  sont donc pas perdus.
- La Fragment Result API ne transporte que des `Bundle` à usage unique. Il aurait fallu
  sérialiser le contact, puis le fusionner à la main dans la liste.

Les modifications non validées restent dans le formulaire. Elles n'atteignent le ViewModel
qu'au moment d'`Enregistrer`, donc `Annuler` (ou Retour) conserve les anciennes valeurs.

# 4. RecyclerView

- Ligne `item_contact.xml` (MaterialCardView) : photo ronde (Glide, image par défaut si absente),
  prénom + nom, téléphone et une icône si le contact a une position GPS.
- `ContactAdapter` hérite de `ListAdapter<Contact, ContactViewHolder>` avec un
  `DiffUtil.ItemCallback` (`areItemsTheSame` compare les `id`, `areContentsTheSame` compare
  tout le contenu). `submitList()` calcule les différences et n'anime que les lignes concernées
  (filtrage, modification).
- Le clic est géré dans le `ViewHolder`. On récupère `getItem(bindingAdapterPosition)` dans la
  liste **actuellement affichée**, puis on transmet `contact.id` à `ouvrirDetail`.
- L'affichage dépend uniquement du modèle `Contact` : aucune donnée n'est figée dans les vues.

# 5. Stratégie de filtrage

- Un `TextInputEditText` placé au-dessus de la liste, avec `doAfterTextChanged`, appelle
  `viewModel.definirFiltre(texte)` à chaque frappe. Il n'y a pas de bouton de validation.
- `contactsFiltres` est un `MediatorLiveData`. Il est recalculé quand **la liste ou le filtre**
  change, ce qui garde la liste filtrée à jour même après une modification.
- `ContactFilter.filtrer()` (fonction pure, testée unitairement) :
    - normalise le texte (minuscules + suppression des accents) : **insensible à la casse**, et
      « helene » trouve « Hélène » ;
    - cherche dans le nom, le prénom, « prénom nom », « nom prénom » et le téléphone ;
    - si la saisie ressemble à un numéro, compare les **chiffres seuls** : « 0698 » trouve
      « 06 98 76 54 32 ».
- Une saisie vide (icône « effacer » du champ) réaffiche tous les contacts. Une liste vide
  affiche « Aucun contact ne correspond à « … » ».

# 6. Coordonnées GPS et Google Maps

**Modèle.** `Contact` comporte `latitude: Double?` et `longitude: Double?`. Six contacts de
démonstration ont une position valide (Paris, Lyon, Alger, Tunis, Casablanca, Rome) et deux n'en
ont pas.

**Saisie et validation** (`ContactValidator`) :

- les deux champs vides : aucune position (valeurs `null`) ;
- un seul champ rempli : erreur « Renseigner latitude ET longitude » ;
- valeur non numérique, `NaN` ou infini : erreur « Valeur numérique attendue » (la virgule
  décimale est acceptée) ;
- latitude hors de [−90 ; 90] ou longitude hors de [−180 ; 180] : message dédié, affiché sous
  le champ (`TextInputLayout.error`).

**Carte.** Le bouton « Voir sur la carte » ouvre `CarteContactFragment` avec l'identifiant du
contact. Si la position est valide, `getMapAsync` puis, dans `onMapReady`, `addMarker`
(titre = nom complet, sous-titre = téléphone, info-bulle ouverte) et
`moveCamera(newLatLngZoom(position, 14f))`. Sinon, la carte est masquée et un message explicite
s'affiche, avec un bouton « Retour à la fiche ». Si les coordonnées du formulaire ne sont pas
encore enregistrées, un Toast prévient que la carte affiche la position enregistrée. Au retour,
le détail retrouve la saisie en cours.

Aucune permission de localisation n'est demandée : on affiche seulement des coordonnées déjà
enregistrées.

**Protection de la clé API.**

- On utilise le **Secrets Gradle Plugin**. La vraie clé se trouve dans `secrets.properties`,
  à la racine du projet, et ce fichier est **listé dans `.gitignore`**. Le manifeste ne
  contient que le placeholder `${MAPS_API_KEY}`.
- `local.defaults.properties` (versionné) contient une valeur factice. Le projet compile donc
  chez l'enseignant même sans clé.
- Dans Google Cloud, la clé est restreinte au package `com.example.contactsv3` et à son
  empreinte SHA-1, et seule l'API *Maps SDK for Android* est autorisée.

# 7. Fonctionnalités V2 conservées

- **Appeler** : `Intent(ACTION_DIAL, tel:…)` ouvre le composeur, sans permission d'appel.
- **E-mail** : `Intent(ACTION_SENDTO, mailto:)` avec `EXTRA_EMAIL` prérempli.
- **Galerie** : `ActivityResultContracts.PickVisualMedia` (sélecteur de photos).
- **Caméra** : `ActivityResultContracts.TakePicture`, avec un fichier unique exposé par un
  `FileProvider`. Aucune permission CAMERA n'est nécessaire.
- **Robustesse** : chaque `startActivity` / `launch` est protégé par un `try/catch
  ActivityNotFoundException`, avec le message « Aucune application … disponible ». Si
  l'utilisateur annule la galerie ou la caméra (résultat `null` / `false`), un Toast s'affiche
  et l'ancienne photo est conservée.
- **Validation** : nom, prénom et téléphone obligatoires ; format du téléphone et de l'e-mail
  vérifiés ; l'e-mail est facultatif.

# 8. Tests réalisés

Tests unitaires JVM (`./gradlew test`) : `ContactFilterTest` (nom, prénom, casse, accents,
téléphone avec ou sans espaces, aucun résultat, filtre vide) et `ContactValidatorTest` (bornes
incluses, hors limites, non numérique, position incomplète, champs obligatoires, e-mail et
téléphone).

Tests manuels sur émulateur (cocher OK / KO après exécution) :

| N° | Scénario | Résultat attendu | OK ? |
|---|---|---|---|
| 1 | Taper « mart », « KARIM », « 0698 » | Le bon contact seul | |
| 2 | Taper « zzzz » | Message « Aucun contact ne correspond » | |
| 3 | Effacer le filtre | Les 8 contacts réapparaissent | |
| 4 | Filtrer « tunis… » / « Trabelsi », cliquer | Fiche de Sarra Trabelsi (pas le 1er contact) | |
| 5 | Modifier nom, téléphone, e-mail, photo, GPS → Enregistrer | Liste actualisée immédiatement | |
| 6 | Modifier puis Annuler | Anciennes valeurs conservées | |
| 7 | Nom vide / e-mail « abc » | Erreur sous le champ, pas d'enregistrement | |
| 8 | Latitude « abc », « 95 », longitude « -200 », un seul champ | Messages d'erreur adaptés | |
| 9 | Appeler / E-mail (avec et sans application) | Composeur / messagerie, ou message clair | |
| 10 | Galerie et Caméra, puis annulation | Photo mise à jour / « annulée », pas de plantage | |
| 11 | Voir sur la carte (Yasmine Benali) | Marqueur « Yasmine Benali » centré sur Paris | |
| 12 | Voir sur la carte (Hélène Dubois) | Message « pas de coordonnées » | |
| 13 | Retour depuis la carte puis le détail | Carte → Détail → Liste | |
| 14 | Rotation sur chaque écran (avec saisie en cours) | Saisie conservée, pas de doublon dans la pile | |

# 9. Captures d'écran

*(Insérer ici les captures réelles prises sur l'émulateur ou le téléphone.)*

1. Liste complète
2. Liste filtrée
3. Résultat vide
4. Détail d'un contact
5. Modification + erreurs de validation
6. Galerie
7. Caméra
8. Carte avec marqueur
9. Composeur (Appeler)
10. Application e-mail

# 10. Difficultés rencontrées

- **Clé Google Maps** : la création du projet Google Cloud et l'activation de l'API (avec
  facturation) sont nécessaires. Sans clé valide, la carte reste grise. Le Secrets Gradle Plugin
  évite de la publier.
- **Erreur d'indice après filtrage** : la position cliquée ne correspond plus à la liste
  complète. Elle est résolue en transmettant l'`id` du contact.
- **Conserver la saisie** lors de l'aller-retour vers la carte et lors de la rotation : brouillon
  conservé dans le fragment, état des vues et `onSaveInstanceState`.
- **Accès aux photos** : on utilise le sélecteur de photos et un `FileProvider` pour la caméra,
  afin d'éviter les permissions de stockage.
- *(Compléter avec vos propres difficultés.)*

# 11. Questions de compréhension

**1. Différence entre Activity et Fragment.** Une `Activity` est un composant de l'application
déclaré dans le manifeste. C'est un point d'entrée que le système lance, et elle possède sa
propre fenêtre. Un `Fragment` est une portion d'interface réutilisable, avec son propre cycle de
vie (et celui de sa vue). Il ne peut exister qu'**hébergé** dans une activité (ou un autre
fragment), et il est géré par un `FragmentManager`. Plusieurs fragments peuvent se succéder ou
cohabiter dans une même activité.

**2. Pourquoi `FragmentContainerView` et `addToBackStack()` ?** `FragmentContainerView` est le
conteneur recommandé pour les fragments. Il corrige les problèmes de `FrameLayout`
(ordre Z des animations de sortie, insets) et gère correctement l'ajout et la restauration des
fragments. `addToBackStack()` enregistre la transaction dans la pile de retour : le bouton
Retour annule le `replace()` et réaffiche l'écran précédent au lieu de quitter l'activité.

**3. Rôle de `RecyclerView.Adapter` et du `ViewHolder`.** L'Adapter fait le lien entre les
données et la liste. Il indique le nombre d'éléments, crée les vues de ligne
(`onCreateViewHolder`) et y place les données (`onBindViewHolder`). Le `ViewHolder` garde les
références aux vues d'une ligne. Les lignes sorties de l'écran sont **recyclées** pour en
afficher d'autres, sans nouvelle inflation ni `findViewById` : la liste reste fluide.

**4. Comment éviter une erreur d'indice après filtrage ?** Au clic, on lit l'élément à
`bindingAdapterPosition` **dans la liste actuellement soumise à l'Adapter** (la liste filtrée),
jamais dans la liste complète. On vérifie que la position n'est pas `NO_POSITION`, puis on
transmet l'**identifiant** du contact. `ListAdapter` + `DiffUtil` garantissent que la liste
affichée et celle de l'Adapter restent identiques.

**5. Pourquoi transmettre l'identifiant plutôt que la position ?** La position dépend de
l'affichage. Elle change avec le filtre, le tri, un ajout ou une suppression, et la même
position désigne un autre contact dans la liste complète. L'identifiant est **stable et
unique** : il désigne toujours le même contact. De plus, un `Bundle` doit rester léger : un
simple `Int` suffit, et le fragment relit des données à jour dans le ViewModel.

**6. Comment garantir que la clé Google Maps n'est pas exposée sur GitHub ?**

- Ne jamais écrire la clé dans le code, le manifeste ou `strings.xml`.
- La placer dans `secrets.properties` (ou `local.properties`), **listé dans `.gitignore`**, et
  l'injecter à la compilation avec le Secrets Gradle Plugin (`${MAPS_API_KEY}`).
- Versionner seulement un fichier de valeurs factices (`local.defaults.properties`).
- Vérifier avec `git status` avant chaque commit.
- Restreindre la clé dans Google Cloud (package + SHA-1, API autorisées). Si une clé a été
  publiée par erreur, la **révoquer et la régénérer** : la supprimer de l'historique Git ne
  suffit pas.
