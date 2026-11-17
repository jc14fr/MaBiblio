package fr.exemple.biblio

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EcranBibliotheque(onRetour: () -> Unit, onOuvrirObjet: (Objet) -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val prefs = remember { contexte.getSharedPreferences("affichage", Context.MODE_PRIVATE) }
    val objets by base.objetDao().tous().collectAsState(initial = emptyList())
    val possedes = objets.filter { it.statut == Statut.POSSEDE }

    // Choix mémorisé dans les préférences : il survit au redémarrage de l'appli
    var enGrille by remember { mutableStateOf(prefs.getBoolean("grille", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onRetour) {
                Text("← Retour")
            }
            TextButton(onClick = {
                enGrille = !enGrille
                prefs.edit().putBoolean("grille", enGrille).apply()
            }) {
                Text(if (enGrille) "Liste" else "Grille")
            }
        }
        Text("Dans la bibliothèque", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (enGrille) {
            GrilleObjets(objets = possedes, onOuvrir = onOuvrirObjet)
        } else {
            LazyColumn {
                items(possedes) { o ->
                    LigneObjet(objet = o, onClick = { onOuvrirObjet(o) })
                }
            }
        }
    }
}