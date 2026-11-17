package com.example.contactsv3

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.contactsv3.ui.CarteContactFragment
import com.example.contactsv3.ui.DetailContactFragment
import com.example.contactsv3.ui.ListeContactsFragment

/**
 * Unique activité de l'application : elle héberge les fragments dans un
 * FragmentContainerView et centralise la navigation (replace + addToBackStack).
 */
class MainActivity : AppCompatActivity(), Navigateur {

    private var derniereNavigation = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Après une rotation, le FragmentManager restaure lui-même les fragments
        // et la pile de retour : on n'ajoute la liste qu'au tout premier lancement
        // pour ne pas empiler de fragments en double.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, ListeContactsFragment(), TAG_LISTE)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener { majBoutonHaut() }
        majBoutonHaut()
    }

    private fun majBoutonHaut() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        retour()
        return true
    }

    override fun ouvrirDetail(contactId: Int) =
        naviguer(DetailContactFragment.newInstance(contactId), TAG_DETAIL)

    override fun ouvrirCarte(contactId: Int) =
        naviguer(CarteContactFragment.newInstance(contactId), TAG_CARTE)

    override fun retour() {
        if (!supportFragmentManager.isStateSaved) supportFragmentManager.popBackStack()
    }

    private fun naviguer(fragment: Fragment, tag: String) {
        // Évite d'empiler deux fois le même écran lors d'un double-clic rapide
        // ou d'une transaction pendant la sauvegarde d'état.
        val maintenant = SystemClock.elapsedRealtime()
        if (maintenant - derniereNavigation < 400 || supportFragmentManager.isStateSaved) return
        derniereNavigation = maintenant

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(R.id.fragment_container, fragment, tag)
            addToBackStack(tag)
        }
    }

    companion object {
        const val TAG_LISTE = "liste"
        const val TAG_DETAIL = "detail"
        const val TAG_CARTE = "carte"
    }
}
