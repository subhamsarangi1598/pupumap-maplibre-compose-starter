package tiles

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
)

type LayerSummary struct {
	ZoomCategory  string `json:"zoom_category"`
	RoadCount     int    `json:"road_count"`
	PlaceCount    int    `json:"place_count"`
	BuildingCount int    `json:"building_count"`
	WaterCount    int    `json:"water_count"`
}

type TileLayers struct {
	Roads     []RoadFeature     `json:"roads"`
	Places    []PlaceFeature    `json:"places"`
	Buildings []BuildingFeature `json:"buildings"`
	Water     []WaterFeature    `json:"water"`
}

type TileResponse struct {
	Z       int          `json:"z"`
	X       int          `json:"x"`
	Y       int          `json:"y"`
	Bounds  TileBounds   `json:"bounds"`
	Summary LayerSummary `json:"summary"`
	Layers  TileLayers   `json:"layers"`
}

func TileHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		path := strings.TrimPrefix(r.URL.Path, "/tiles/")
		parts := strings.Split(path, "/")

		if len(parts) != 3 {
			http.Error(w, "invalid tile path, expected /tiles/{z}/{x}/{y}", http.StatusBadRequest)
			return
		}

		z, err := strconv.Atoi(parts[0])
		if err != nil {
			http.Error(w, "invalid z value", http.StatusBadRequest)
			return
		}

		x, err := strconv.Atoi(parts[1])
		if err != nil {
			http.Error(w, "invalid x value", http.StatusBadRequest)
			return
		}

		y, err := strconv.Atoi(parts[2])
		if err != nil {
			http.Error(w, "invalid y value", http.StatusBadRequest)
			return
		}

		bounds := TileToBounds(z, x, y)

		roads, err := QueryRoadsInBounds(database, bounds, z)
		if err != nil {
			http.Error(w, "failed to query roads: "+err.Error(), http.StatusInternalServerError)
			return
		}

		places, err := QueryPlacesInBounds(database, bounds, z)
		if err != nil {
			http.Error(w, "failed to query places: "+err.Error(), http.StatusInternalServerError)
			return
		}

		buildings, err := QueryBuildingsInBounds(database, bounds, z)
		if err != nil {
			http.Error(w, "failed to query buildings: "+err.Error(), http.StatusInternalServerError)
			return
		}

		water, err := QueryWaterInBounds(database, bounds, z)
		if err != nil {
			http.Error(w, "failed to query water: "+err.Error(), http.StatusInternalServerError)
			return
		}

		zoomCategory := "high"
		if z <= 8 {
			zoomCategory = "low"
		} else if z <= 12 {
			zoomCategory = "medium"
		}

		response := TileResponse{
			Z:      z,
			X:      x,
			Y:      y,
			Bounds: bounds,
			Summary: LayerSummary{
				ZoomCategory:  zoomCategory,
				RoadCount:     len(roads),
				PlaceCount:    len(places),
				BuildingCount: len(buildings),
				WaterCount:    len(water),
			},
			Layers: TileLayers{
				Roads:     roads,
				Places:    places,
				Buildings: buildings,
				Water:     water,
			},
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
	}
}
