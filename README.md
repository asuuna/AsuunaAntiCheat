# AsuunaAntiCheat

[![Build](https://github.com/asuuna/AsuunaAntiCheat/actions/workflows/build.yml/badge.svg)](https://github.com/asuuna/AsuunaAntiCheat/actions/workflows/build.yml)

Plugin anti-cheat Minecraft custom en Java pour Paper, Spigot et Bukkit.

Createur, auteur et mainteneur: asuuna.

## Objectif

AsuunaAntiCheat detecte des comportements suspects sans bannir automatiquement par defaut. Le plugin privilegie les alertes staff, les violations configurees et les mitigations progressives afin de limiter les faux positifs sur un serveur reel.

## Checks inclus

- `movement-speed`: deplacement horizontal anormal.
- `flight`: maintien en l'air ou hover suspect.
- `timer`: trop de mouvements utiles dans une fenetre courte.
- `step`: gain vertical anormal sans contexte legitime.
- `liquid-walk`: marche stable sur liquide.
- `reach`: distance d'attaque trop longue.
- `combat-angle`: attaque avec angle de visee incoherent.
- `multi-aura`: trop de cibles attaquees dans une courte fenetre.
- `autoclicker`: CPS trop eleve sur fenetre glissante.
- `fast-place`: placement de blocs trop rapide.
- `fast-break`: cassage de blocs trop rapide.
- `scaffold`: placement sous le joueur sans regard/sneak coherent.
- `inventory-move`: mouvement horizontal avec inventaire serveur ouvert.

## Compatibilite

- Java 17 minimum.
- Compile contre Spigot API `1.20.4-R0.1-SNAPSHOT`.
- Cible runtime realiste: Paper, Spigot et Bukkit 1.20.x+.
- Pas de ProtocolLib requis.
- Les alertes ajoutent le ping joueur et le TPS si la plateforme expose ces informations.

## Build

```bash
mvn clean package
```

Jar genere:

```text
target/AsuunaAntiCheat-1.1.0.jar
```

## Commandes

| Commande | Permission | Description |
| --- | --- | --- |
| `/sac help` | `asuunaac.command` | Aide |
| `/sac status` | `asuunaac.command` | Etat du plugin |
| `/sac alerts` | `asuunaac.alerts` | Active/desactive les alertes |
| `/sac profile <joueur>` | `asuunaac.command` | Affiche les violations |
| `/sac reset <joueur>` | `asuunaac.admin` | Reinitialise les violations |
| `/sac reload` | `asuunaac.admin` | Recharge la configuration |

Alias: `/asuunaac`, `/anticheat`.

## Permissions

| Permission | Description |
| --- | --- |
| `asuunaac.command` | Acces aux commandes de lecture |
| `asuunaac.alerts` | Reception des alertes |
| `asuunaac.admin` | Administration complete |
| `asuunaac.bypass` | Ignore tous les checks |

## Licence

AsuunaAntiCheat appartient a asuuna. Le code est publie sous licence proprietaire `All rights reserved`: lecture, compilation et usage serveur personnel autorises, mais redistribution, revente, re-upload, suppression de l'attribution ou revendication d'auteur interdites sans autorisation explicite de asuuna.

## Limites connues

- Aucun anti-cheat Bukkit-only ne peut detecter parfaitement tous les cheats sans acces paquet/protocole.
- Les checks packet-level comme spoof de rotation avance, bad packets profonds ou timer ultra fin demandent une couche protocole dediee.
- Les checks sont volontairement conservateurs par defaut.
- Tester les seuils avec vos plugins de mouvement, jobs, skills, pets, claims et items custom avant production.
