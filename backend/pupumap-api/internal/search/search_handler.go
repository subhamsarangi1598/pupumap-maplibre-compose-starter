package search

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
)

type Result struct {
	OSMID     int64   `json:"osm_id"`
	Name      string  `json:"name"`
	Subtitle  string  `json:"subtitle"`
	Type      string  `json:"type"`
	Source    string  `json:"source"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
	Distance  *int    `json:"distance_meters,omitempty"`
}

type searchCenter struct {
	Latitude  float64
	Longitude float64
	Enabled   bool
}

type tableConfig struct {
	Name     string
	Source   string
	GeomExpr string
	Limit    int
	Priority int
}

var allowedColumns = map[string]bool{
	"name":             true,
	"place":            true,
	"highway":          true,
	"amenity":          true,
	"shop":             true,
	"tourism":          true,
	"historic":         true,
	"man_made":         true,
	"building":         true,
	"addr:housenumber": true,
	"addr:street":      true,
}

func Handler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		query := strings.TrimSpace(r.URL.Query().Get("q"))
		if len(query) < 2 {
			writeJSON(w, []Result{})
			return
		}

		limit := 25
		if limitValue := r.URL.Query().Get("limit"); limitValue != "" {
			parsedLimit, err := strconv.Atoi(limitValue)
			if err == nil && parsedLimit > 0 && parsedLimit <= 50 {
				limit = parsedLimit
			}
		}

		center := parseSearchCenter(r)

		results, err := searchOSM(database, query, limit, center)
		if err != nil {
			http.Error(w, "failed to search osm data: "+err.Error(), http.StatusInternalServerError)
			return
		}

		writeJSON(w, results)
	}
}

func searchOSM(database *sql.DB, query string, limit int, center searchCenter) ([]Result, error) {
	tables := []tableConfig{
		{
			Name:     "planet_osm_point",
			Source:   "point",
			GeomExpr: "way",
			Limit:    limit,
			Priority: 1,
		},
		{
			Name:     "planet_osm_polygon",
			Source:   "polygon",
			GeomExpr: "ST_PointOnSurface(way)",
			Limit:    limit,
			Priority: 2,
		},
		{
			Name:     "planet_osm_line",
			Source:   "line",
			GeomExpr: "ST_PointOnSurface(way)",
			Limit:    limit,
			Priority: 3,
		},
	}

	parts := make([]string, 0, len(tables))
	args := []any{}

	for _, table := range tables {
		columns, err := availableColumns(database, table.Name)
		if err != nil {
			return nil, err
		}

		if len(columns) == 0 {
			continue
		}

		searchArgIndex := len(args) + 1
		prefixArgIndex := len(args) + 2
		exactArgIndex := len(args) + 3
		nextArgIndex := len(args) + 4

		if center.Enabled {
			latArgIndex := nextArgIndex
			lngArgIndex := nextArgIndex + 1
			limitArgIndex := nextArgIndex + 2

			parts = append(parts, fmt.Sprintf("(%s)", tableSQL(table, columns, searchArgIndex, prefixArgIndex, exactArgIndex, latArgIndex, lngArgIndex, limitArgIndex, true)))
			args = append(args, "%"+strings.ToLower(query)+"%", strings.ToLower(query)+"%", strings.ToLower(query), center.Latitude, center.Longitude, table.Limit)
		} else {
			limitArgIndex := nextArgIndex

			parts = append(parts, fmt.Sprintf("(%s)", tableSQL(table, columns, searchArgIndex, prefixArgIndex, exactArgIndex, 0, 0, limitArgIndex, false)))
			args = append(args, "%"+strings.ToLower(query)+"%", strings.ToLower(query)+"%", strings.ToLower(query), table.Limit)
		}
	}

	if len(parts) == 0 {
		return []Result{}, nil
	}

	sqlText := fmt.Sprintf(`
		WITH raw_results AS (
			%s
		)
		SELECT osm_id, name, subtitle, type, source, latitude, longitude, distance_meters
		FROM raw_results
		WHERE name <> ''
		ORDER BY rank ASC, distance_rank ASC, priority ASC, name ASC
		LIMIT %d
	`, strings.Join(parts, "\nUNION ALL\n"), limit)

	rows, err := database.Query(sqlText, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	results := make([]Result, 0)
	for rows.Next() {
		var result Result
		if err := rows.Scan(
			&result.OSMID,
			&result.Name,
			&result.Subtitle,
			&result.Type,
			&result.Source,
			&result.Latitude,
			&result.Longitude,
			&result.Distance,
		); err != nil {
			return nil, err
		}
		results = append(results, result)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return results, nil
}

func tableSQL(table tableConfig, columns map[string]bool, searchArgIndex int, prefixArgIndex int, exactArgIndex int, latArgIndex int, lngArgIndex int, limitArgIndex int, hasCenter bool) string {
	nameExpr := columnExpr(columns, "name")
	placeExpr := columnExpr(columns, "place")
	highwayExpr := columnExpr(columns, "highway")
	amenityExpr := columnExpr(columns, "amenity")
	shopExpr := columnExpr(columns, "shop")
	tourismExpr := columnExpr(columns, "tourism")
	historicExpr := columnExpr(columns, "historic")
	manMadeExpr := columnExpr(columns, "man_made")
	buildingExpr := columnExpr(columns, "building")
	houseNumberExpr := columnExpr(columns, "addr:housenumber")
	streetExpr := columnExpr(columns, "addr:street")

	addressExpr := fmt.Sprintf("NULLIF(TRIM(CONCAT_WS(' ', NULLIF(%s, ''), NULLIF(%s, ''))), '')", houseNumberExpr, streetExpr)
	displayNameExpr := fmt.Sprintf("COALESCE(NULLIF(%s, ''), %s, NULLIF(%s, ''), NULLIF(%s, ''), NULLIF(%s, ''), NULLIF(%s, ''), NULLIF(%s, ''), NULLIF(%s, ''), NULLIF(%s, ''), '')",
		nameExpr,
		addressExpr,
		amenityExpr,
		shopExpr,
		tourismExpr,
		historicExpr,
		manMadeExpr,
		highwayExpr,
		placeExpr,
	)
	typeExpr := fmt.Sprintf(`
		CASE
			WHEN NULLIF(%s, '') IS NOT NULL THEN 'road'
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			WHEN NULLIF(%s, '') IS NOT NULL THEN 'building'
			WHEN NULLIF(%s, '') IS NOT NULL THEN %s
			ELSE '%s'
		END
	`, highwayExpr, amenityExpr, amenityExpr, shopExpr, shopExpr, tourismExpr, tourismExpr, historicExpr, historicExpr, manMadeExpr, manMadeExpr, buildingExpr, placeExpr, placeExpr, table.Source)
	subtitleExpr := fmt.Sprintf(`
		CASE
			WHEN NULLIF(%s, '') IS NOT NULL THEN 'Road'
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			WHEN NULLIF(%s, '') IS NOT NULL THEN 'Building'
			WHEN NULLIF(%s, '') IS NOT NULL THEN INITCAP(REPLACE(%s, '_', ' '))
			ELSE INITCAP('%s')
		END
	`, highwayExpr, amenityExpr, amenityExpr, shopExpr, shopExpr, tourismExpr, tourismExpr, historicExpr, historicExpr, manMadeExpr, manMadeExpr, buildingExpr, placeExpr, placeExpr, table.Source)
	searchTextExpr := fmt.Sprintf("LOWER(CONCAT_WS(' ', %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s))",
		nameExpr,
		placeExpr,
		highwayExpr,
		amenityExpr,
		shopExpr,
		tourismExpr,
		historicExpr,
		manMadeExpr,
		buildingExpr,
		houseNumberExpr,
		streetExpr,
	)
	categoryRankExpr := categoryRankExpression(exactArgIndex, amenityExpr, shopExpr, tourismExpr, highwayExpr, buildingExpr)
	distanceExpr := "NULL::integer"
	distanceRankExpr := "999999999"
	if hasCenter {
		userPointExpr := fmt.Sprintf("ST_Transform(ST_SetSRID(ST_MakePoint($%d, $%d), 4326), 3857)", lngArgIndex, latArgIndex)
		distanceExpr = fmt.Sprintf("ROUND(ST_Distance(%s, %s))::integer", table.GeomExpr, userPointExpr)
		distanceRankExpr = distanceExpr
	}

	return fmt.Sprintf(`
		SELECT
			osm_id,
			%s AS name,
			%s AS subtitle,
			%s AS type,
			'%s' AS source,
			ST_Y(ST_Transform(%s, 4326)) AS latitude,
			ST_X(ST_Transform(%s, 4326)) AS longitude,
			%s AS distance_meters,
			CASE
				WHEN LOWER(%s) = $%d THEN 0
				WHEN LOWER(%s) LIKE $%d THEN 1
				WHEN %s THEN 2
				WHEN LOWER(%s) LIKE $%d THEN 3
				ELSE 8
			END AS rank,
			%s AS distance_rank,
			%d AS priority
		FROM %s
		WHERE (%s LIKE $%d OR %s)
			AND %s IS NOT NULL
			AND ST_IsEmpty(%s) = false
		ORDER BY rank ASC, distance_rank ASC, priority ASC, name ASC
		LIMIT $%d
	`,
		displayNameExpr,
		subtitleExpr,
		typeExpr,
		table.Source,
		table.GeomExpr,
		table.GeomExpr,
		distanceExpr,
		displayNameExpr,
		exactArgIndex,
		displayNameExpr,
		prefixArgIndex,
		categoryRankExpr,
		displayNameExpr,
		searchArgIndex,
		distanceRankExpr,
		table.Priority,
		table.Name,
		searchTextExpr,
		searchArgIndex,
		categoryRankExpr,
		table.GeomExpr,
		table.GeomExpr,
		limitArgIndex,
	)
}

func categoryRankExpression(queryArgIndex int, amenityExpr string, shopExpr string, tourismExpr string, highwayExpr string, buildingExpr string) string {
	return fmt.Sprintf(`
		(
			($%d LIKE 'hospital%%' AND %s IN ('hospital', 'clinic', 'doctors'))
			OR ($%d LIKE 'clinic%%' AND %s IN ('clinic', 'hospital', 'doctors'))
			OR ($%d LIKE 'doctor%%' AND %s IN ('doctors', 'clinic', 'hospital'))
			OR ($%d LIKE 'fuel%%' AND %s = 'fuel')
			OR ($%d LIKE 'petrol%%' AND %s = 'fuel')
			OR ($%d LIKE 'food%%' AND (%s IN ('restaurant', 'fast_food', 'cafe') OR %s IN ('bakery', 'convenience')))
			OR ($%d LIKE 'restaurant%%' AND %s IN ('restaurant', 'fast_food', 'cafe'))
			OR ($%d LIKE 'atm%%' AND %s = 'atm')
			OR ($%d LIKE 'bank%%' AND %s IN ('bank', 'atm'))
			OR ($%d LIKE 'hotel%%' AND %s IN ('hotel', 'guest_house', 'hostel'))
			OR ($%d LIKE 'road%%' AND NULLIF(%s, '') IS NOT NULL)
			OR ($%d LIKE 'building%%' AND NULLIF(%s, '') IS NOT NULL)
		)
	`, queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr, shopExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, amenityExpr,
		queryArgIndex, tourismExpr,
		queryArgIndex, highwayExpr,
		queryArgIndex, buildingExpr)
}

func parseSearchCenter(r *http.Request) searchCenter {
	latValue := strings.TrimSpace(r.URL.Query().Get("lat"))
	lngValue := strings.TrimSpace(r.URL.Query().Get("lng"))
	if latValue == "" || lngValue == "" {
		return searchCenter{}
	}

	latitude, latErr := strconv.ParseFloat(latValue, 64)
	longitude, lngErr := strconv.ParseFloat(lngValue, 64)
	if latErr != nil || lngErr != nil {
		return searchCenter{}
	}

	if latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180 {
		return searchCenter{}
	}

	return searchCenter{
		Latitude:  latitude,
		Longitude: longitude,
		Enabled:   true,
	}
}

func availableColumns(database *sql.DB, tableName string) (map[string]bool, error) {
	rows, err := database.Query(`
		SELECT column_name
		FROM information_schema.columns
		WHERE table_schema = 'public'
			AND table_name = $1
	`, tableName)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	columns := map[string]bool{}
	for rows.Next() {
		var column string
		if err := rows.Scan(&column); err != nil {
			return nil, err
		}
		if allowedColumns[column] {
			columns[column] = true
		}
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return columns, nil
}

func columnExpr(columns map[string]bool, column string) string {
	if !columns[column] {
		return "''"
	}
	return fmt.Sprintf(`COALESCE(%s, '')`, quoteIdent(column))
}

func quoteIdent(column string) string {
	return `"` + strings.ReplaceAll(column, `"`, `""`) + `"`
}

func writeJSON(w http.ResponseWriter, value any) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(value)
}
