package com.example.contactsv3.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.contactsv3.ContactsViewModel
import com.example.contactsv3.Navigateur
import com.example.contactsv3.R
import com.example.contactsv3.databinding.FragmentDetailContactBinding
import com.example.contactsv3.model.Contact
import com.example.contactsv3.model.ContactValidator
import com.example.contactsv3.model.ContactValidator.Champ
import com.example.contactsv3.model.ContactValidator.Erreur
import com.google.android.material.textfield.TextInputLayout
import java.io.File

/**
 * Écran 2 : consultation / modification d'un contact et actions (appel, e-mail,
 * galerie, caméra, carte). Reçoit l'IDENTIFIANT du contact dans un Bundle.
 *
 * Les modifications restent dans le formulaire tant que l'utilisateur n'a pas
 * appuyé sur « Enregistrer » ; « Annuler » ou Retour les abandonnent.
 */
class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()

    private val contactId: Int get() = requireArguments().getInt(ARG_CONTACT_ID, -1)

    /** Photo choisie mais pas encore enregistrée (sauvegardée lors d'une rotation). */
    private var photoUriCourante: String? = null

    /** Fichier cible de la caméra, conservé si l'activité est recréée pendant la prise de vue. */
    private var uriCameraEnAttente: Uri? = null

    /** Brouillon du formulaire conservé quand on part vers la carte (vue détruite, fragment gardé). */
    private var brouillon: ContactValidator.Saisie? = null

    // --- Résultats d'activités (enregistrés avant onCreate, comme exigé par l'API) ---

    private val choisirPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                toast(R.string.msg_photo_annulee)
            } else {
                // Sur les anciens appareils (repli ACTION_OPEN_DOCUMENT), on conserve l'accès.
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) { /* non persistable : accès valable pour la session */ }
                photoUriCourante = uri.toString()
                afficherPhoto()
                toast(R.string.msg_photo_ok)
            }
        }

    private val prendrePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { reussi ->
            val uri = uriCameraEnAttente
            uriCameraEnAttente = null
            if (reussi && uri != null) {
                photoUriCourante = uri.toString()
                afficherPhoto()
                toast(R.string.msg_photo_ok)
            } else {
                toast(R.string.msg_camera_annulee)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contact = viewModel.getContact(contactId)
        if (savedInstanceState != null) {
            photoUriCourante = savedInstanceState.getString(KEY_PHOTO)
            uriCameraEnAttente = savedInstanceState.getString(KEY_CAMERA)?.toUri()
        } else {
            photoUriCourante = contact?.photoUri
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailContactBinding.bind(view)

        val contact = viewModel.getContact(contactId)
        if (contact == null) {
            toast(R.string.msg_contact_introuvable)
            view.post { (activity as? Navigateur)?.retour() }
            return
        }

        // Remplissage depuis le modèle (ou le brouillon au retour de la carte).
        // Après une rotation, Android restaure ensuite le texte saisi dans les champs.
        remplirFormulaire(brouillon ?: saisieDepuis(contact))
        brouillon = null
        afficherPhoto()

        binding.btnGalerie.setOnClickListener { ouvrirGalerie() }
        binding.btnCamera.setOnClickListener { ouvrirCamera() }
        binding.btnAppeler.setOnClickListener { appeler() }
        binding.btnEmail.setOnClickListener { envoyerEmail() }
        binding.btnCarte.setOnClickListener { voirSurLaCarte(contact.id) }
        binding.btnEnregistrer.setOnClickListener { enregistrer() }
        binding.btnAnnuler.setOnClickListener {
            toast(R.string.msg_annule)
            (requireActivity() as Navigateur).retour()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.titre_detail)
    }

    // --- Formulaire ---

    private fun saisieDepuis(c: Contact) = ContactValidator.Saisie(
        nom = c.nom,
        prenom = c.prenom,
        telephone = c.telephone,
        email = c.email,
        latitude = c.latitude?.toString().orEmpty(),
        longitude = c.longitude?.toString().orEmpty()
    )

    private fun lireFormulaire() = ContactValidator.Saisie(
        nom = binding.editNom.text?.toString().orEmpty(),
        prenom = binding.editPrenom.text?.toString().orEmpty(),
        telephone = binding.editTelephone.text?.toString().orEmpty(),
        email = binding.editEmail.text?.toString().orEmpty(),
        latitude = binding.editLatitude.text?.toString().orEmpty(),
        longitude = binding.editLongitude.text?.toString().orEmpty()
    )

    private fun remplirFormulaire(s: ContactValidator.Saisie) {
        binding.editNom.setText(s.nom)
        binding.editPrenom.setText(s.prenom)
        binding.editTelephone.setText(s.telephone)
        binding.editEmail.setText(s.email)
        binding.editLatitude.setText(s.latitude)
        binding.editLongitude.setText(s.longitude)
    }

    private fun afficherPhoto() {
        val b = _binding ?: return
        Glide.with(b.imagePhoto)
            .load(photoUriCourante?.toUri())
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .fallback(R.drawable.ic_person)
            .circleCrop()
            .into(b.imagePhoto)
    }

    private fun champ(c: Champ): TextInputLayout = when (c) {
        Champ.NOM -> binding.layoutNom
        Champ.PRENOM -> binding.layoutPrenom
        Champ.TELEPHONE -> binding.layoutTelephone
        Champ.EMAIL -> binding.layoutEmail
        Champ.LATITUDE -> binding.layoutLatitude
        Champ.LONGITUDE -> binding.layoutLongitude
    }

    private fun message(e: Erreur): String = getString(
        when (e) {
            Erreur.OBLIGATOIRE -> R.string.err_obligatoire
            Erreur.TELEPHONE_INVALIDE -> R.string.err_telephone
            Erreur.EMAIL_INVALIDE -> R.string.err_email
            Erreur.NON_NUMERIQUE -> R.string.err_non_numerique
            Erreur.LATITUDE_HORS_LIMITES -> R.string.err_latitude
            Erreur.LONGITUDE_HORS_LIMITES -> R.string.err_longitude
            Erreur.GPS_INCOMPLET -> R.string.err_gps_incomplet
        }
    )

    private fun enregistrer() {
        val contact = viewModel.getContact(contactId) ?: return
        val saisie = lireFormulaire()
        val resultat = ContactValidator.valider(saisie)

        Champ.entries.forEach { c -> champ(c).error = resultat.erreurs[c]?.let { message(it) } }
        if (!resultat.estValide) {
            resultat.erreurs.keys.firstOrNull()?.let { champ(it).editText?.requestFocus() }
            toast(R.string.err_formulaire)
            return
        }

        viewModel.mettreAJour(
            contact.copy(
                nom = saisie.nom.trim(),
                prenom = saisie.prenom.trim(),
                telephone = saisie.telephone.trim(),
                email = saisie.email.trim(),
                photoUri = photoUriCourante,
                latitude = resultat.latitude,
                longitude = resultat.longitude
            )
        )
        toast(R.string.msg_enregistre)
        (requireActivity() as Navigateur).retour()   // la liste observée se met à jour seule
    }

    // --- Actions ---

    private fun appeler() {
        val tel = binding.editTelephone.text?.toString()?.trim().orEmpty()
        if (tel.isEmpty()) { toast(R.string.msg_pas_telephone); return }
        lancer(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", tel, null)), R.string.msg_pas_app_tel)
    }

    private fun envoyerEmail() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty()) { toast(R.string.msg_pas_email); return }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        lancer(intent, R.string.msg_pas_app_mail)
    }

    private fun ouvrirGalerie() {
        try {
            choisirPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_pas_galerie)
        }
    }

    private fun ouvrirCamera() {
        val ctx = requireContext()
        val dossier = File(ctx.filesDir, "photos").apply { mkdirs() }
        // Nom unique : évite d'afficher une ancienne image mise en cache.
        val fichier = File(dossier, "contact_${contactId}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", fichier)
        uriCameraEnAttente = uri
        try {
            prendrePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            uriCameraEnAttente = null
            toast(R.string.msg_pas_app_camera)
        }
    }

    private fun voirSurLaCarte(id: Int) {
        val enregistre = viewModel.getContact(id)
        val saisie = lireFormulaire()
        val latSaisie = ContactValidator.lireCoordonnee(saisie.latitude)
        val lonSaisie = ContactValidator.lireCoordonnee(saisie.longitude)
        if (latSaisie != enregistre?.latitude || lonSaisie != enregistre?.longitude) {
            toast(R.string.msg_carte_modifs)
        }
        (requireActivity() as Navigateur).ouvrirCarte(id)
    }

    private fun lancer(intent: Intent, messageIndisponible: Int) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(messageIndisponible)
        }
    }

    private fun toast(res: Int) {
        context?.let { Toast.makeText(it, res, Toast.LENGTH_SHORT).show() }
    }

    // --- Cycle de vie ---

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHOTO, photoUriCourante)
        outState.putString(KEY_CAMERA, uriCameraEnAttente?.toString())
    }

    override fun onDestroyView() {
        // Conserve la saisie si le fragment reste dans la pile (ex. : ouverture de la carte).
        _binding?.let { if (viewModel.getContact(contactId) != null) brouillon = lireFormulaire() }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CONTACT_ID = "contact_id"
        private const val KEY_PHOTO = "photo_uri_courante"
        private const val KEY_CAMERA = "uri_camera_en_attente"

        /** Passage de paramètre : seul l'identifiant du contact est transmis dans le Bundle. */
        fun newInstance(contactId: Int) = DetailContactFragment().apply {
            arguments = bundleOf(ARG_CONTACT_ID to contactId)
        }
    }
}
