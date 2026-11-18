package fr.exemple.biblio

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranAccueil(
    onOuvrirCategorie: (Categorie) -> Unit,
    onOuvrirEcran: (String) -> Unit
) {
    val contexte = LocalContext.current
    val base = remember { BiblioDatabase.get(contexte) }
    val scope = rememberCoroutineScope()

    val nbObjets by base.objetDao().nombre().collectAsState(initial = 0)
    val nbEmpruntes by base.pretDao().nombreEnCours(Sens.EMPRUNTE).collectAsState(initial = 0)
    val nbPretes by base.pretDao().nombreEnCours(Sens.PRETE).collectAsState(initial = 0)
    val categories by base.categorieDao().premierNiveau().collectAsState(initial = emptyList())

    var dialogueOuvert by remember { mutableStateOf(false) }
    var categorieASupprimer by remember { mutableStateOf<Categorie?>(null) }
    var fichierAImporter by remember { mutableStateOf<Uri?>(null) }

    val lanceurExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = ecrireSauvegarde(contexte, base, uri)
                Toast.makeText(
                    contexte,
                    if (ok) "Sauvegarde enregistrée" else "Échec de la sauvegarde",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val lanceurImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) fichierAImporter = uri
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ma bibliothèque", style = MaterialTheme.typography.headlineMedium)
                MenuTroisPoints()
            }
            Spacer(Modifier.height(12.dp))

            // Statistiques : trois cartes de même hauteur
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                CarteStatistique(
                    "Dans la bibliothèque", nbObjets,
                    Modifier.weight(1f).fillMaxHeight().clickable { onOuvrirEcran("BIBLIOTHEQUE") }
                )
                CarteStatistique(
                    "Empruntés", nbEmpruntes,
                    Modifier.weight(1f).fillMaxHeight().clickable { onOuvrirEcran("EMPRUNTE") }
                )
                CarteStatistique(
                    "Prêtés", nbPretes,
                    Modifier.weight(1f).fillMaxHeight().clickable { onOuvrirEcran("PRETE") }
                )
            }

            // Actions : deux lignes de trois tuiles de même taille
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TuileAction("Exporter", { lanceurExport.launch("biblio-sauvegarde.zip") }, Modifier.weight(1f))
                TuileAction(
                    "Importer",
                    { lanceurImport.launch(arrayOf("application/zip", "application/json")) },
                    Modifier.weight(1f)
                )
                TuileAction("Souhaits", { onOuvrirEcran("SOUHAITS") }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TuileAction("Recherche", { onOuvrirEcran("RECHERCHE") }, Modifier.weight(1f))
                TuileAction("Personnes", { onOuvrirEcran("PERSONNES") }, Modifier.weight(1f))
                TuileAction("Séries", { onOuvrirEcran("SERIES") }, Modifier.weight(1f))
            }

            // Liste des catégories, avec pastille = nombre d'objets de tout l'arbre
            TitreSection("Catégories", categories.size)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(categories) { categorie ->
                    LigneSousCategorie(
                        sous = categorie,
                        onClick = { onOuvrirCategorie(categorie) },
                        onLongClick = { categorieASupprimer = categorie }
                    )
                }
            }
        }
    }

    if (dialogueOuvert) {
        DialogueNouvelleCategorie(
            onConfirmer = { nom ->
                scope.launch { base.categorieDao().ajouter(Categorie(nom = nom)) }
                dialogueOuvert = false
            },
            onAnnuler = { dialogueOuvert = false }
        )
    }

    categorieASupprimer?.let { categorie ->
        AlertDialog(
            onDismissRequest = { categorieASupprimer = null },
            title = { Text("Supprimer la catégorie ?") },
            text = {
                Text(
                    "« ${categorie.nom} » sera supprimée, avec toutes ses sous-catégories. " +
                        "Les objets qu'elle contenait resteront dans ta bibliothèque, sans catégorie."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { base.categorieDao().supprimer(categorie) }
                    categorieASupprimer = null
                }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { categorieASupprimer = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    fichierAImporter?.let { uri ->
        AlertDialog(
            onDismissRequest = { fichierAImporter = null },
            title = { Text("Remplacer toute la bibliothèque ?") },
            text = {
                Text(
                    "L'import remplace toutes les données actuelles par celles du fichier. " +
                        "Cette action est irréversible, sauf si tu as une autre sauvegarde."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = importerSauvegarde(contexte, base, uri)
                        Toast.makeText(
                            contexte,
                            if (ok) "Import réussi" else "Échec de l'import : fichier invalide",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    fichierAImporter = null
                }) {
                    Text("Remplacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { fichierAImporter = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun CarteStatistique(titre: String, valeur: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = valeur.toString(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = titre,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DialogueNouvelleCategorie(onConfirmer: (String) -> Unit, onAnnuler: () -> Unit) {
    var nom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle catégorie") },
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