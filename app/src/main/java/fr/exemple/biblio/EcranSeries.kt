package fr.exemple.biblio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EcranSeries(onRetour: () -> Unit, onOuvrirObjet: (Objet) -> Unit) {
    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val tous by base.objetDao().tous().collectAsState(initial = emptyList())
    var serieChoisie by rememberSaveable { mutableStateOf("") }

    BackHandler(onBack = {
        if (serieChoisie.isNotBlank()) serieChoisie = "" else onRetour()
    })

    // Objets groupés par nom de série, sans les objets sans série
    val series: Map<String, List<Objet>> = tous
        .groupBy { it.serie?.trim().orEmpty() }
        .filterKeys { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = {
            if (serieChoisie.isNotBlank()) serieChoisie = "" else onRetour()
        }) {
            Text("← Retour")
        }

        if (serieChoisie.isBlank()) {
            Text("Séries", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            if (series.isEmpty()) {
                Text(
                    "Aucune série. Renseigne la série et le numéro de tome dans la fiche d'un objet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LazyColumn {
                items(series.keys.toList().sorted()) { nom ->
                    Text(
                        text = "$nom (${series[nom]?.size ?: 0})",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { serieChoisie = nom }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        } else {
            val livres = series[serieChoisie].orEmpty()
                .sortedBy { it.numeroTome ?: Int.MAX_VALUE }
            val possedes = livres.mapNotNull { it.numeroTome }.toSet()
            val maximum = possedes.maxOrNull() ?: 0
            val manquants = (1..maximum).filter { it !in possedes }

            Text(serieChoisie, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(livres) { o ->
                    val prefixe = o.numeroTome?.let { "Tome $it : " } ?: ""
                    Text(
                        text = prefixe + o.titre,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOuvrirObjet(o) }
                            .padding(vertical = 12.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    if (manquants.isNotEmpty()) {
                        Text(
                            "Tomes manquants : " + manquants.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        "Tome suivant à acquérir : ${maximum + 1}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}