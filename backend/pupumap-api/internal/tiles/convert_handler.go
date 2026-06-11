package tiles

import (
	"encoding/json"
	"net/http"
	"strconv"
)

func TileFromLatLonHandler(w http.ResponseWriter, r *http.Request) {
	latStr := r.URL.Query().Get("lat")
	lonStr := r.URL.Query().Get("lon")
	zoomStr := r.URL.Query().Get("zoom")

	if latStr == "" || lonStr == "" || zoomStr == "" {
		http.Error(w, "missing lat, lon, or zoom query parameter", http.StatusBadRequest)
		return
	}

	lat, err := strconv.ParseFloat(latStr, 64)
	if err != nil {
		http.Error(w, "invalid lat value", http.StatusBadRequest)
		return
	}

	lon, err := strconv.ParseFloat(lonStr, 64)
	if err != nil {
		http.Error(w, "invalid lon value", http.StatusBadRequest)
		return
	}

	zoom, err := strconv.Atoi(zoomStr)
	if err != nil {
		http.Error(w, "invalid zoom value", http.StatusBadRequest)
		return
	}

	tile := LatLonToTile(lat, lon, zoom)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(tile)
}