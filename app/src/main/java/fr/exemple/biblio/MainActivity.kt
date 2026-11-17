package fr.exemple.biblio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val contexte = LocalContext.current
                    val base = remember { BiblioDatabase.get(contexte) }

                    var pileIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
                    var ecranSpecial by rememberSaveable { mutableStateOf<String?>(null) }
                    var objetOuvert by remember { mutableStateOf<Objet?>(null) }

                    val idCourant = pileIds.lastOrNull()
                    val categorieCourante by produceState<Categorie?>(null, idCourant) {
                        value = idCourant?.let { base.categorieDao().parId(it) }
                    }

                    val objet = objetOuvert
                    val special = ecranSpecial
                    val courante = categorieCourante

                    when {
                        objet != null -> EcranDetailObjet(
                            objet = objet,
                            onRetour = { objetOuvert = null },
                            onOuvrirObjet = { objetOuvert = it }
                        )
                        special == "RECHERCHE" -> EcranRecherche(
                            onRetour = { ecranSpecial = null },
                            onOuvrirObjet = { objetOuvert = it }
                        )
                        special == "PERSONNES" -> EcranPersonnes(
                            onRetour = { ecranSpecial = null }
                        )
                        special == "SERIES" -> EcranSeries(
                            onRetour = { ecranSpecial = null },
                            onOuvrirObjet = { objetOuvert = it }
                        )
                        special == "BIBLIOTHEQUE" -> EcranBibliotheque(
                            onRetour = { ecranSpecial = null },
                            onOuvrirObjet = { objetOuvert = it }
                        )
                        special == "PRETE" -> EcranPrets(
                            sens = Sens.PRETE,
                            onRetour = { ecranSpecial = null }
                        )
                        special == "EMPRUNTE" -> EcranPrets(
                            sens = Sens.EMPRUNTE,
                            onRetour = { ecranSpecial = null }
                        )
                        special == "SOUHAITS" -> EcranSouhaits(
                            onRetour = { ecranSpecial = null }
                        )
                        pileIds.isEmpty() -> EcranAccueil(
                            onOuvrirCategorie = { pileIds = pileIds + it.id },
                            onOuvrirEcran = { ecranSpecial = it }
                        )
                        courante != null -> EcranDetailCategorie(
                            categorie = courante,
                            onRetour = { pileIds = pileIds.dropLast(1) },
                            onOuvrirSousCategorie = { pileIds = pileIds + it.id },
                            onOuvrirObjet = { objetOuvert = it }
                        )
                    }
                }
            }
        }
    }
}