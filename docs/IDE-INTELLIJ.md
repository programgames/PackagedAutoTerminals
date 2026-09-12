# Ouvrir le projet dans IntelliJ IDEA

> ⚠️ **Avant tout : IntelliJ doit utiliser le JDK 8.** ForgeGradle 2.3 et Gradle 4.10.3
> échouent sous tout JDK plus récent. C'est la première chose à régler, et la cause de
> presque tous les échecs d'import.

## 1. Importer

1. Ferme le client de dev s'il tourne.
2. Dans IntelliJ : **File → Open**, puis choisis le dossier
   `C:\Users\Julien\Desktop\PackagedAutoTerminals`.
3. IntelliJ détecte `build.gradle` et propose l'import Gradle. Accepte.

## 2. Régler les deux JDK

| Réglage | Valeur | Où |
|---|---|---|
| **Gradle JVM** | `C:\Program Files\Eclipse Adoptium\jdk-8.0.472.8-hotspot` | Settings → Build, Execution, Deployment → Build Tools → Gradle |
| **Project SDK** | le même JDK 8, niveau de langage **8** | File → Project Structure → Project |
| **Distribution Gradle** | **Use Gradle from: gradle-wrapper.properties** | même page que le Gradle JVM |

> Si IntelliJ refuse Gradle 4.10.3 parce qu'il est trop ancien, garde le wrapper et ignore
> l'avertissement. Ne mets **jamais** à jour le wrapper : ForgeGradle 2.3 ne va pas au-delà.

## 3. Les quatre configurations fournies

Elles sont versionnées dans `.idea/runConfigurations/` et apparaissent dans le menu
déroulant en haut à droite.

| Configuration | Type | Ce qu'elle fait |
|---|---|---|
| **Gradle runClient** | Gradle | lance le client de dev par la tâche `runClient`. **C'est la voie recommandée.** |
| **Gradle build** | Gradle | construit le jar |
| **Minecraft Client** | Application | lance `GradleStart` directement, pour le débogage pas à pas |
| **Minecraft Server** | Application | lance `GradleStartServer` |

Toutes travaillent dans le dossier `run`, donc le monde, les touches et le pseudo `PAT_Dev`
sont conservés d'une session à l'autre.

### Quelle configuration choisir

- **Pour tester** : `Gradle runClient`. Elle passe par ForgeGradle, donc la version du mod
  est injectée et les ressources sont placées correctement.
- **Pour déboguer** : `Minecraft Client`, avec le bouton *Debug*. Les points d'arrêt
  fonctionnent, et le rechargement à chaud des méthodes aussi.

> Avec `Minecraft Client`, la version du mod s'affiche `@MOD_VERSION@` dans la liste des
> mods. C'est normal : la substitution est faite par ForgeGradle, que cette configuration
> court-circuite. Rien d'autre ne change.

## 4. Si une configuration « Application » ne démarre pas

Le nom du module doit correspondre à celui qu'IntelliJ a créé à l'import, normalement
`packagedautoterminals.main`. S'il diffère, ouvre la configuration et choisis le bon module
dans la liste.

Tu peux aussi laisser ForgeGradle les régénérer, une fois le projet importé :

```
gradlew genIntellijRuns
```

Cette tâche n'écrit rien tant qu'IntelliJ n'a pas importé le projet. C'est pourquoi les
quatre fichiers ci-dessus sont fournis d'avance.

## 5. Ce que Git suit, et ce qu'il ignore

`.gitignore` ignore tout `.idea/`, **sauf** `.idea/runConfigurations/`. Les configurations
de lancement sont donc partagées, et les réglages personnels restent locaux.
