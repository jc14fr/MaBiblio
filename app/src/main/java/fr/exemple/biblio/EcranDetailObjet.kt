package fr.exemple.biblio

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EcranDetailObjet(objet: Objet, onRetour: () -> Unit, onOuvrirObjet: (Objet) -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val scope = rememberCoroutineScope()
    val tous by base.objetDao().tous().collectAsState(initial = emptyList())

    var enEdition by rememberSaveable { mutableStateOf(false) }
    var titre by rememberSaveable { mutableStateOf(objet.titre) }
    var auteur by rememberSaveable { mutableStateOf(objet.auteur) }
    var isbn by rememberSaveable { mutableStateOf(objet.isbn ?: "") }
    var serie by rememberSaveable { mutableStateOf(objet.serie ?: "") }
    var tome by rememberSaveable { mutableStateOf(objet.numeroTome?.toString() ?: "") }
    var statutNom by rememberSaveable { mutableStateOf(objet.statut.name) }
    var confirmerSuppression by remember { mutableStateOf(false) }

    val image = remember(objet.couvertureUrl) {
        val chemin = objet.couvertureUrl
        if (chemin != null && File(chemin).exists()) {
            BitmapFactory.decodeFile(chemin)?.asImageBitmap()
        } else {
            null
        }
    }

    val nomSerie = serie.trim()
    val numero = tome.trim().toIntOrNull()
    // Le tome suivant : même série, numéro + 1, s'il est dans la bibliothèque
    val suivant: Objet? = if (nomSerie.isNotBlank() && numero != null) {
        tous.find { it.id != objet.id && it.serie?.trim() == nomSerie && it.numeroTome == numero + 1 }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onRetour) { Text("← Retour") }

        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Couverture de ${objet.titre}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(220.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!enEdition) {
            Text(titre, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            if (auteur.isNotBlank()) {
                Text("Auteur : $auteur", style = MaterialTheme.typography.bodyLarge)
            }
            if (isbn.isNotBlank()) {
                Text("ISBN : $isbn", style = MaterialTheme.typography.bodyLarge)
            }
            if (nomSerie.isNotBlank()) {
                Text(
                    "Série : $nomSerie" + (numero?.let { ", tome $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (nomSerie.isNotBlank() && numero != null) {
                if (suivant != null) {
                    TextButton(onClick = { onOuvrirObjet(suivant) }) {
                        Text("Tome suivant : ${suivant.titre}")
                    }
                } else {
                    Text(
                        "Tome suivant (n°${numero + 1}) : pas dans ta bibliothèque",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                if (statutNom == Statut.SOUHAITE.name) "Statut : souhait" else "Statut : possédé",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = { enEdition = true }) { Text("Modifier") }
                OutlinedButton(onClick = { confirmerSuppression = true }) { Text("Supprimer") }
            }
        } else {
            OutlinedTextField(
                value = titre,
                onValueChange = { titre = it },
                label = { Text("Titre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = auteur,
                onValueChange = { auteur = it },
                label = { Text("Auteur") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text("ISBN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = serie,
                onValueChange = { serie = it },
                label = { Text("Série (facultatif)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tome,
                onValueChange = { tome = it },
                label = { Text("Numéro de tome (facultatif)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                statutNom = if (statutNom == Statut.POSSEDE.name) {
                    Statut.SOUHAITE.name
                } else {
                    Statut.POSSEDE.name
                }
            }) {
                Text(
                    if (statutNom == Statut.POSSEDE.name) "Statut : possédé (toucher pour un souhait)"
                    else "Statut : souhait (toucher pour possédé)"
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    if (titre.isNotBlank()) {
                        scope.launch {
                            base.objetDao().modifier(
                                objet.copy(
                                    titre = titre.trim(),
                                    auteur = auteur.trim(),
                                    isbn = isbn.trim().ifBlank { null },
                                    serie = nomSerie.ifBlank { null },
                                    numeroTome = numero,
                                    statut = Statut.valueOf(statutNom)
                                )
                            )
                        }
                        enEdition = false
                    }
                }) {
                    Text("Enregistrer")
                }
                OutlinedButton(onClick = {
                    titre = objet.titre
                    auteur = objet.auteur
                    isbn = objet.isbn ?: ""
                    serie = objet.serie ?: ""
                    tome = objet.numeroTome?.toString() ?: ""
                    statutNom = objet.statut.name
                    enEdition = false
                }) {
                    Text("Annuler")
                }
            }
        }
    }

    if (confirmerSuppression) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text("Supprimer l'objet ?") },
            text = {
                Text("« $titre » sera supprimé. Ses prêts et emprunts éventuels seront aussi effacés.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { base.objetDao().supprimer(objet) }
                    confirmerSuppression = false
                    onRetour()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmerSuppression = false }) { Text("Annuler") }
            }
        )
    }
}