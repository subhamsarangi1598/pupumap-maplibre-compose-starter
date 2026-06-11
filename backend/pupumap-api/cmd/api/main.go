package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"

	dbconn "com.subham.pupumap/internal/db"
	searchserver "com.subham.pupumap/internal/search"
	tileserver "com.subham.pupumap/internal/tiles"

	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
)

type Place struct {
	ID        int     `json:"id"`
	Name      string  `json:"name"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type CreatePlaceRequest struct {
	Name      string  `json:"name"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type UpdatePlaceRequest struct {
	Name      string  `json:"name"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type OSMPlace struct {
	Name      *string `json:"name"`
	Place     string  `json:"place"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"service": "pupumap-api",
		"status":  "ok",
	})
}

func placeHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodPost:
			var req CreatePlaceRequest
			if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
				http.Error(w, "invalid request body", http.StatusBadRequest)
				return
			}

			if req.Name == "" {
				http.Error(w, "name is required", http.StatusBadRequest)
				return
			}

			var id int
			err := database.QueryRow(`
				INSERT INTO places (name, latitude, longitude)
				VALUES ($1, $2, $3)
				RETURNING id
			`, req.Name, req.Latitude, req.Longitude).Scan(&id)
			if err != nil {
				http.Error(w, "failed to insert place: "+err.Error(), http.StatusInternalServerError)
				return
			}

			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusCreated)
			json.NewEncoder(w).Encode(Place{
				ID:        id,
				Name:      req.Name,
				Latitude:  req.Latitude,
				Longitude: req.Longitude,
			})

		case http.MethodPut:
			idStr := r.URL.Query().Get("id")
			if idStr == "" {
				http.Error(w, "id query param is required", http.StatusBadRequest)
				return
			}

			id, err := strconv.Atoi(idStr)
			if err != nil {
				http.Error(w, "invalid id", http.StatusBadRequest)
				return
			}

			var req UpdatePlaceRequest
			if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
				http.Error(w, "invalid request body", http.StatusBadRequest)
				return
			}

			result, err := database.Exec(`
				UPDATE places
				SET name = $1, latitude = $2, longitude = $3
				WHERE id = $4
			`, req.Name, req.Latitude, req.Longitude, id)
			if err != nil {
				http.Error(w, "failed to update place: "+err.Error(), http.StatusInternalServerError)
				return
			}

			rowsAffected, err := result.RowsAffected()
			if err != nil {
				http.Error(w, "failed to read update result", http.StatusInternalServerError)
				return
			}

			if rowsAffected == 0 {
				http.Error(w, "place not found", http.StatusNotFound)
				return
			}

			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(Place{
				ID:        id,
				Name:      req.Name,
				Latitude:  req.Latitude,
				Longitude: req.Longitude,
			})

		case http.MethodDelete:
			idStr := r.URL.Query().Get("id")
			if idStr == "" {
				http.Error(w, "id query param is required", http.StatusBadRequest)
				return
			}

			id, err := strconv.Atoi(idStr)
			if err != nil {
				http.Error(w, "invalid id", http.StatusBadRequest)
				return
			}

			result, err := database.Exec(`DELETE FROM places WHERE id = $1`, id)
			if err != nil {
				http.Error(w, "failed to delete place: "+err.Error(), http.StatusInternalServerError)
				return
			}

			rowsAffected, err := result.RowsAffected()
			if err != nil {
				http.Error(w, "failed to read delete result", http.StatusInternalServerError)
				return
			}

			if rowsAffected == 0 {
				http.Error(w, "place not found", http.StatusNotFound)
				return
			}

			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(map[string]any{
				"success": true,
				"id":      id,
			})

		default:
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		}
	}
}

func placesHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		rows, err := database.Query(`
			SELECT id, name, latitude, longitude
			FROM places
			ORDER BY id ASC
		`)
		if err != nil {
			http.Error(w, "failed to fetch places: "+err.Error(), http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		places := make([]Place, 0)

		for rows.Next() {
			var place Place
			if err := rows.Scan(&place.ID, &place.Name, &place.Latitude, &place.Longitude); err != nil {
				http.Error(w, "failed to scan place: "+err.Error(), http.StatusInternalServerError)
				return
			}
			places = append(places, place)
		}

		if err := rows.Err(); err != nil {
			http.Error(w, "row iteration error: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(places)
	}
}

func osmPlacesHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		limit := 100

		if limitStr := r.URL.Query().Get("limit"); limitStr != "" {
			parsedLimit, err := strconv.Atoi(limitStr)
			if err == nil && parsedLimit > 0 && parsedLimit <= 1000 {
				limit = parsedLimit
			}
		}

		rows, err := database.Query(`
			SELECT
				name,
				COALESCE(place, '') AS place,
				ST_Y(ST_Transform(way, 4326)) AS latitude,
				ST_X(ST_Transform(way, 4326)) AS longitude
			FROM planet_osm_point
			WHERE place IS NOT NULL
			ORDER BY name NULLS LAST
			LIMIT $1
		`, limit)
		if err != nil {
			http.Error(w, "failed to fetch osm places: "+err.Error(), http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		places := make([]OSMPlace, 0)

		for rows.Next() {
			var place OSMPlace
			if err := rows.Scan(&place.Name, &place.Place, &place.Latitude, &place.Longitude); err != nil {
				http.Error(w, "failed to scan osm place: "+err.Error(), http.StatusInternalServerError)
				return
			}
			places = append(places, place)
		}

		if err := rows.Err(); err != nil {
			http.Error(w, "row iteration error: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(places)
	}
}

func main() {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found. Using system environment variables.")
	}

	serverPort := getEnv("SERVER_PORT", "8080")

	dbPort, err := strconv.Atoi(getEnv("DB_PORT", "5432"))
	if err != nil {
		log.Fatal("Invalid DB_PORT value. DB_PORT must be a number.")
	}

	databaseConfig := dbconn.Config{
		Host:     getEnv("DB_HOST", "localhost"),
		Port:     dbPort,
		User:     getEnv("DB_USER", "postgres"),
		Password: getEnv("DB_PASSWORD", ""),
		Database: getEnv("DB_NAME", "pupumap"),
		SSLMode:  getEnv("DB_SSLMODE", "disable"),
	}

	if databaseConfig.Password == "" {
		log.Fatal("DB_PASSWORD is missing. Add it in your local .env file.")
	}

	database := dbconn.Connect(databaseConfig)
	defer database.Close()

	http.HandleFunc("/health", healthHandler)
	http.HandleFunc("/place", placeHandler(database))
	http.HandleFunc("/places", placesHandler(database))
	http.HandleFunc("/osm/places", osmPlacesHandler(database))
	http.HandleFunc("/api/search", searchserver.Handler(database))

	http.HandleFunc("/tile-from-latlon", tileserver.TileFromLatLonHandler)
	http.HandleFunc("/tiles/", tileserver.TileHandler(database))
	http.HandleFunc("/tiles-vector/", tileserver.VectorTileHandler(database))
	http.HandleFunc("/tiles-debug/", tileserver.DebugTileHandler(database))

	http.Handle("/styles/", http.StripPrefix("/styles/", http.FileServer(http.Dir("web/styles"))))

	fmt.Println("Server started at http://localhost:" + serverPort)
	log.Fatal(http.ListenAndServe(":"+serverPort, nil))
}

func getEnv(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}