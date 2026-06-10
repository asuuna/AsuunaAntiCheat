# Installer AsuunaAntiCheat

Ce guide explique comment ajouter AsuunaAntiCheat sur un serveur Paper, Spigot ou Bukkit.

## Prerequis

- Serveur Minecraft Paper, Spigot ou Bukkit compatible 1.20.x+.
- Java 17 minimum.
- Acces au dossier du serveur.
- Permission de redemarrer le serveur.

## Installation rapide

1. Telecharge le dernier jar depuis la release GitHub:
   - https://github.com/asuuna/AsuunaAntiCheat/releases/latest

2. Place le fichier jar dans le dossier:

```text
plugins/
```

3. Redemarre completement le serveur.

4. Verifie que le plugin est charge:

```text
/plugins
```

Le plugin doit apparaitre sous le nom `AsuunaAntiCheat`.

## Premiere configuration

Au premier demarrage, le plugin cree:

```text
plugins/AsuunaAntiCheat/config.yml
plugins/AsuunaAntiCheat/messages.yml
```

Politique par defaut:

- alertes activees;
- mitigations/annulations progressives activees selon les checks;
- bannissements automatiques desactives;
- seuils conservateurs pour limiter les faux positifs.

## Permissions conseillees

Donne ces permissions aux staffs:

```text
asuunaac.command
asuunaac.alerts
```

Donne cette permission uniquement aux administrateurs:

```text
asuunaac.admin
```

Donne cette permission seulement aux joueurs qui doivent ignorer tous les checks:

```text
asuunaac.bypass
```

## Commandes utiles

```text
/sac help
/sac status
/sac alerts
/sac profile <joueur>
/sac reset <joueur>
/sac reload
```

Alias disponibles:

```text
/asuunaac
/anticheat
```

## Reglages recommandes

Avant production:

1. Lance le plugin sur un serveur de test.
2. Garde les punishments automatiques desactives au debut.
3. Observe les alertes staff pendant plusieurs heures de jeu normal.
4. Ajuste les seuils dans `config.yml` selon ton gameplay.
5. Ajoute les mondes speciaux dans `settings.disabled-worlds` si besoin.

Exemple:

```yaml
settings:
  disabled-worlds:
    - world_event
    - world_lobby
```

## Mise a jour

1. Arrete le serveur.
2. Remplace l'ancien jar dans `plugins/`.
3. Garde une sauvegarde de `plugins/AsuunaAntiCheat/config.yml`.
4. Redemarre le serveur.
5. Compare les nouvelles options de config avec celles du changelog.

## Limites techniques

AsuunaAntiCheat fonctionne sans dependance externe. Cette approche garde l'installation simple, mais certains cheats modernes demandent une couche packet/protocole pour une detection plus profonde.

Pour les serveurs competitifs, il faut tester les seuils avec les plugins de mouvement, pets, skills, items custom, claims, jobs et events deja installes.
