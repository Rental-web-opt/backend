#!/bin/bash

# Base URL
API="http://localhost:8081/api/cars"

echo "Cleaning up existing cars..."
for i in {1..20}
do
   curl -X DELETE "$API/$i" -s > /dev/null
done
echo "Cleanup done."

echo "Seeding Cars..."

# Car 1
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Audi Rouge Sport",
  "pricePerDay": 150000,
  "type": "Sport",
  "brand": "Audi",
  "model": "R8 Coupé",
  "fuelType": "Essence",
  "seats": 2,
  "transmission": "Automatique",
  "image": "/assets/car2.jpeg",
  "description": "Une voiture sportive élégante, parfaite pour les mariages ou les week-ends de luxe.",
  "location": "Yaoundé, Route de Kribi",
  "available": true,
  "maxSpeed": 330,
  "images": ["/assets/car2.jpeg", "/assets/car1.jpeg", "/assets/car3.jpeg", "/assets/car4.jpeg"]
}'
echo ""

# Car 2
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Toyota Fortuner",
  "pricePerDay": 85000,
  "type": "SUV",
  "brand": "Toyota",
  "model": "Fortuner",
  "fuelType": "Diesel",
  "seats": 7,
  "transmission": "Automatique",
  "image": "/assets/fortuner.jpg",
  "description": "Le SUV parfait pour les routes camerounaises. Robuste et spacieux.",
  "location": "Douala, Bonapriso",
  "available": true,
  "maxSpeed": 200,
  "images": ["/assets/fortuner.jpg", "/assets/car6.png", "/assets/car5.png"]
}'
echo ""

# Car 3
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Mercedes GLE 450",
  "pricePerDay": 120000,
  "type": "Luxe",
  "brand": "Mercedes",
  "model": "GLE 450",
  "fuelType": "Hybride",
  "seats": 5,
  "transmission": "Automatique",
  "image": "/assets/mercedes gle.webp",
  "description": "Le summum du luxe et de la technologie.",
  "location": "Yaoundé, Bastos",
  "available": true,
  "maxSpeed": 250,
  "images": ["/assets/car6.png", "/assets/limousine.jpg"]
}'
echo ""

# Car 4
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Limousine Alpha",
  "pricePerDay": 200000,
  "type": "Luxe",
  "brand": "Lincoln",
  "model": "Stretch Limo",
  "fuelType": "Essence",
  "seats": 8,
  "transmission": "Automatique",
  "image": "/assets/limousine.jpg",
  "description": "Pour vos événements les plus prestigieux.",
  "location": "Douala, Akwa",
  "available": true,
  "maxSpeed": 180,
  "images": ["/assets/limousine.jpg"]
}'
echo ""

# Car 5
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Moto Cross",
  "pricePerDay": 25000,
  "type": "Moto",
  "brand": "KTM",
  "model": "Cross 250",
  "fuelType": "Essence",
  "seats": 1,
  "transmission": "Manuelle",
  "image": "/assets/motocross.jpeg",
  "description": "Parfaite pour les terrains difficiles.",
  "location": "Bafoussam",
  "available": true,
  "maxSpeed": 140,
  "images": ["/assets/motocross.jpeg"]
}'
echo ""

# Car 6
curl -X POST $API -H "Content-Type: application/json" -d '{
  "name": "Quad Bike",
  "pricePerDay": 40000,
  "type": "Quad",
  "brand": "Yamaha",
  "model": "Raptor",
  "fuelType": "Essence",
  "seats": 2,
  "transmission": "Automatique",
  "image": "/assets/quad.png",
  "description": "Pour des balades inoubliables en bord de mer.",
  "location": "Kribi",
  "available": true,
  "maxSpeed": 100,
  "images": ["/assets/quad.png"]
}'
echo ""

echo "Done!"
