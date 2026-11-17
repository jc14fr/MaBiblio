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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcranPrets(sens: Sens, onRetour: () -> Unit) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val scope = rememberCoroutineScope()
    val prets by base.pretDao().pretsAvecDetails(sens).collectAsState(initial = emptyList())
    val objets by base.objetDao().tous().collectAsState(initial = emptyList())
    val personnes by base.personneDao().toutes().collectAsState(initial = emptyList())
    val format = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    // Tout l'état du formulaire est sauvegardé, pour survivre au passage par le scanner
    var dialogueOuvert by rememberSaveable { mutableStateOf(false) }
    var recherche by rememberSaveable { mutableStateOf("") }
    var objetChoisiId by rememberSaveable { mutableStateOf(0L) }
    var titreExterne by rememberSaveable { mutableStateOf("") }
    var isbnExterne by rememberSaveable { mutableStateOf("") }
    var nomPersonne by rememberSaveable { mutableStateOf("") }
    var messageRecherche by rememberSaveable { mutableStateOf("") }
    var isbnScanne by rememberSaveable { mutableStateOf("") }
    var messageScan by rememberSaveable { mutableStateOf("") }

    fun reinitialiser() {
        dialogueOuvert = false
        recherche = ""
        objetChoisiId = 0L
        titreExterne = ""
        isbnExterne = ""
        nomPersonne = ""
        messageRecherche = ""
        isbnScanne = ""
        messageScan = ""
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            isbnScanne = result.contents
            messageScan = ""
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

    // Traitement du scan : relancé après recréation de l'activité tant que isbnScanne est rempli
    LaunchedEffect(isbnScanne, objets.size) {
        val code = isbnScanne
        if (code.isBlank()) return@LaunchedEffect
        if (sens == Sens.PRETE) {
            val trouve = objets.find { it.statut == Statut.POSSEDE && it.isbn == code }
            objetChoisiId = trouve?.id ?: 0L
            messageRecherche = if (trouve == null) "Ce livre n'est pas dans ta bibliothèque" else ""
        } else {
            isbnExterne = code
            messageRecherche = "Recherche en cours..."
            val infos = chercherParIsbn(code) ?: chercherBnf(code)
            if (infos != null) {
                if (titreExterne.isBlank()) titreExterne = infos.titre
                messageRecherche = "Titre trouvé"
            } else {
                messageRecherche = "Aucune info trouvée, saisis le titre"
            }
        }
    }

    fun enregistrer(nom: String) {
        // Valeurs copiées avant la réinitialisation du formulaire
        val titre = titreExterne.trim()
        val isbn = isbnExterne.trim().ifBlank { null }
        val idChoisi = objetChoisiId
        scope.launch {
            val idObjet = if (sens == Sens.PRETE) {
                idChoisi
            } else {
                base.objetDao().ajouter(
                    Objet(titre = titre, isbn = isbn, statut = Statut.EXTERIEUR)
                )
            }
            val idPersonne = base.personneDao().lireTout().find { it.nom == nom }?.id
                ?: base.personneDao().ajouter(Personne(nom = nom))
            base.pretDao().ajouter(
                Pret(
                    objetId = idObjet,
                    personneId = idPersonne,
                    sens = sens,
                    dateDebut = System.currentTimeMillis()
                )
            )
        }
        reinitialiser()
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
            Text(
                if (sens == Sens.PRETE) "Objets prêtés" else "Objets empruntés",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(prets) { p ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${p.titre} — ${p.nom}", style = MaterialTheme.typography.bodyLarge)
                        val rendu = p.dateRetour?.let { " · rendu le ${format.format(Date(it))}" } ?: ""
                        Text(
                            "Depuis le ${format.format(Date(p.dateDebut))}$rendu",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (p.dateRetour == null) {
                            TextButton(onClick = {
                                scope.launch {
                                    base.pretDao().marquerRendu(p.id, System.currentTimeMillis())
                                }
                            }) {
                                Text("Marquer comme rendu")
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogueOuvert) {
        val objetsPossedes = objets.filter { it.statut == Statut.POSSEDE }
        val choisi = objetsPossedes.find { it.id == objetChoisiId }
        val suggestionsLivres = if (recherche.isBlank()) {
            emptyList()
        } else {
            objetsPossedes.filter { it.titre.contains(recherche, ignoreCase = true) }.take(8)
        }
        val suggestionsPersonnes = if (nomPersonne.isBlank()) {
            emptyList()
        } else {
            personnes
                .filter { it.nom.contains(nomPersonne, ignoreCase = true) && it.nom != nomPersonne }
                .take(5)
        }

        AlertDialog(
            onDismissRequest = { reinitialiser() },
            title = { Text(if (sens == Sens.PRETE) "Nouveau prêt" else "Nouvel emprunt") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (sens == Sens.PRETE) {
                        if (choisi != null) {
                            Text("Livre prêté : ${choisi.titre}", style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = { objetChoisiId = 0L }) {
                                Text("Changer de livre")
                            }
                        } else {
                            OutlinedTextField(
                                value = recherche,
                                onValueChange = { recherche = it },
                                label = { Text("Rechercher un livre") },
                                singleLine = true
                            )
                            suggestionsLivres.forEach { o ->
                                TextButton(onClick = {
                                    objetChoisiId = o.id
                                    recherche = ""
                                }) {
                                    Text(o.titre)
                                }
                            }
                        }
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Scanner l'ISBN")
                        }
                    } else {
                        OutlinedTextField(
                            value = titreExterne,
                            onValueChange = { titreExterne = it },
                            label = { Text("Titre de l'objet emprunté") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = isbnExterne,
                            onValueChange = { isbnExterne = it },
                            label = { Text("ISBN (facultatif)") },
                            singleLine = true
                        )
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Scanner l'ISBN")
                        }
                        TextButton(onClick = {
                            val code = isbnExterne.trim()
                            if (code.isNotBlank()) isbnScanne = code
                        }) {
                            Text("Chercher avec l'ISBN")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nomPersonne,
                        onValueChange = { nomPersonne = it },
                        label = { Text("Personne") },
                        singleLine = true
                    )
                    suggestionsPersonnes.forEach { p ->
                        TextButton(onClick = { nomPersonne = p.nom }) {
                            Text(p.nom)
                        }
                    }

                    if (messageRecherche.isNotBlank()) {
                        Text(messageRecherche, style = MaterialTheme.typography.bodySmall)
                    }
                    if (messageScan.isNotBlank()) {
                        Text(messageScan, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nom = nomPersonne.trim()
                    val objetOk = if (sens == Sens.PRETE) objetChoisiId != 0L else titreExterne.isNotBlank()
                    if (nom.isNotBlank() && objetOk) enregistrer(nom)
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { reinitialiser() }) { Text("Annuler") }
            }
        )
    }
}