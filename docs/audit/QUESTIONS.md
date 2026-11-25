# Questions pour Julien

Réponds en écrivant `Réponse :` sous chaque question. « oui » accepte la recommandation. Relance ensuite `/audit-mod suite`.

Les autres choix sont réversibles. L'orchestrateur les a pris seul et les note dans `DECISIONS.md`.

---

## 1. L'éjection par le bouton de ligne (C-PR-01)

Contexte : un clic simple, avec n'importe quel bouton de souris, renvoie au réseau le porte-recettes de chaque machine du groupe. L'infobulle dit « Shift + left click ». Tu as voulu ce bouton à deux sens dans le commit `8158530`, mais le terminal ne sait pas remettre un porte-recettes qui porte des recettes.

Options :
- **A (recommandée)** : le bouton garde seulement « insérer » et « réparer ». L'éjection passe sur Maj + clic gauche de la ligne, avec une confirmation en deux clics.
- B : le bouton garde l'éjection, mais seulement au clic gauche, avec une confirmation en deux clics et une infobulle juste.
- C : seule l'infobulle change.

Si tu suis la recommandation : un ticket S. L'infobulle, le README et le test E6 de `TESTING.md` changent.

Réponse : oui (A, donnée le 2026-11-25)

## 2. La suppression d'une recette (C-PR-02)

Contexte : un clic droit sur la croix supprime la recette dans toutes les machines du groupe, sans confirmation. Le README enseigne le clic droit pour « éditer ». Après Delete, l'éditeur vide la grille.

Options :
- **A (recommandée)** : la croix accepte seulement le clic gauche, avec une confirmation en deux clics (même motif que « non enregistré »). Après Delete, l'éditeur garde la grille remplie sur l'onglet « nouvelle recette » : un Save restaure la recette.
- B : clic gauche seulement, sans confirmation.
- C : confirmation seulement.

Si tu suis la recommandation : un ticket S, groupé avec la suppression par identité (C-MC-03).

Réponse : oui (A, donnée le 2026-11-25)

## 3. La paire séparée (C-PR-03)

Contexte : une paire sans nom dont un côté porte une recette de plus se sépare en deux lignes rouges. Le bouton « réparer » de ces lignes écrit dans la seule machine qui a déjà la recette, et il répond « Nothing was written ».

Options :
- **A (recommandée)** : tout de suite, le bouton « réparer » disparaît sur un groupe qui ne contient qu'un rôle. Son infobulle donne la consigne : nommer les deux lignes pareil. Le geste « lier deux lignes » va au backlog.
- B : construire tout de suite le geste « lier deux lignes » (coût M).
- C : ne rien changer.

Si tu suis la recommandation : un ticket S et une clé de traduction de plus (16 langues).

Réponse : oui (A, donnée le 2026-11-25, après une reformulation avec un exemple)

## 4. La hauteur du terminal (C-PR-04) — question de goût

Contexte : le terminal fait 367 px de haut. En 1920×1080, il ne tient ni à l'échelle 4 ni à l'échelle 3, et `GuiScaleFit` descend à l'échelle 2 : le texte devient petit à chaque ouverture.

Options :
- **A (recommandée)** : 10 lignes au lieu de 12 (331 px). Le terminal tient à l'échelle 3 en 1080p.
- B : un nombre de lignes calculé selon la fenêtre, de 6 à 16 (coût M).
- C : garder 12 lignes.

Si tu suis la recommandation : une constante change, et deux lignes de la liste disparaissent.

Réponse : oui (A, donnée le 2026-11-25)

## 5. La clé de configuration `tooltipStacks` — porte à sens unique

Contexte : l'option apparaît dans le fichier de configuration et dans l'écran de configuration, traduite en 16 langues. Aucun code ne la lit. Retirer une clé publique est une porte à sens unique.

Options :
- **A (recommandée)** : retirer la clé et ses 16 traductions. Le ticket vérifie qu'un ancien fichier de configuration se charge sans erreur.
- B : brancher l'option sur l'infobulle de recette (coût S).
- C : la garder telle quelle.

Si tu suis la recommandation : un ticket S. Un administrateur qui avait réglé la valeur ne voit aucune différence, puisqu'elle n'avait aucun effet.

Question de Julien (2026-11-25) : « Est-ce que cette option de configuration peut vraiment être utile ? »

Réponse de l'orchestrateur : non, plus aujourd'hui. L'option limitait le nombre d'objets listés dans l'infobulle d'une recette. Un commit plus récent a retiré cette liste exprès : `GuiPatTerminal.java` dit « Inputs and outputs no longer appear here: the list serves to find a recipe, the editor to read it in detail. » L'option redeviendrait utile seulement si la liste des objets revenait dans l'infobulle.

Réponse : A, retirer la clé (accord explicite de Julien, donné le 2026-11-25 : « on le supprime »).

## 6. Pousser vers GitHub — porte à sens unique

Contexte : 11 commits sont seulement locaux. La CI n'a donc jamais lancé un test, et `update.json` n'existe pas sur `origin/main` : l'avis de mise à jour reçoit une erreur 404.

Options :
- **A (recommandée)** : à la fin de la phase 5, je pousse la seule branche `audit/2026-11-25`, pas `main`. La CI lance le build et les tests sur cette branche. Tu fusionnes dans `main` toi-même.
- B : je ne pousse rien. Tu pousses toi-même.
- C : je pousse aussi `main`.

Si tu suis la recommandation : une seule commande `git push origin audit/2026-11-25`, après le dernier ticket vert.

Réponse : oui (A, donnée le 2026-11-25)

## 7. Une porte de publication en jeu (C-VE-08)

Contexte : `docs/TESTING.md` compte 149 tests, et 6 à 8 groupes n'ont jamais tourné. `tools/release.sh` vérifie le build et le CHANGELOG, mais aucun test en jeu.

Options :
- **A (recommandée)** : ajouter en tête de `TESTING.md` une section « Smoke, 15 minutes » de six tests. `release.sh` refuse alors une étiquette sans la ligne `Smoke passed: <version>`.
- B : ajouter la section seulement, sans contrôle dans `release.sh`.
- C : ne rien changer.

Si tu suis la recommandation : avant chaque publication, tu joues 15 minutes et tu écris une ligne. Le script te le rappelle.

Réponse : C (donnée le 2026-11-25). Julien : « Dans tous les cas, on ne va pas tester pour le moment. On accepte de publier la release. » Pas de section « Smoke », pas de contrôle dans `release.sh`. Les corrections de texte de `TESTING.md` (C-VE-12) restent des tickets.
