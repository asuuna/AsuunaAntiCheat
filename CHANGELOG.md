# Changelog

## [Unreleased]

### Ajoute

- Guide `INSTALLATION.md` pour ajouter le plugin sur un serveur.
- Resume d'installation dans le README.

## [1.2.0] - 2026-06-10

### Ajoute

- Checks block-reach, nuker, fast-bow, fast-eat, inventory-click-speed et nofall heuristique.
- Annulation configurable pour les actions bloc, inventaire, arc et nourriture.
- Nettoyage des caches joueur pour les nouveaux samples d'interaction.

### Modifie

- Documentation des limites nofall en Bukkit pur.

## [1.1.0] - 2026-06-10

### Ajoute

- Checks avances: timer, step, liquid-walk, combat-angle, multi-aura, scaffold et inventory-move.
- Detection autoclicker amelioree avec regularite des intervalles.
- Alertes enrichies avec ping joueur et TPS serveur quand disponible.
- Exclusions de mondes via `settings.disabled-worlds`.

### Modifie

- Documentation des limites Bukkit-only et des seuils de production.
- Tests de coherence des cles de checks.

## [1.0.0] - 2026-06-10

### Ajoute

- Base du plugin AsuunaAntiCheat.
- Checks movement-speed, flight, reach, autoclicker, fast-place et fast-break.
- Systemes de violations, decay, alertes staff et punishments configurables.
- Commandes `/sac`.
- Configuration, messages et licence proprietaire asuuna.
- Tests unitaires Maven/JUnit.
