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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EcranPersonnes(onRetour: () -> Unit) {
    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val personnes by base.personneDao().toutes().collectAsState(initial = emptyList())
    var personneId by rememberSaveable { mutableStateOf(0L) }
    val choisie = personnes.find { it.id == personneId }

    if (choisie != null) {
        EcranPersonneDetail(personne = choisie, onRetour = { personneId = 0L })
    } else {
        BackHandler(onBack = onRetour)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onRetour) { Text("← Retour") }
            Text("Personnes", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            if (personnes.isEmpty()) {
                Text(
                    "Aucune personne pour l'instant. Elles sont créées lors d'un prêt ou d'un emprunt.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LazyColumn {
                items(personnes) { p ->
                    Text(
                        text = p.nom,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { personneId = p.id }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EcranPersonneDetail(personne: Personne, onRetour: () -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val prets by base.pretDao().pretsDePersonne(personne.id).collectAsState(initial = emptyList())
    val format = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onRetour) { Text("← Retour") }
        Text(personne.nom, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(prets) { p ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(p.titre, style = MaterialTheme.typography.bodyLarge)
                    val type = if (p.sens == Sens.PRETE) "Prêté" else "Emprunté"
                    val etat = p.dateRetour?.let { " · rendu le ${format.format(Date(it))}" }
                        ?: " · en cours"
                    Text(
                        "$type depuis le ${format.format(Date(p.dateDebut))}$etat",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}