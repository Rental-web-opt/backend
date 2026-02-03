# 🚗 Easy-Rent Backend

Backend Spring Boot pour l'application de location de véhicules Easy-Rent.

---

## 📦 Prérequis

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose**

---

## 🚀 Démarrage Rapide

### 1. Démarrer l'infrastructure Docker

```bash
docker-compose up -d
```

Cela démarre :
| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5433 | Base de données |
| Elasticsearch | 9200 | Moteur de recherche |
| Kafka | 9094 | Messagerie événementielle |
| Zookeeper | 2181 | Coordination Kafka |

### 2. Démarrer le backend

```bash
mvn spring-boot:run
```

Le backend sera accessible sur : **http://localhost:8081**

---

## 📊 Gestion des Données

### Script Interactif

Un script shell est fourni pour gérer facilement les données :

```bash
./data-manager.sh
```

Cela affiche un menu interactif avec les options suivantes :

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                     🚗 EASY-RENT - GESTION DES DONNÉES                    ║
╚═══════════════════════════════════════════════════════════════════════════╝

  1) 🗑️  Supprimer TOUTES les données (vider la base)
  2) 🔄 Recharger les données (supprimer + insérer)
  3) ➕ Charger les données (si base vide)
  4) 📊 Afficher les statistiques
  5) 🔌 Vérifier les services Docker
  6) 🚀 Démarrer le backend avec rechargement
  7) ❌ Quitter
```

### Commandes Directes

```bash
# Supprimer TOUTES les données
./data-manager.sh clear

# Supprimer + préparer le rechargement
./data-manager.sh reload

# Charger les données (si base vide)
./data-manager.sh load

# Afficher les statistiques
./data-manager.sh stats

# Vérifier les services Docker
./data-manager.sh docker

# Démarrer le backend avec rechargement forcé
./data-manager.sh start
```

---

## 🔄 Rechargement des Données via Maven

### Option 1 : Rechargement Forcé au Démarrage

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.data.force-reload=true"
```

Cette commande :
1. ✅ Supprime toutes les données existantes
2. ✅ Recrée tous les utilisateurs, agences, voitures, chauffeurs
3. ✅ Génère 50 réservations avec historique
4. ✅ Synchronise Elasticsearch
5. ✅ Envoie des notifications via Kafka

### Option 2 : Chargement Initial (si base vide)

```bash
mvn spring-boot:run
```

Les données ne sont chargées que si la base est vide.

---

## 🗑️ Suppression des Données

### Via le Script

```bash
./data-manager.sh clear
```

### Via SQL Direct

```bash
# Connexion à PostgreSQL
PGPASSWORD=root psql -h localhost -p 5433 -U postgres -d easyrent_db

# Supprimer toutes les tables
TRUNCATE TABLE payments CASCADE;
TRUNCATE TABLE bookings CASCADE;
TRUNCATE TABLE cars CASCADE;
TRUNCATE TABLE drivers CASCADE;
TRUNCATE TABLE agencies CASCADE;
TRUNCATE TABLE users CASCADE;
```

### Réinitialisation Complète (Docker)

```bash
# Arrêter et supprimer les volumes
docker-compose down -v

# Redémarrer
docker-compose up -d

# Relancer le backend
mvn spring-boot:run
```

---

## 📋 Données de Démonstration

Le DataLoader crée automatiquement :

| Type | Quantité | Détails |
|------|----------|---------|
| 👥 Utilisateurs | 23 | 1 admin + 3 agences + 20 users |
| 🏢 Agences | 8 | Douala, Yaoundé, Bafoussam, Kribi, Garoua, Limbe, Buea |
| 🚗 Véhicules | 34 | SUV, Berlines, Luxe, Sport, Économique, 4x4 |
| 🧑‍✈️ Chauffeurs | 15 | Avec biographies et ratings |
| 📅 Réservations | 50 | Historique des 60 derniers jours |
| 💳 Paiements | ~30 | Liés aux réservations confirmées |

---

## 🔑 Identifiants de Connexion

### Administrateur
```
Email:    admin@easyrent.com
Password: admin123
```

### Agences
```
Email:    agency.douala@easyrent.com   | Password: agency123
Email:    agency.yaounde@easyrent.com  | Password: agency123
Email:    agency.bafoussam@easyrent.com| Password: agency123
```

### Utilisateurs
```
Email:    user1@easyrent.com à user20@easyrent.com
Password: user123
```

---

## 🔌 Vérification des Services

### Docker

```bash
docker ps
```

Doit afficher :
- `easyrent_db` (PostgreSQL)
- `easyrent-elasticsearch`
- `easyrent-kafka`
- `easyrent-zookeeper`

### API Backend

```bash
# Santé
curl http://localhost:8081/actuator/health

# Liste des voitures
curl http://localhost:8081/api/cars

# Liste des agences
curl http://localhost:8081/api/agencies
```

### Elasticsearch

```bash
curl http://localhost:9200/_cluster/health
```

---

## 📁 Structure du Projet

```
backend/
├── src/main/java/com/rental/backend/
│   ├── config/
│   │   ├── DataLoader.java      # Chargement des données
│   │   ├── KafkaConfig.java     # Configuration Kafka
│   │   └── SecurityConfig.java  # Sécurité JWT
│   ├── controller/              # Controllers REST
│   ├── model/                   # Entités JPA
│   ├── repository/              # Repositories Spring Data
│   ├── service/                 # Services métier
│   ├── kafka/                   # Producteur/Consommateur Kafka
│   └── elasticsearch/           # Documents Elasticsearch
├── docker-compose.yml           # Infrastructure Docker
├── data-manager.sh              # Script de gestion des données
└── README.md                    # Ce fichier
```

---

## 🐛 Dépannage

### Le backend ne démarre pas

1. Vérifiez que Docker tourne :
   ```bash
   docker ps
   ```

2. Vérifiez les logs Docker :
   ```bash
   docker-compose logs -f
   ```

3. Vérifiez que PostgreSQL est accessible :
   ```bash
   PGPASSWORD=root psql -h localhost -p 5433 -U postgres -d easyrent_db -c "SELECT 1;"
   ```

### Les données ne se chargent pas

1. Vérifiez si des données existent :
   ```bash
   ./data-manager.sh stats
   ```

2. Forcez le rechargement :
   ```bash
   ./data-manager.sh clear
   mvn spring-boot:run
   ```

### Elasticsearch ne répond pas

```bash
# Vérifier le statut
curl http://localhost:9200/_cluster/health

# Redémarrer Elasticsearch
docker restart easyrent-elasticsearch
```

---

## 📝 Variables d'Environnement

| Variable | Valeur par défaut | Description |
|----------|-------------------|-------------|
| `app.data.force-reload` | `false` | Force le rechargement des données |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/easyrent_db` | URL PostgreSQL |
| `spring.elasticsearch.uris` | `http://localhost:9200` | URL Elasticsearch |
| `spring.kafka.bootstrap-servers` | `localhost:9094` | Serveur Kafka |

---

## 📞 Support

Pour toute question, consultez les logs du backend :

```bash
mvn spring-boot:run 2>&1 | tee backend.log
```

---

*Easy-Rent Backend v1.0 - Développé avec ❤️*
