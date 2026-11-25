# Décisions prises sans Julien

Chaque décision est réversible. Format : décision, alternatives, raison, retour arrière.

## DA-01 — L'orchestrateur écrit les fichiers des sous-agents
- Décision : les quatre fichiers `findings-*.md` et les deux fichiers `challenge*.md` sont recopiés par l'orchestrateur depuis la réponse finale des sous-agents, sans modification du fond. Chaque fichier porte une note qui le dit.
- Alternatives : relancer les sous-agents ; garder les constats seulement dans la conversation.
- Raison : le harnais refuse aux sous-agents l'écriture de fichiers de rapport (« Subagents should return findings as text »). Le skill exige un état dans des fichiers.
- Retour arrière : aucun besoin ; le contenu est identique.

## DA-02 — Un changement de format de paquet est une porte à double sens
- Décision : C-MC-01, C-MC-03, C-MC-10 et C-AR-11 gardent la porte « double sens », même quand le format d'un paquet change.
- Alternative : classer tout changement de paquet en sens unique, comme le propose la référence d'audit pour un client et un serveur de versions différentes.
- Raison : `@Mod` ne déclare pas `acceptableRemoteVersions`. FML exige donc la même version des deux côtés (`FMLModContainer.java:608`, `NetworkModHolder.acceptVersion`, `FMLHandshakeServerState.java:70`). Deux versions différentes ne se connectent jamais.
- Retour arrière : reclasser ces constats si `acceptableRemoteVersions` apparaît un jour.

## DA-03 — Fusion des doublons
- Décision : C-VE-03 → C-MC-01 ; C-AR-01 → C-MC-03 ; C-AR-02 → C-MC-05 et C-MC-09 ; C-AR-04 → C-VE-05 ; C-AR-09 et C-PR-05 → C-MC-10 ; C-PR-08 → C-AR-12 ; C-VE-07 → C-AR-03.
- Raison : deux lentilles ont trouvé le même défaut. Un seul ticket le corrigera.
- Retour arrière : les constats d'origine restent dans `findings-*.md`.

## DA-04 — La sévérité retenue est celle du contre-expert
- Décision : quand un contre-expert baisse une sévérité avec une preuve, `AUDIT.md` garde la sévérité baissée (C-MC-01, C-MC-04, C-VE-02, C-VE-04, C-VE-05, C-VE-06, C-VE-07, C-AR-04, C-AR-05, C-AR-06, C-AR-07, C-PR-04).
- Raison : le contre-expert a lu le code sans le raisonnement de l'auteur, et chaque baisse s'appuie sur une preuve.
- Retour arrière : relire `challenge*.md` et rehausser.

## DA-05 — Le terminal exigera `BUILD` à l'ouverture (C-MC-08)
- Décision : pas de question à Julien ; le ticket suivra D12.
- Raison : D12 (« Strict respect of the AE2 security permissions ») est déjà approuvée, et AE2 exige `BUILD` pour son Interface Terminal (`GuiBridge.java:181`).
- Retour arrière : retirer le test d'accès à l'ouverture.

## DA-06 — Les flèches du type suivent l'Encoder (C-MC-16)
- Décision : la flèche droite avancera, comme dans le Package Recipe Encoder.
- Raison : la règle du projet est « on apprend un seul écran » (README, section éditeur).
- Retour arrière : inverser le booléen.

## DA-07 — Un nom de groupe s'efface quand la machine casse (C-MC-04)
- Décision : purge sur `BlockEvent.BreakEvent`, sans nouveau champ dans le NBT de `GroupNames`.
- Alternative : enregistrer la classe de la machine avec le nom, ce qui change le format de sauvegarde (porte à sens unique).
- Raison : la purge suffit au cas décrit et ne touche pas le format.
- Retour arrière : retirer l'abonnement à l'événement.

## DA-08 — `update.json` : la version du mod perd le préfixe Minecraft (C-MC-11)
- Décision : `Reference.VERSION` reçoit `modVersion` seul (`0.1.0-beta.10`), pas `1.12.2-0.1.0-beta.10`.
- Alternative : préfixer les clés de `update.json`.
- Raison : `tools/release.sh:56` écrit déjà la clé sans préfixe ; changer une ligne de `build.gradle` est le plus court.
- Retour arrière : remettre `project.version` dans le `replace` de `build.gradle`.

## DA-09 — Regroupement des constats en 31 tickets
- Décision : des constats proches partagent un ticket quand ils touchent les mêmes fichiers (par exemple T14 : `BUILD` et types désactivés ; T17 : trois défauts du terminal sans fil).
- Alternative : un ticket par constat (plus de 45 tickets).
- Raison : chaque ticket coûte deux sous-agents, et la phase 5 s'arrête après 20 tickets.
- Retour arrière : découper un ticket dans `tickets.json`.

## DA-10 — Ordre des tickets et place de T20
- Décision : harnais (T01–T06), puis bugs majeurs (T07–T13), puis bugs mineurs (T14–T25), puis dette (T26–T31). Le retrait de `tooltipStacks` (T20) est classé « bug », car l'option n'a aucun effet, et il entre ainsi dans les 20 premiers tickets.
- Raison : Julien a tranché la question 5 ; le ticket est petit.
- Retour arrière : reclasser T20 en dette.

## DA-11 — Dépendances limitées aux dépendances réelles
- Décision : seuls T09 (attend T08, même chemin de suppression) et T12 (attend la mesure de T04) ont un bloqueur. L'ordre de la liste fixe le reste.
- Raison : une chaîne de bloqueurs ferait bloquer tous les tickets suivants si un seul échouait.
- Retour arrière : ajouter un bloqueur dans `tickets.json`.

## DA-12 — Constats sans ticket
- Décision : C-MC-15 (hypothèse latente), C-VE-04 (QA seulement), C-VE-08 (question 7 : pas de porte), C-AR-05, C-AR-06, C-AR-11, C-AR-18 et la page JEI vont au backlog d'`AUDIT.md`.
- Raison : pas de moyen de vérifier, décision de Julien, ou refactor de taille M sans bug derrière.
- Retour arrière : créer le ticket.
