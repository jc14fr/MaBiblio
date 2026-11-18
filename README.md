# Ma Biblio

Application Android libre pour gérer sa collection personnelle de livres, BD, films, séries et autres objets, avec leurs catégories, leurs prêts et leurs souhaits. Les données restent sur le téléphone.

## Fonctionnalités

- Bibliothèque organisée par catégories et sous-catégories
- Objets représentés en texte avec possibilité de prendre une photo
- Suivi des objets empruntés et prêtés, avec les personnes concernées
- Liste de souhaits
- Recherche dans la collection par Titre, Auteur
- Recherche d'informations par ISBN (scan par appareil photo ou remplissage manuel)
- Export et import de la sauvegarde (fichier ZIP)

## Compilation

```bash
./gradlew assembleDebug
```

Sous Windows : `.\gradlew.bat :app:assembleDebug`

Le fichier `local.properties` (chemin du SDK Android sur votre machine) n'est pas versionné. Créez-le à la racine du projet avec une ligne du type `sdk.dir=C:\\Android\\Sdk` (adaptez le chemin à votre installation).

## Retours

Merci de me faire part de vos commentaires ou de vos souhaits d'amélioration : vous pouvez ouvrir une [issue](https://github.com/jc14fr/MaBiblio/issues) ou m'écrire à apk@jc14fr.ovh.

## Licence

Ce projet est distribué sous licence GPL-3.0. Voir le fichier [LICENSE](LICENSE).

## Soutien

Si l'application vous est utile, vous pouvez offrir un café : https://revolut.me/jc14fr