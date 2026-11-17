package fr.exemple.biblio

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun EcranSouhaits(onRetour: () -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val scope = rememberCoroutineScope()
    val souhaits by base.objetDao().souhaits().collectAsState(initial = emptyList())

    // Formulaire : sauvegardé pour survivre au passage par le scanner
    var dialogueOuvert by rememberSaveable { mutableStateOf(false) }
    var sTitre by rememberSaveable { mutableStateOf("") }
    var sAuteur by rememberSaveable { mutableStateOf("") }
    var sIsbn by rememberSaveable { mutableStateOf("") }
    var sUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var sMessage by rememberSaveable { mutableStateOf("") }
    var messageScan by rememberSaveable { mutableStateOf("") }

    fun reinitialiser() {
        dialogueOuvert = false
        sTitre = ""
        sAuteur = ""
        sIsbn = ""
        sUrl = null
        sMessage = ""
        messageScan = ""
    }

    // Cherche titre, auteur et pochette à partir de l'ISBN
    fun lancerRecherche(code: String) {
        if (code.isBlank()) return
        scope.launch {
            sMessage = "Recherche en cours..."
            val infos = chercherParIsbn(code) ?: chercherBnf(code)
            if (infos != null) {
                if (sTitre.isBlank()) sTitre = infos.titre
                if (sAuteur.isBlank()) sAuteur = infos.auteur
                if (infos.urlCouverture != null) sUrl = infos.urlCouverture
                sMessage = "Infos trouvées (Open Library ou BnF)"
            } else {
                sMessage = "Aucune info trouvée, saisis le titre à la main"
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            sIsbn = result.contents
            messageScan = ""
            lancerRecherche(result.contents)
        } else {
            messageScan = "Scan annulé"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (accordee) {
            scanLauncher.launch(
                ScanOptions()
                    .setPrompt("Place le code-barres dans le cadre")
                    .setBeepEnabled(true)
            )
        } else {
            messageScan = "Permission caméra refusée"
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogueOuvert = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            TextButton(onClick = onRetour) {
                Text("← Retour")
            }
            Text("Liste de souhaits", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(souhaits) { o ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(o.titre, style = MaterialTheme.typography.bodyLarge)
                        if (o.auteur.isNotBlank()) {
                            Text(o.auteur, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            scope.launch { base.objetDao().modifier(o.copy(statut = Statut.POSSEDE)) }
                        }) {
                            Text("Je l'ai acheté")
                        }
                    }
                }
            }
        }
    }

    if (dialogueOuvert) {
        AlertDialog(
            onDismissRequest = { reinitialiser() },
            title = { Text("Nouveau souhait") },
            text = {
                Column {
                    OutlinedTextField(
                        value = sTitre,
                        onValueChange = { sTitre = it },
                        label = { Text("Titre") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sAuteur,
                        onValueChange = { sAuteur = it },
                        label = { Text("Auteur (facultatif)") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sIsbn,
                        onValueChange = { sIsbn = it },
                        label = { Text("ISBN (facultatif)") },
                        singleLine = true
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Scanner le code-barres")
                    }
                    TextButton(onClick = { lancerRecherche(sIsbn.trim()) }) {
                        Text("Chercher avec l'ISBN")
                    }
                    if (sUrl != null) {
                        Text("Pochette trouvée en ligne", style = MaterialTheme.typography.bodySmall)
                    }
                    if (sMessage.isNotBlank()) {
                        Text(sMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    if (messageScan.isNotBlank()) {
                        Text(messageScan, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (sTitre.isNotBlank()) {
                        // Valeurs copiées avant la réinitialisation du formulaire
                        val t = sTitre.trim()
                        val a = sAuteur.trim()
                        val i = sIsbn.trim().ifBlank { null }
                        val u = sUrl
                        scope.launch {
                            val pochette = u?.let { telechargerPochette(contexte, it) }
                            base.objetDao().ajouter(
                                Objet(
                                    titre = t,
                                    auteur = a,
                                    isbn = i,
                                    statut = Statut.SOUHAITE,
                                    couvertureUrl = pochette
                                )
                            )
                        }
                        reinitialiser()
                    }
                }) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { reinitialiser() }) { Text("Annuler") }
            }
        )
    }
}