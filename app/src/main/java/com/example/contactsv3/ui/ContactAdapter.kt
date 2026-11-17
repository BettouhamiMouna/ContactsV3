package com.example.contactsv3.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactsv3.R
import com.example.contactsv3.databinding.ItemContactBinding
import com.example.contactsv3.model.Contact

/**
 * Adapter de la RecyclerView. ListAdapter + DiffUtil calculent les différences
 * entre l'ancienne et la nouvelle liste (filtrage, modification) et n'animent
 * que les lignes concernées.
 */
class ContactAdapter(
    private val auClic: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    /** Le ViewHolder garde les références aux vues d'une ligne (pas de findViewById répétés). */
    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                // On relit l'élément à la position COURANTE de la liste affichée (filtrée),
                // puis on transmet son identifiant : aucune erreur d'indice possible.
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) auClic(getItem(position))
            }
        }

        fun lier(contact: Contact) {
            binding.textNomComplet.text = contact.nomComplet
            binding.textTelephone.text = contact.telephone
            binding.imageGps.isVisible = contact.aDesCoordonnees
            Glide.with(binding.imagePhoto)
                .load(contact.photoUri?.toUri())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .fallback(R.drawable.ic_person)
                .circleCrop()
                .into(binding.imagePhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder =
        ContactViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) =
        holder.lier(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
