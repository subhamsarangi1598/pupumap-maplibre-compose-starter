package tiles

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
)

type DebugTileResponse struct {
	Z           int            `json:"z"`
	X           int            `json:"x"`
	Y           int            `json:"y"`
	Bounds      TileBounds     `json:"bounds"`
	NamedRoads  []RoadFeature  `json:"named_roads"`
	NamedPlaces []PlaceFeature `json:"named_places"`
	NamedWater  []WaterFeature `json:"named_water"`
}

func DebugTileHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		path := strings.TrimPrefix(r.URL.Path, "/tiles-debug/")
		parts := strings.Split(path, "/")

		if len(parts) != 3 {
			http.Error(w, "invalid debug tile path, expected /tiles-debug/{z}/{x}/{y}", http.StatusBadRequest)
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

		water, err := QueryWaterInBounds(database, bounds, z)
		if err != nil {
			http.Error(w, "failed to query water: "+err.Error(), http.StatusInternalServerError)
			return
		}

		namedRoads := make([]RoadFeature, 0)
		for _, road := range roads {
			if road.Name != "" {
				namedRoads = append(namedRoads, road)
			}
		}

		namedPlaces := make([]PlaceFeature, 0)
		for _, place := range places {
			if place.Name != "" {
				namedPlaces = append(namedPlaces, place)
			}
		}

		namedWater := make([]WaterFeature, 0)
		for _, wt := range water {
			if wt.Name != "" {
				namedWater = append(namedWater, wt)
			}
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(DebugTileResponse{
			Z:           z,
			X:           x,
			Y:           y,
			Bounds:      bounds,
			NamedRoads:  namedRoads,
			NamedPlaces: namedPlaces,
			NamedWater:  namedWater,
		})
	}
}
