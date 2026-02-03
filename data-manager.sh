#!/bin/bash

# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                     EASY-RENT - GESTION DES DONNÉES                       ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_NAME="easyrent_db"
DB_USER="postgres"
DB_PASSWORD="root"
DB_PORT="5433"

show_menu() {
    clear
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════════════════╗"
    echo "║                     🚗 EASY-RENT - GESTION DES DONNÉES                    ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Choisissez une option:${NC}"
    echo ""
    echo "  1) 🗑️  Supprimer TOUTES les données (vider la base)"
    echo "  2) 🔄 Recharger les données (supprimer + insérer)"
    echo "  3) ➕ Charger les données (si base vide)"
    echo "  4) 📊 Afficher les statistiques"
    echo "  5) 🔌 Vérifier les services Docker"
    echo "  6) 🚀 Démarrer le backend avec rechargement"
    echo "  7) ❌ Quitter"
    echo ""
    echo -n "Votre choix: "
}

# Fonction pour exécuter une commande SQL
exec_sql() {
    PGPASSWORD=$DB_PASSWORD psql -h localhost -p $DB_PORT -U $DB_USER -d $DB_NAME -c "$1" 2>/dev/null
}

# Supprimer toutes les données
clear_data() {
    echo -e "${YELLOW}🗑️  Suppression de toutes les données...${NC}"
    
    exec_sql "TRUNCATE TABLE payments CASCADE;"
    exec_sql "TRUNCATE TABLE bookings CASCADE;"
    exec_sql "TRUNCATE TABLE cars CASCADE;"
    exec_sql "TRUNCATE TABLE drivers CASCADE;"
    exec_sql "TRUNCATE TABLE agencies CASCADE;"
    exec_sql "TRUNCATE TABLE users CASCADE;"
    
    echo -e "${GREEN}✅ Toutes les données ont été supprimées.${NC}"
    
    # Nettoyer Elasticsearch aussi
    echo -e "${YELLOW}🔍 Nettoyage d'Elasticsearch...${NC}"
    curl -s -X DELETE "http://localhost:9200/cars" > /dev/null 2>&1
    curl -s -X DELETE "http://localhost:9200/agencies" > /dev/null 2>&1
    echo -e "${GREEN}✅ Index Elasticsearch supprimés.${NC}"
}

# Afficher les statistiques
show_stats() {
    echo -e "${BLUE}📊 Statistiques de la base de données:${NC}"
    echo ""
    
    echo -n "   👥 Utilisateurs: "
    exec_sql "SELECT COUNT(*) FROM users;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo -n "   🏢 Agences: "
    exec_sql "SELECT COUNT(*) FROM agencies;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo -n "   🚗 Voitures: "
    exec_sql "SELECT COUNT(*) FROM cars;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo -n "   🧑‍✈️ Chauffeurs: "
    exec_sql "SELECT COUNT(*) FROM drivers;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo -n "   📅 Réservations: "
    exec_sql "SELECT COUNT(*) FROM bookings;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo -n "   💳 Paiements: "
    exec_sql "SELECT COUNT(*) FROM payments;" | grep -E "^\s*[0-9]+" | tr -d ' '
    
    echo ""
}

# Vérifier les services Docker
check_docker() {
    echo -e "${BLUE}🔌 État des services Docker:${NC}"
    echo ""
    
    services=("easyrent_db" "easyrent-elasticsearch" "easyrent-kafka" "easyrent-zookeeper")
    
    for service in "${services[@]}"; do
        status=$(docker ps --filter "name=$service" --format "{{.Status}}" 2>/dev/null)
        if [ -n "$status" ]; then
            echo -e "   ${GREEN}✅ $service: $status${NC}"
        else
            echo -e "   ${RED}❌ $service: Non démarré${NC}"
        fi
    done
    echo ""
}

# Démarrer le backend avec rechargement forcé
start_with_reload() {
    echo -e "${YELLOW}🚀 Démarrage du backend avec rechargement des données...${NC}"
    echo ""
    
    cd "$BACKEND_DIR"
    
    # Arrêter le backend s'il tourne
    pkill -f "spring-boot:run" 2>/dev/null
    sleep 2
    
    # Démarrer avec le flag force-reload
    echo -e "${GREEN}Exécution: mvn spring-boot:run -Dspring-boot.run.arguments=--app.data.force-reload=true${NC}"
    mvn spring-boot:run -Dspring-boot.run.arguments="--app.data.force-reload=true"
}

# Recharger les données (supprimer + backend restart)
reload_data() {
    echo -e "${YELLOW}🔄 Rechargement complet des données...${NC}"
    clear_data
    echo ""
    echo -e "${YELLOW}📝 Les données seront rechargées au prochain démarrage du backend.${NC}"
    echo -e "${YELLOW}   Utilisez l'option 6 ou exécutez:${NC}"
    echo -e "${GREEN}   mvn spring-boot:run -Dspring-boot.run.arguments=--app.data.force-reload=true${NC}"
}

# Charger les données (si base vide)
load_data() {
    count=$(exec_sql "SELECT COUNT(*) FROM users;" | grep -E "^\s*[0-9]+" | tr -d ' ')
    
    if [ "$count" == "0" ]; then
        echo -e "${YELLOW}➕ La base est vide. Démarrage du backend pour charger les données...${NC}"
        start_with_reload
    else
        echo -e "${YELLOW}⚠️  La base contient déjà $count utilisateurs.${NC}"
        echo -e "   Utilisez l'option 2 pour forcer le rechargement."
    fi
}

# Menu principal
main() {
    while true; do
        show_menu
        read choice
        echo ""
        
        case $choice in
            1)
                clear_data
                ;;
            2)
                reload_data
                ;;
            3)
                load_data
                ;;
            4)
                show_stats
                ;;
            5)
                check_docker
                ;;
            6)
                start_with_reload
                ;;
            7)
                echo -e "${GREEN}👋 Au revoir!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ Option invalide${NC}"
                ;;
        esac
        
        echo ""
        echo -n "Appuyez sur Entrée pour continuer..."
        read
    done
}

# Commandes directes si argument passé
case "$1" in
    "clear"|"delete"|"clean")
        clear_data
        ;;
    "reload"|"reset")
        reload_data
        ;;
    "load")
        load_data
        ;;
    "stats")
        show_stats
        ;;
    "docker")
        check_docker
        ;;
    "start")
        start_with_reload
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  clear   - Supprimer toutes les données"
        echo "  reload  - Supprimer et recharger les données"
        echo "  load    - Charger les données (si base vide)"
        echo "  stats   - Afficher les statistiques"
        echo "  docker  - Vérifier les services Docker"
        echo "  start   - Démarrer le backend avec rechargement"
        echo ""
        echo "Sans argument: affiche le menu interactif"
        ;;
    "")
        main
        ;;
    *)
        echo "Commande inconnue: $1"
        echo "Utilisez '$0 help' pour voir les options disponibles."
        ;;
esac
