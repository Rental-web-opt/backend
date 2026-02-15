package com.rental.backend.config;

import com.rental.backend.model.*;
import com.rental.backend.repository.*;
import com.rental.backend.kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired(required = false) private KafkaProducerService kafkaProducerService;
    @Autowired(required = false) private com.rental.backend.elasticsearch.CarSearchRepository carSearchRepository;
    @Autowired(required = false) private com.rental.backend.elasticsearch.AgencySearchRepository agencySearchRepository;

    @Value("${app.data.force-reload:false}")
    private boolean forceReload;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        // Vérifier si on doit forcer le rechargement
        boolean shouldLoad = userRepository.count() == 0 || forceReload;
        
        if (shouldLoad) {
            if (forceReload && userRepository.count() > 0) {
                System.out.println("🔄 Force reload activé - Suppression des données existantes...");
                clearAllData();
            }
            
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║        🚀 INITIALISATION DES DONNÉES EASY-RENT 🚀           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");

            List<User> agencyUsers = loadUsers();
            List<Agency> agencies = loadAgencies(agencyUsers);
            List<Car> cars = loadCars(agencies);
            List<Driver> drivers = loadDrivers();
            loadBookings(cars, drivers);
            
            syncElasticsearch();
            sendWelcomeNotifications();
            
            printCredentials();
        } else {
            System.out.println("✅ Données déjà présentes. Utilisez --app.data.force-reload=true pour recharger.");
        }
    }

    private void clearAllData() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        carRepository.deleteAll();
        driverRepository.deleteAll();
        agencyRepository.deleteAll();
        userRepository.deleteAll();
        
        if (carSearchRepository != null) {
            carSearchRepository.deleteAll();
        }
        if (agencySearchRepository != null) {
            agencySearchRepository.deleteAll();
        }
        System.out.println("🗑️ Toutes les données ont été supprimées.");
    }

    private List<User> loadUsers() {
        System.out.println("\n👥 Chargement des utilisateurs...");
        
        // Admin
        createUser("Admin System", "admin@easyrent.com", "admin123", Role.ADMIN);
        
        // Agences - on garde les références pour les lier aux entités Agency
        List<User> agencyUsers = new java.util.ArrayList<>();
        agencyUsers.add(createUser("AutoLux Douala", "agency.douala@easyrent.com", "agency123", Role.AGENCY));
        agencyUsers.add(createUser("Premium Cars Yaoundé", "agency.yaounde@easyrent.com", "agency123", Role.AGENCY));
        agencyUsers.add(createUser("Speed Motors Bafoussam", "agency.bafoussam@easyrent.com", "agency123", Role.AGENCY));
        
        // Utilisateurs réguliers
        String[] names = {
            "Jean Dupont", "Marie Kouam", "Paul Ngoh", "Sophie Bella", "Marc Tchoupo",
            "Alice Fouda", "David Mbarga", "Emma Nkeng", "Lucas Tabi", "Julie Mengueme",
            "Thomas Eto'o", "Sarah Ngo", "Kevin Mbia", "Claire Onana", "Antoine Bassogog",
            "Léa Moukouri", "Hugo Songo'o", "Camille Djemba", "Maxime Webo", "Inès Choupo"
        };
        
        for (int i = 0; i < names.length; i++) {
            String email = "user" + (i + 1) + "@easyrent.com";
            createUser(names[i], email, "user123", Role.USER);
        }
        
        System.out.println("   ✓ " + userRepository.count() + " utilisateurs créés");
        return agencyUsers;
    }

    private User createUser(String name, String email, String password, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private List<Agency> loadAgencies(List<User> agencyUsers) {
        System.out.println("\n🏢 Chargement des agences...");
        
        Agency[] agenciesData = {
            createAgency("AutoLux Douala", "Douala", "Bonanjo, Rue de la Liberté", 
                "L'excellence automobile au cœur de Douala", "08h-20h", 4.8, 156,
                Arrays.asList("Luxe", "24/7", "Aéroport")),
            createAgency("Premium Cars Yaoundé", "Yaoundé", "Bastos, Avenue Kennedy",
                "Location premium pour clients exigeants", "07h-19h", 4.7, 203,
                Arrays.asList("Premium", "VIP", "Chauffeur")),
            createAgency("Speed Motors Bafoussam", "Bafoussam", "Centre Commercial, Zone A",
                "Rapidité et fiabilité garanties", "08h-18h", 4.5, 89,
                Arrays.asList("Rapide", "Économique")),
            createAgency("Elite Auto Kribi", "Kribi", "Boulevard de l'Océan",
                "Location touristique et événementielle", "08h-20h", 4.9, 67,
                Arrays.asList("Tourisme", "Plage", "Événements")),
            createAgency("Royal Cars Garoua", "Garoua", "Avenue du Commerce",
                "Leader de la location dans le Nord", "08h-18h", 4.6, 112,
                Arrays.asList("Nord", "Safari", "4x4")),
            createAgency("Express Rent Limbe", "Limbe", "Mile 4, Beach Road",
                "Location rapide pour vos escapades", "07h-21h", 4.4, 78,
                Arrays.asList("Plage", "Weekend", "Express")),
            createAgency("Capital Motors Yaoundé", "Yaoundé", "Mvan, Face Total",
                "Service 5 étoiles dans la capitale", "24h/24", 4.8, 189,
                Arrays.asList("24/7", "5 étoiles", "Corporate")),
            createAgency("West Coast Auto Buea", "Buea", "Molyko, University Junction",
                "Spécialiste montagne et aventure", "08h-19h", 4.5, 56,
                Arrays.asList("Montagne", "Aventure", "4x4"))
        };

        // 🔗 Lier les 3 premières agences aux comptes utilisateur AGENCY
        for (int i = 0; i < agencyUsers.size() && i < agenciesData.length; i++) {
            agenciesData[i].setUserId(agencyUsers.get(i).getId());
            System.out.println("   🔗 Agence '" + agenciesData[i].getName() + "' liée au User ID: " + agencyUsers.get(i).getId());
        }
        
        List<Agency> savedAgencies = Arrays.asList(agencyRepository.saveAll(Arrays.asList(agenciesData)).toArray(new Agency[0]));
        System.out.println("   ✓ " + savedAgencies.size() + " agences créées");
        return savedAgencies;
    }

    private Agency createAgency(String name, String city, String location, String desc, 
                                 String hours, double rating, int reviews, List<String> tags) {
        Agency agency = new Agency();
        agency.setName(name);
        agency.setCity(city);
        agency.setLocation(location);
        agency.setDescription(desc);
        agency.setOpeningHours(hours);
        agency.setOpen(true);
        agency.setRating(rating);
        agency.setReviewCount(reviews);
        agency.setTags(tags);
        agency.setPhone("+237 6" + (50000000 + random.nextInt(50000000)));
        agency.setEmail(name.toLowerCase().replace(" ", ".") + "@easyrent.cm");
        return agency;
    }

    private List<Car> loadCars(List<Agency> agencies) {
        System.out.println("\n🚗 Chargement des véhicules...");
        
        String[][] carsData = {
            // SUV
            {"Mercedes GLE 450", "Mercedes", "SUV", "GLE 450", "85000", "9000", "Automatique", "Essence", "5", "220", "/assets/car1.jpeg"},
            {"BMW X5 M Sport", "BMW", "SUV", "X5", "90000", "9500", "Automatique", "Essence", "5", "250", "/assets/car2.jpeg"},
            {"Range Rover Evoque", "Land Rover", "SUV", "Evoque", "95000", "10000", "Automatique", "Diesel", "5", "230", "/assets/car3.jpeg"},
            {"Audi Q7 Premium", "Audi", "SUV", "Q7", "88000", "9200", "Automatique", "Diesel", "7", "240", "/assets/car4.jpeg"},
            {"Toyota Land Cruiser", "Toyota", "SUV", "Land Cruiser", "110000", "11500", "Automatique", "Diesel", "7", "200", "/assets/car1.jpeg"},
            {"Porsche Cayenne", "Porsche", "SUV", "Cayenne", "150000", "16000", "Automatique", "Essence", "5", "280", "/assets/car2.jpeg"},
            {"Lexus RX 350", "Lexus", "SUV", "RX 350", "82000", "8500", "Automatique", "Essence", "5", "215", "/assets/car3.jpeg"},
            {"Volvo XC90", "Volvo", "SUV", "XC90", "78000", "8000", "Automatique", "Hybride", "7", "210", "/assets/car4.jpeg"},
            
            // Berlines
            {"Mercedes Classe S", "Mercedes", "Luxe", "S500", "120000", "12500", "Automatique", "Essence", "5", "260", "/assets/car1.jpeg"},
            {"BMW Série 7", "BMW", "Luxe", "740i", "115000", "12000", "Automatique", "Essence", "5", "255", "/assets/car2.jpeg"},
            {"Audi A8", "Audi", "Luxe", "A8", "110000", "11500", "Automatique", "Diesel", "5", "250", "/assets/car3.jpeg"},
            {"Toyota Camry", "Toyota", "Berline", "Camry", "45000", "4800", "Automatique", "Essence", "5", "200", "/assets/car4.jpeg"},
            {"Honda Accord", "Honda", "Berline", "Accord", "42000", "4500", "Automatique", "Essence", "5", "195", "/assets/car1.jpeg"},
            {"Hyundai Sonata", "Hyundai", "Berline", "Sonata", "38000", "4000", "Automatique", "Essence", "5", "190", "/assets/car2.jpeg"},
            {"Mazda 6", "Mazda", "Berline", "6", "40000", "4200", "Automatique", "Essence", "5", "200", "/assets/car3.jpeg"},
            {"Volkswagen Passat", "Volkswagen", "Berline", "Passat", "44000", "4600", "Automatique", "Diesel", "5", "205", "/assets/car4.jpeg"},
            
            // Sport
            {"Porsche 911 Turbo", "Porsche", "Sport", "911", "250000", "26000", "Automatique", "Essence", "2", "330", "/assets/car1.jpeg"},
            {"Ferrari Roma", "Ferrari", "Sport", "Roma", "350000", "37000", "Automatique", "Essence", "2", "320", "/assets/car2.jpeg"},
            {"Lamborghini Huracan", "Lamborghini", "Sport", "Huracan", "400000", "42000", "Automatique", "Essence", "2", "340", "/assets/car3.jpeg"},
            {"Audi R8", "Audi", "Sport", "R8", "280000", "29500", "Automatique", "Essence", "2", "330", "/assets/car4.jpeg"},
            {"Mercedes AMG GT", "Mercedes", "Sport", "AMG GT", "220000", "23000", "Automatique", "Essence", "2", "315", "/assets/car1.jpeg"},
            {"BMW M4 Competition", "BMW", "Sport", "M4", "180000", "19000", "Automatique", "Essence", "4", "290", "/assets/car2.jpeg"},
            
            // Économique
            {"Toyota Corolla", "Toyota", "Économique", "Corolla", "25000", "2800", "Automatique", "Essence", "5", "180", "/assets/car3.jpeg"},
            {"Honda Civic", "Honda", "Économique", "Civic", "28000", "3000", "Automatique", "Essence", "5", "185", "/assets/car4.jpeg"},
            {"Hyundai Elantra", "Hyundai", "Économique", "Elantra", "22000", "2500", "Automatique", "Essence", "5", "175", "/assets/car1.jpeg"},
            {"Kia Forte", "Kia", "Économique", "Forte", "20000", "2200", "Automatique", "Essence", "5", "170", "/assets/car2.jpeg"},
            {"Nissan Sentra", "Nissan", "Économique", "Sentra", "23000", "2600", "Automatique", "Essence", "5", "175", "/assets/car3.jpeg"},
            {"Mazda 3", "Mazda", "Économique", "3", "26000", "2900", "Automatique", "Essence", "5", "185", "/assets/car4.jpeg"},
            
            // 4x4
            {"Jeep Wrangler", "Jeep", "4x4", "Wrangler", "75000", "8000", "Manuelle", "Essence", "5", "180", "/assets/car1.jpeg"},
            {"Toyota Hilux", "Toyota", "4x4", "Hilux", "55000", "5800", "Manuelle", "Diesel", "5", "170", "/assets/car2.jpeg"},
            {"Ford Ranger", "Ford", "4x4", "Ranger", "52000", "5500", "Manuelle", "Diesel", "5", "175", "/assets/car3.jpeg"},
            {"Mitsubishi L200", "Mitsubishi", "4x4", "L200", "48000", "5000", "Manuelle", "Diesel", "5", "165", "/assets/car4.jpeg"},
            {"Nissan Navara", "Nissan", "4x4", "Navara", "50000", "5300", "Automatique", "Diesel", "5", "170", "/assets/car1.jpeg"},
            {"Isuzu D-Max", "Isuzu", "4x4", "D-Max", "46000", "4800", "Manuelle", "Diesel", "5", "160", "/assets/car2.jpeg"}
        };

        int carIndex = 0;
        for (String[] data : carsData) {
            Car car = new Car();
            car.setName(data[0]);
            car.setBrand(data[1]);
            car.setType(data[2]);
            car.setModel(data[3]);
            car.setPricePerDay(Double.parseDouble(data[4]));
            car.setPricePerHour(Double.parseDouble(data[5]));
            car.setMonthlyPrice(Double.parseDouble(data[4]) * 25); // 25 jours pour le mois
            car.setTransmission(data[6]);
            car.setFuelType(data[7]);
            car.setSeats(Integer.parseInt(data[8]));
            car.setMaxSpeed(Integer.parseInt(data[9]));
            car.setImage(data[10]);
            car.setAvailable(random.nextDouble() > 0.15); // 85% disponible
            car.setLocation(agencies.get(carIndex % agencies.size()).getCity());
            car.setAgency(agencies.get(carIndex % agencies.size()));
            car.setDescription("Véhicule " + data[0] + " en excellent état, entretien régulier, kilométrage raisonnable.");
            carRepository.save(car);
            carIndex++;
        }
        
        System.out.println("   ✓ " + carRepository.count() + " véhicules créés");
        return carRepository.findAll();
    }

    private List<Driver> loadDrivers() {
        System.out.println("\n🧑‍✈️ Chargement des chauffeurs...");
        
        String[][] driversData = {
            {"Emmanuel Eboué", "Douala", "45", "20", "4.9", "Chauffeur professionnel avec 20 ans d'expérience. Spécialiste des trajets longue distance."},
            {"Samuel Eto'o Jr", "Yaoundé", "35", "12", "4.8", "Expert en conduite touristique et événementielle."},
            {"Patrick Mboma", "Bafoussam", "50", "25", "4.7", "Ancien chauffeur VIP, connaissance parfaite de l'Ouest."},
            {"Roger Milla", "Kribi", "55", "30", "5.0", "Légende du transport camerounais, service impeccable."},
            {"Rigobert Song", "Garoua", "48", "22", "4.6", "Spécialiste des safaris et excursions dans le Nord."},
            {"Geremi Njitap", "Limbe", "42", "18", "4.8", "Expert des routes côtières, parfait pour les escapades plage."},
            {"Lauren Etame", "Yaoundé", "38", "14", "4.7", "Chauffeur corporate, discret et professionnel."},
            {"Marc-Vivien Foé", "Douala", "40", "16", "4.9", "Multilangue, idéal pour les clients internationaux."},
            {"Joseph-Antoine Bell", "Buea", "52", "28", "4.8", "Maître des routes de montagne, conduite sécuritaire."},
            {"Thomas Nkono", "Yaoundé", "58", "35", "5.0", "Doyen des chauffeurs, référence nationale."},
            {"Jacques Songo'o", "Douala", "44", "19", "4.7", "Spécialiste aéroport et transferts VIP."},
            {"François Omam-Biyik", "Bafoussam", "47", "21", "4.6", "Expert des routes de l'Ouest et du Nord-Ouest."},
            {"Benjamin Moukandjo", "Kribi", "36", "11", "4.8", "Jeune dynamique, parfait pour les clients business."},
            {"Eric Choupo-Moting", "Yaoundé", "32", "8", "4.5", "Nouveau mais prometteur, très apprécié des jeunes clients."},
            {"Vincent Aboubakar", "Garoua", "34", "10", "4.7", "Connaissance approfondie de l'Extrême-Nord."}
        };

        for (String[] data : driversData) {
            Driver driver = new Driver();
            driver.setFullName(data[0]);
            driver.setName(data[0]);
            driver.setLocation(data[1]);
            driver.setAge(Integer.parseInt(data[2]));
            driver.setExperience(Integer.parseInt(data[3]));
            driver.setRating(Double.parseDouble(data[4]));
            driver.setBio(data[5]);
            driver.setPricePerDay((double) (15000 + random.nextInt(25000))); // 15k à 40k
            driver.setAvailable(random.nextDouble() > 0.2); // 80% disponible
            driver.setPhone("+237 6" + (90000000 + random.nextInt(10000000)));
            driver.setEmail(data[0].toLowerCase().replace(" ", ".") + "@drivers.easyrent.cm");
            driver.setReviewCount(random.nextInt(150) + 20);
            driver.setLanguages(Arrays.asList("Français", "Anglais", random.nextBoolean() ? "Pidgin" : "Ewondo"));
            driverRepository.save(driver);
        }
        
        System.out.println("   ✓ " + driverRepository.count() + " chauffeurs créés");
        return driverRepository.findAll();
    }

    private void loadBookings(List<Car> cars, List<Driver> drivers) {
        System.out.println("\n📅 Chargement des réservations...");
        
        List<User> users = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.USER)
            .toList();

        BookingStatus[] statuses = {BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.CANCELLED};
        String[] rentalTypes = {"daily", "hourly", "monthly"};

        // Créer 50 réservations
        for (int i = 0; i < 50; i++) {
            Booking booking = new Booking();
            
            Car car = cars.get(random.nextInt(cars.size()));
            User user = users.get(random.nextInt(users.size()));
            
            booking.setCar(car);
            booking.setUserId(user.getId());
            
            // Dates aléatoires (derniers 60 jours)
            int daysAgo = random.nextInt(60);
            int duration = random.nextInt(7) + 1; // 1 à 7 jours
            LocalDateTime start = LocalDateTime.now().minusDays(daysAgo);
            LocalDateTime end = start.plusDays(duration);
            
            booking.setStartDate(start);
            booking.setEndDate(end);
            booking.setRentalType(rentalTypes[random.nextInt(rentalTypes.length)]);
            booking.setWithDriver(random.nextBoolean());
            
            // Calcul du prix
            double carPrice = car.getPricePerDay() * duration;
            double driverPrice = booking.isWithDriver() ? 25000 * duration : 0;
            booking.setTotalPrice(carPrice + driverPrice);
            
            // Status basé sur la date
            if (end.isBefore(LocalDateTime.now())) {
                booking.setStatus(random.nextDouble() > 0.1 ? BookingStatus.COMPLETED : BookingStatus.CANCELLED);
            } else if (start.isBefore(LocalDateTime.now())) {
                booking.setStatus(BookingStatus.CONFIRMED);
            } else {
                booking.setStatus(random.nextDouble() > 0.3 ? BookingStatus.CONFIRMED : BookingStatus.PENDING);
            }
            
            booking.setCreatedAt(start.minusDays(random.nextInt(5) + 1));
            bookingRepository.save(booking);
            
            // Créer un paiement pour les réservations confirmées/complétées
            if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED) {
                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setUserId(user.getId());
                payment.setAmount(booking.getTotalPrice());
                payment.setPaymentMethod(random.nextBoolean() ? "MOBILE_MONEY" : "CARD");
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCreatedAt(booking.getCreatedAt().plusHours(1));
                paymentRepository.save(payment);
            }
        }
        
        System.out.println("   ✓ " + bookingRepository.count() + " réservations créées");
        System.out.println("   ✓ " + paymentRepository.count() + " paiements créés");
    }

    private void syncElasticsearch() {
        System.out.println("\n⚡ Synchronisation Elasticsearch...");
        
        if (carSearchRepository != null) {
            carRepository.findAll().forEach(car -> {
                com.rental.backend.elasticsearch.CarDocument doc = new com.rental.backend.elasticsearch.CarDocument();
                doc.setId(car.getId().toString());
                doc.setName(car.getName());
                doc.setBrand(car.getBrand());
                doc.setType(car.getType());
                doc.setPricePerDay(car.getPricePerDay());
                doc.setAvailable(car.isAvailable());
                carSearchRepository.save(doc);
            });
            System.out.println("   ✓ Voitures indexées dans Elasticsearch");
        }

        if (agencySearchRepository != null) {
            agencyRepository.findAll().forEach(agency -> {
                com.rental.backend.elasticsearch.AgencyDocument doc = new com.rental.backend.elasticsearch.AgencyDocument();
                doc.setId(agency.getId().toString());
                doc.setName(agency.getName());
                doc.setCity(agency.getCity());
                doc.setLocation(agency.getLocation());
                doc.setIsOpen(agency.isOpen());
                agencySearchRepository.save(doc);
            });
            System.out.println("   ✓ Agences indexées dans Elasticsearch");
        }
    }

    private void sendWelcomeNotifications() {
        if (kafkaProducerService != null) {
            System.out.println("\n📬 Envoi des notifications Kafka...");
            
            // Notification à l'admin
            kafkaProducerService.sendNotification("1", "🎉 Système initialisé avec succès! Bienvenue sur Easy-Rent.");
            
            // Notifications aux utilisateurs
            userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .limit(5)
                .forEach(user -> {
                    kafkaProducerService.sendNotification(
                        user.getId().toString(), 
                        "👋 Bienvenue " + user.getFullName() + "! Découvrez nos offres exclusives."
                    );
                });
            
            // Événements de réservation
            bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .limit(10)
                .forEach(booking -> {
                    kafkaProducerService.sendBookingEvent(
                        "BOOKING_CONFIRMED|" + booking.getId() + "|" + booking.getCar().getName()
                    );
                });
            
            System.out.println("   ✓ Notifications Kafka envoyées");
        }
    }

    private void printCredentials() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              📋 IDENTIFIANTS DE CONNEXION                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  ADMIN:   admin@easyrent.com / admin123                      ║");
        System.out.println("║  AGENCE:  agency.douala@easyrent.com / agency123             ║");
        System.out.println("║  USER:    user1@easyrent.com / user123                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Total: " + userRepository.count() + " utilisateurs, " + 
                           carRepository.count() + " véhicules, " + 
                           agencyRepository.count() + " agences        ║");
        System.out.println("║  Réservations: " + bookingRepository.count() + 
                           ", Chauffeurs: " + driverRepository.count() + "                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
