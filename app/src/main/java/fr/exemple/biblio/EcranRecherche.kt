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
fun EcranRecherche(onRetour: () -> Unit, onOuvrirObjet: (Objet) -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val objets by base.objetDao().tous().collectAsState(initial = emptyList())
    var texte by rememberSaveable { mutableStateOf("") }

    val resultats = if (texte.isBlank()) {
        emptyList()
    } else {
        objets.filter {
            it.titre.contains(texte, ignoreCase = true) ||
                it.auteur.contains(texte, ignoreCase = true) ||
                (it.serie?.contains(texte, ignoreCase = true) == true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onRetour) { Text("← Retour") }
        Text("Recherche", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = texte,
            onValueChange = { texte = it },
            label = { Text("Titre, auteur ou série") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(resultats) { o ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOuvrirObjet(o) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(o.titre, style = MaterialTheme.typography.bodyLarge)
                    if (o.auteur.isNotBlank()) {
                        Text(o.auteur, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}