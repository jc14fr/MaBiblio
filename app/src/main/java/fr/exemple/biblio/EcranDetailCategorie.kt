package fr.exemple.biblio

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranDetailCategorie(
    categorie: Categorie,
    onRetour: () -> Unit,
    onOuvrirSousCategorie: (Categorie) -> Unit,
    onOuvrirObjet: (Objet) -> Unit
) {
    BackHandler(onBack = onRetour)

    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val scope = rememberCoroutineScope()
    val prefs = remember { contexte.getSharedPreferences("affichage", Context.MODE_PRIVATE) }

    val sousCategories by base.categorieDao()
        .sousCategories(categorie.id)
        .collectAsState(initial = emptyList())

    val objets by base.objetDao()
        .dansCategorie(categorie.id)
        .collectAsState(initial = emptyList())

    var enGrille by remember { mutableStateOf(prefs.getBoolean("grille", false)) }

    var dialogueSousCategorie by remember { mutableStateOf(false) }
    var dialogueObjet by rememberSaveable { mutableStateOf(false) }
    var objetASupprimer by remember { mutableStateOf<Objet?>(null) }
    var sousCategorieASupprimer by remember { mutableStateOf<Categorie?>(null) }

    // Formulaire « Nouvel objet » : tout est sauvegardé, pour survivre au scanner
    var oTitre by rememberSaveable { mutableStateOf("") }
    var oAuteur by rememberSaveable { mutableStateOf("") }
    var oIsbn by rememberSaveable { mutableStateOf("") }
    var oStatutNom by rememberSaveable { mutableStateOf(Statut.POSSEDE.name) }
    var oUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var oMessage by rememberSaveable { mutableStateOf("") }
    var messageScan by rememberSaveable { mutableStateOf("") }
    var cheminPhotoEnCours by rememberSaveable { mutableStateOf<String?>(null) }
    var photoPrise by rememberSaveable { mutableStateOf<String?>(null) }

    // Demande de recherche : incrémentée après un scan ou un appui sur « Chercher ».
    // « rechercheTraitee » reprend la valeur de la dernière recherche terminée.
    var demandeRecherche by rememberSaveable { mutableStateOf(0) }
    var rechercheTraitee by rememberSaveable { mutableStateOf(0) }

    fun reinitialiserObjet() {
        dialogueObjet = false
        oTitre = ""
        oAuteur = ""
        oIsbn = ""
        oStatutNom = Statut.POSSEDE.name
        oUrl = null
        oMessage = ""
        messageScan = ""
        cheminPhotoEnCours = null
        photoPrise = null
    }

    fun demanderRecherche() {
        if (oIsbn.isNotBlank()) demandeRecherche++
    }

    // La recherche est faite ici, dans la composition : elle reprend si l'écran est recréé
    LaunchedEffect(demandeRecherche) {
        if (demandeRecherche > rechercheTraitee) {
            val code = oIsbn.trim()
            oMessage = "Recherche en cours..."
            val infos = chercherParIsbn(code) ?: chercherBnf(code)
            if (infos != null) {
                if (oTitre.isBlank()) oTitre = infos.titre
                if (oAuteur.isBlank()) oAuteur = infos.auteur
                if (infos.urlCouverture != null) oUrl = infos.urlCouverture
                oMessage = "Infos trouvées (Open Library ou BnF)"
            } else {
                oMessage = "Aucune info trouvée, saisis le titre à la main"
            }
            rechercheTraitee = demandeRecherche
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            oIsbn = result.contents
            messageScan = ""
            demanderRecherche()
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

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            photoPrise = cheminPhotoEnCours
            messageScan = ""
        } else {
            messageScan = "Photo annulée"
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(onClick = { dialogueObjet = true }) {
                    Text("Objet")
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(onClick = { dialogueSousCategorie = true }) {
                    Text("+")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
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
            Text(categorie.nom, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (enGrille) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Sous-catégories", style = MaterialTheme.typography.titleMedium)
                    }
                    gridItems(sousCategories, span = { GridItemSpan(maxLineSpan) }) { sous ->
                        Text(
                            text = sous.nom,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOuvrirSousCategorie(sous) },
                                    onLongClick = { sousCategorieASupprimer = sous }
                                )
                                .padding(vertical = 12.dp)
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(16.dp))
                        Text("Objets", style = MaterialTheme.typography.titleMedium)
                    }
                    gridItems(objets) { objet ->
                        CaseObjet(objet = objet, onClick = { onOuvrirObjet(objet) })
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Sous-catégories", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(sousCategories) { sous ->
                        Text(
                            text = sous.nom,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOuvrirSousCategorie(sous) },
                                    onLongClick = { sousCategorieASupprimer = sous }
                                )
                                .padding(vertical = 12.dp)
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Objets", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(objets) { objet ->
                        LigneObjet(
                            objet = objet,
                            onClick = { onOuvrirObjet(objet) }
                        )
                    }
                }
            }
        }
    }

    if (dialogueSousCategorie) {
        DialogueNouvelleSousCategorie(
            onConfirmer = { nom ->
                scope.launch {
                    base.categorieDao().ajouter(Categorie(nom = nom, parentId = categorie.id))
                }
                dialogueSousCategorie = false
            },
            onAnnuler = { dialogueSousCategorie = false }
        )
    }

    if (dialogueObjet) {
        DialogueNouvelObjet(
            titre = oTitre,
            onTitre = { oTitre = it },
            auteur = oAuteur,
            onAuteur = { oAuteur = it },
            isbn = oIsbn,
            onIsbn = { oIsbn = it },
            statut = Statut.valueOf(oStatutNom),
            onBasculerStatut = {
                oStatutNom = if (oStatutNom == Statut.POSSEDE.name) {
                    Statut.SOUHAITE.name
                } else {
                    Statut.POSSEDE.name
                }
            },
            message = oMessage,
            messageScan = messageScan,
            pochetteEnLigne = oUrl != null,
            photoEnregistree = photoPrise != null,
            onScanner = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onRechercher = { demanderRecherche() },
            onPrendrePhoto = {
                val (fichier, uri) = preparerFichierPhoto(contexte)
                cheminPhotoEnCours = fichier.absolutePath
                photoLauncher.launch(uri)
            },
            onEnregistrer = {
                if (oTitre.isNotBlank()) {
                    val t = oTitre.trim()
                    val a = oAuteur.trim()
                    val i = oIsbn.trim().ifBlank { null }
                    val s = Statut.valueOf(oStatutNom)
                    val u = oUrl
                    val p = photoPrise
                    scope.launch {
                        val pochette = p ?: u?.let { telechargerPochette(contexte, it) }
                        base.objetDao().ajouter(
                            Objet(
                                titre = t,
                                auteur = a,
                                isbn = i,
                                categorieId = categorie.id,
                                statut = s,
                                couvertureUrl = pochette
                            )
                        )
                    }
                    reinitialiserObjet()
                }
            },
            onAnnuler = { reinitialiserObjet() }
        )
    }

    sousCategorieASupprimer?.let { sous ->
        AlertDialog(
            onDismissRequest = { sousCategorieASupprimer = null },
            title = { Text("Supprimer la sous-catégorie ?") },
            text = {
                Text(
                    "« ${sous.nom} » sera supprimée, avec ses propres sous-catégories. " +
                        "Les objets qu'elle contenait resteront dans ta bibliothèque, sans catégorie."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { base.categorieDao().supprimer(sous) }
                    sousCategorieASupprimer = null
                }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { sousCategorieASupprimer = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    objetASupprimer?.let { objet ->
        AlertDialog(
            onDismissRequest = { objetASupprimer = null },
            title = { Text("Supprimer l'objet ?") },
            text = {
                Text(
                    "« ${objet.titre} » sera supprimé. " +
                        "Ses prêts et emprunts éventuels seront aussi effacés."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { base.objetDao().supprimer(objet) }
                    objetASupprimer = null
                }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { objetASupprimer = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun DialogueNouvelleSousCategorie(onConfirmer: (String) -> Unit, onAnnuler: () -> Unit) {
    var nom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle sous-catégorie") },
        text = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (nom.isNotBlank()) onConfirmer(nom.trim()) }) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// Boîte sans état propre : tout vient de l'écran, qui la garde en vie
@Composable
fun DialogueNouvelObjet(
    titre: String,
    onTitre: (String) -> Unit,
    auteur: String,
    onAuteur: (String) -> Unit,
    isbn: String,
    onIsbn: (String) -> Unit,
    statut: Statut,
    onBasculerStatut: () -> Unit,
    message: String,
    messageScan: String,
    pochetteEnLigne: Boolean,
    photoEnregistree: Boolean,
    onScanner: () -> Unit,
    onRechercher: () -> Unit,
    onPrendrePhoto: () -> Unit,
    onEnregistrer: () -> Unit,
    onAnnuler: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvel objet") },
        text = {
            Column {
                OutlinedTextField(
                    value = titre,
                    onValueChange = onTitre,
                    label = { Text("Titre") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = auteur,
                    onValueChange = onAuteur,
                    label = { Text("Auteur (facultatif)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = isbn,
                    onValueChange = onIsbn,
                    label = { Text("ISBN (facultatif)") },
                    singleLine = true
                )
                TextButton(onClick = onScanner) {
                    Text("Scanner le code-barres")
                }
                TextButton(onClick = onRechercher) {
                    Text("Chercher avec l'ISBN")
                }
                TextButton(onClick = onPrendrePhoto) {
                    Text("Photo de la couverture")
                }
                if (photoEnregistree) {
                    Text("Photo enregistrée", style = MaterialTheme.typography.bodySmall)
                } else if (pochetteEnLigne) {
                    Text("Pochette trouvée en ligne", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onBasculerStatut) {
                    Text(
                        if (statut == Statut.POSSEDE) "Statut : possédé (toucher pour un souhait)"
                        else "Statut : souhait (toucher pour possédé)"
                    )
                }
                if (message.isNotBlank()) {
                    Text(message, style = MaterialTheme.typography.bodySmall)
                }
                if (messageScan.isNotBlank()) {
                    Text(messageScan, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEnregistrer) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}