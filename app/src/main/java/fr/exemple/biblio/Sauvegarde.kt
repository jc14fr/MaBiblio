package fr.exemple.biblio

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// Remplace une valeur vide par « null » dans le JSON
private fun Any?.versJson(): Any = this ?: JSONObject.NULL

// Lit toute la base et renvoie un texte JSON
suspend fun construireSauvegarde(base: BiblioDatabase): String {
    val racine = JSONObject()
    racine.put("version", 2)

    val categories = JSONArray()
    base.categorieDao().lireTout().forEach { c ->
        categories.put(
            JSONObject()
                .put("id", c.id)
                .put("nom", c.nom)
                .put("parentId", c.parentId.versJson())
        )
    }
    racine.put("categories", categories)

    val objets = JSONArray()
    base.objetDao().lireTout().forEach { o ->
        objets.put(
            JSONObject()
                .put("id", o.id)
                .put("titre", o.titre)
                .put("auteur", o.auteur)
                .put("isbn", o.isbn.versJson())
                .put("categorieId", o.categorieId.versJson())
                .put("serie", o.serie.versJson())
                .put("numeroTome", o.numeroTome.versJson())
                .put("statut", o.statut.name)
                .put("couvertureUrl", o.couvertureUrl?.let { File(it).name }.versJson())
        )
    }
    racine.put("objets", objets)

    val personnes = JSONArray()
    base.personneDao().lireTout().forEach { p ->
        personnes.put(
            JSONObject()
                .put("id", p.id)
                .put("nom", p.nom)
        )
    }
    racine.put("personnes", personnes)

    val prets = JSONArray()
    base.pretDao().lireTout().forEach { p ->
        prets.put(
            JSONObject()
                .put("id", p.id)
                .put("objetId", p.objetId)
                .put("personneId", p.personneId)
                .put("sens", p.sens.name)
                .put("dateDebut", p.dateDebut)
                .put("dateRetour", p.dateRetour.versJson())
        )
    }
    racine.put("prets", prets)

    return racine.toString(2)
}