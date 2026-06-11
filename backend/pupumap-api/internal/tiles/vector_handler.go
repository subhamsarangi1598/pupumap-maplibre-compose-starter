package tiles

import (
	"database/sql"
	"net/http"
	"strconv"
	"strings"
)

func VectorTileHandler(database *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		path := strings.TrimPrefix(r.URL.Path, "/tiles-vector/")
		path = strings.TrimSuffix(path, ".pbf")
		parts := strings.Split(path, "/")

		if len(parts) != 3 {
			http.Error(w, "invalid vector tile path, expected /tiles-vector/{z}/{x}/{y}.pbf", http.StatusBadRequest)
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

		tileData, err := GenerateVectorTile(database, z, x, y)
		if err != nil {
			http.Error(w, "failed to generate vector tile: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/x-protobuf")
		w.WriteHeader(http.StatusOK)
		w.Write(tileData)
	}
}

func GenerateVectorTile(database *sql.DB, z, x, y int) ([]byte, error) {
	var tileData []byte
	addressCTEs, addressMVT := BuildAddressTileSQL(database)

	queryTemplate := `
		WITH
		tile AS (
			SELECT ST_TileEnvelope($1, $2, $3) AS geom
		),

		roads AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(highway, '') AS highway,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_roads, tile
			WHERE way && tile.geom
			AND highway IS NOT NULL
			AND highway <> ''
		),

		detailed_roads AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(highway, '') AS highway,
				COALESCE(bridge, '') AS bridge,
				COALESCE(tunnel, '') AS tunnel,
				COALESCE(oneway, '') AS oneway,
				COALESCE("layer", '0') AS layer,
				CASE
					WHEN highway IN ('motorway', 'motorway_link', 'trunk', 'trunk_link') THEN 1
					WHEN highway IN ('primary', 'primary_link') THEN 2
					WHEN highway IN ('secondary', 'secondary_link') THEN 3
					WHEN highway IN ('tertiary', 'tertiary_link') THEN 4
					WHEN highway IN ('unclassified', 'residential', 'living_street') THEN 5
					WHEN highway IN ('service', 'road') THEN 6
					WHEN highway = 'track' THEN 7
					ELSE 9
				END AS road_rank,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_line, tile
			WHERE way && tile.geom
			AND highway IS NOT NULL
			AND highway <> ''
			AND (
				($1 <= 10 AND highway IN ('motorway', 'trunk', 'primary', 'secondary'))
				OR ($1 > 10 AND $1 <= 13 AND highway IN (
					'motorway', 'motorway_link',
					'trunk', 'trunk_link',
					'primary', 'primary_link',
					'secondary', 'secondary_link',
					'tertiary', 'tertiary_link',
					'unclassified', 'residential'
				))
				OR ($1 > 13 AND highway IN (
					'motorway', 'motorway_link',
					'trunk', 'trunk_link',
					'primary', 'primary_link',
					'secondary', 'secondary_link',
					'tertiary', 'tertiary_link',
					'unclassified', 'residential', 'living_street',
					'service', 'road', 'track'
				))
			)
			ORDER BY road_rank ASC, name ASC
			LIMIT 500
		),

		path_lines AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(highway, '') AS highway,
				COALESCE(bridge, '') AS bridge,
				COALESCE(tunnel, '') AS tunnel,
				COALESCE("layer", '0') AS layer,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_line, tile
			WHERE way && tile.geom
			AND $1 >= 14
			AND highway IN ('footway', 'path', 'pedestrian', 'steps', 'cycleway', 'bridleway')
			LIMIT 250
		),

		railways AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(railway, '') AS railway,
				COALESCE(bridge, '') AS bridge,
				COALESCE(tunnel, '') AS tunnel,
				COALESCE("layer", '0') AS layer,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_line, tile
			WHERE way && tile.geom
			AND $1 >= 9
			AND railway IN ('rail', 'light_rail', 'subway', 'tram', 'monorail')
			LIMIT 180
		),

		landuse_areas AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(landuse, '') AS landuse,
				COALESCE(amenity, '') AS amenity,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND $1 >= 8
			AND landuse IN (
				'residential', 'commercial', 'retail',
				'industrial', 'education', 'institutional',
				'cemetery', 'farmland', 'farmyard', 'orchard'
			)
			LIMIT 180
		),

		green_areas AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(leisure, '') AS leisure,
				COALESCE(landuse, '') AS landuse,
				COALESCE("natural", '') AS natural_value,
				COALESCE(boundary, '') AS boundary,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND $1 >= 6
			AND (
				leisure IN ('park', 'garden', 'nature_reserve', 'playground', 'sports_centre', 'pitch', 'golf_course')
				OR landuse IN ('forest', 'recreation_ground', 'grass', 'meadow', 'village_green', 'allotments', 'farmland', 'orchard')
				OR "natural" IN ('wood', 'grassland', 'scrub', 'heath')
				OR boundary = 'national_park'
			)
			LIMIT 160
		),

		buildings AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(building, '') AS building,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND $1 >= 14
			AND building IS NOT NULL
			AND building <> ''
			LIMIT 450
		),

		boundary_lines AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(boundary, '') AS boundary,
				COALESCE(admin_level, '') AS admin_level,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_line, tile
			WHERE way && tile.geom
			AND boundary = 'administrative'
			AND (
				($1 <= 7 AND admin_level IN ('2', '4'))
				OR ($1 > 7 AND $1 <= 11 AND admin_level IN ('4', '5', '6'))
				OR ($1 > 11 AND admin_level IN ('4', '5', '6', '7', '8', '9', '10'))
			)
			LIMIT 160
		),

		boundary_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(boundary, '') AS boundary,
				COALESCE(admin_level, '') AS admin_level,
				ST_AsMVTGeom(
					ST_PointOnSurface(way),
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND boundary = 'administrative'
			AND name IS NOT NULL
			AND name <> ''
			AND (
				($1 <= 7 AND admin_level IN ('2', '4'))
				OR ($1 > 7 AND $1 <= 11 AND admin_level IN ('4', '5', '6'))
				OR ($1 > 11 AND admin_level IN ('6', '7', '8', '9', '10'))
			)
			LIMIT 60
		),

		water AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE("natural", '') AS natural_value,
				COALESCE(landuse, '') AS landuse,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND (
				"natural" = 'water'
				OR landuse = 'reservoir'
			)
		),

		water_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE("natural", '') AS natural_value,
				COALESCE(landuse, '') AS landuse,
				ST_AsMVTGeom(
					ST_PointOnSurface(way),
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND (
				"natural" = 'water'
				OR landuse = 'reservoir'
			)
			AND $1 >= 8
			LIMIT 30
		),

		park_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(leisure, '') AS leisure,
				COALESCE(landuse, '') AS landuse,
				COALESCE("natural", '') AS natural_value,
				COALESCE(boundary, '') AS boundary,
				ST_AsMVTGeom(
					ST_PointOnSurface(way),
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND $1 >= 11
			AND (
				leisure IN ('park', 'garden', 'nature_reserve', 'playground', 'sports_centre')
				OR landuse IN ('forest', 'recreation_ground', 'grass', 'meadow')
				OR "natural" IN ('wood', 'grassland')
				OR boundary = 'national_park'
			)
			LIMIT 45
		),

		places AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(place, '') AS place,
				CASE
					WHEN place = 'country' THEN 1
					WHEN place = 'state' THEN 2
					WHEN place = 'region' THEN 3
					WHEN place = 'city' THEN 4
					WHEN place = 'town' THEN 5
					WHEN place = 'village' THEN 6
					WHEN place = 'suburb' THEN 7
					WHEN place = 'neighbourhood' THEN 8
					WHEN place = 'locality' THEN 9
					WHEN place = 'hamlet' THEN 10
					ELSE 99
				END AS label_rank,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_point, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND (
				($1 <= 6 AND place IN ('country', 'state', 'region', 'city'))
				OR ($1 > 6 AND $1 <= 9 AND place IN ('city', 'town'))
				OR ($1 > 9 AND $1 <= 12 AND place IN ('city', 'town', 'village', 'suburb'))
				OR ($1 > 12 AND place IN ('town', 'village', 'suburb', 'neighbourhood', 'locality', 'hamlet'))
			)
			ORDER BY label_rank ASC, name ASC
			LIMIT 80
		),

		pois AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(amenity, '') AS amenity,
				COALESCE(shop, '') AS shop,
				COALESCE(tourism, '') AS tourism,
				COALESCE(historic, '') AS historic,
				COALESCE(railway, '') AS railway,
				CASE
					WHEN amenity IN ('hospital', 'clinic', 'doctors') THEN 1
					WHEN amenity IN ('school', 'college') THEN 2
					WHEN amenity IN ('bank', 'fuel') THEN 3
					WHEN shop IN ('supermarket', 'mall', 'department_store') THEN 4
					WHEN amenity IN ('restaurant', 'fast_food', 'cafe') THEN 5
					WHEN railway IN ('station', 'halt', 'tram_stop', 'subway_entrance') THEN 6
					WHEN tourism <> '' OR historic <> '' THEN 6
					WHEN shop <> '' THEN 7
					ELSE 9
				END AS label_rank,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_point, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND $1 >= 13
			AND (
				amenity IN (
					'hospital', 'clinic', 'doctors', 'pharmacy',
					'school', 'college', 'university', 'library',
					'bank', 'atm', 'fuel', 'post_office', 'police',
					'fire_station', 'bus_station', 'taxi',
					'restaurant', 'fast_food', 'cafe', 'bar',
					'place_of_worship', 'community_centre', 'townhall'
				)
				OR shop <> ''
				OR tourism <> ''
				OR historic <> ''
				OR railway IN ('station', 'halt', 'tram_stop', 'subway_entrance')
			)
			ORDER BY label_rank ASC, name ASC
			LIMIT 150
		),

		landmark_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(tourism, '') AS tourism,
				COALESCE(historic, '') AS historic,
				COALESCE(man_made, '') AS man_made,
				COALESCE(amenity, '') AS amenity,
				ST_AsMVTGeom(
					way,
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_point, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND $1 >= 11
			AND (
				tourism IN ('attraction', 'museum', 'hotel', 'viewpoint', 'guest_house', 'zoo')
				OR historic IN ('monument', 'memorial', 'castle', 'ruins', 'archaeological_site')
				OR man_made IN ('tower', 'water_tower', 'lighthouse', 'bridge')
				OR amenity IN ('place_of_worship', 'townhall', 'courthouse')
			)
			LIMIT 80
		),

		landmark_area_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(tourism, '') AS tourism,
				COALESCE(historic, '') AS historic,
				COALESCE(man_made, '') AS man_made,
				COALESCE(amenity, '') AS amenity,
				ST_AsMVTGeom(
					ST_PointOnSurface(way),
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND $1 >= 11
			AND (
				tourism IN ('attraction', 'museum', 'hotel', 'viewpoint', 'guest_house', 'zoo')
				OR historic IN ('monument', 'memorial', 'castle', 'ruins', 'archaeological_site')
				OR man_made IN ('tower', 'water_tower', 'lighthouse', 'bridge')
				OR amenity IN ('place_of_worship', 'townhall', 'courthouse')
			)
			LIMIT 80
		),

		building_labels AS (
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				COALESCE(building, '') AS building,
				ST_AsMVTGeom(
					ST_PointOnSurface(way),
					tile.geom,
					4096,
					256,
					true
				) AS geom
			FROM planet_osm_polygon, tile
			WHERE way && tile.geom
			AND name IS NOT NULL
			AND name <> ''
			AND building IS NOT NULL
			AND building <> ''
			AND $1 >= 15
			LIMIT 60
		)
		__ADDRESS_CTES__

		SELECT
			COALESCE((SELECT ST_AsMVT(roads, 'roads', 4096, 'geom') FROM roads), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(detailed_roads, 'detailed_roads', 4096, 'geom') FROM detailed_roads), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(path_lines, 'path_lines', 4096, 'geom') FROM path_lines), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(railways, 'railways', 4096, 'geom') FROM railways), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(landuse_areas, 'landuse_areas', 4096, 'geom') FROM landuse_areas), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(green_areas, 'green_areas', 4096, 'geom') FROM green_areas), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(buildings, 'buildings', 4096, 'geom') FROM buildings), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(boundary_lines, 'boundary_lines', 4096, 'geom') FROM boundary_lines), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(boundary_labels, 'boundary_labels', 4096, 'geom') FROM boundary_labels), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(water, 'water', 4096, 'geom') FROM water), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(water_labels, 'water_labels', 4096, 'geom') FROM water_labels), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(park_labels, 'park_labels', 4096, 'geom') FROM park_labels), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(places, 'places', 4096, 'geom') FROM places), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(pois, 'pois', 4096, 'geom') FROM pois), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(landmark_labels, 'landmark_labels', 4096, 'geom') FROM landmark_labels), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(landmark_area_labels, 'landmark_area_labels', 4096, 'geom') FROM landmark_area_labels), ''::bytea) ||
			COALESCE((SELECT ST_AsMVT(building_labels, 'building_labels', 4096, 'geom') FROM building_labels), ''::bytea)
			__ADDRESS_MVT__
	`
	query := strings.ReplaceAll(queryTemplate, "__ADDRESS_CTES__", addressCTEs)
	query = strings.ReplaceAll(query, "__ADDRESS_MVT__", addressMVT)

	err := database.QueryRow(query, z, x, y).Scan(&tileData)
	if err != nil {
		return nil, err
	}

	return tileData, nil
}

func BuildAddressTileSQL(database *sql.DB) (string, string) {
	addressSources := make([]string, 0, 2)

	if HasColumns(database, "planet_osm_point", "addr:housenumber", "addr:street") {
		addressSources = append(addressSources, `
				SELECT
					osm_id,
					TRIM(
						COALESCE("addr:housenumber", '') ||
						CASE
							WHEN COALESCE("addr:street", '') <> '' THEN ' ' || COALESCE("addr:street", '')
							ELSE ''
						END
					) AS address,
					ST_AsMVTGeom(
						way,
						tile.geom,
						4096,
						256,
						true
					) AS geom
				FROM planet_osm_point, tile
				WHERE way && tile.geom
				AND $1 >= 16
				AND COALESCE("addr:housenumber", '') <> ''`)
	}

	if HasColumns(database, "planet_osm_polygon", "addr:housenumber", "addr:street") {
		addressSources = append(addressSources, `
				SELECT
					osm_id,
					TRIM(
						COALESCE("addr:housenumber", '') ||
						CASE
							WHEN COALESCE("addr:street", '') <> '' THEN ' ' || COALESCE("addr:street", '')
							ELSE ''
						END
					) AS address,
					ST_AsMVTGeom(
						ST_PointOnSurface(way),
						tile.geom,
						4096,
						256,
						true
					) AS geom
				FROM planet_osm_polygon, tile
				WHERE way && tile.geom
				AND $1 >= 16
				AND COALESCE("addr:housenumber", '') <> ''`)
	}

	if len(addressSources) == 0 {
		return "", ""
	}

	addressCTEs := `,

		address_labels AS (
			SELECT *
			FROM (
` + strings.Join(addressSources, "\n				UNION ALL\n") + `
			) addresses
			LIMIT 120
		)`

	addressMVT := ` ||
			COALESCE((SELECT ST_AsMVT(address_labels, 'address_labels', 4096, 'geom') FROM address_labels), ''::bytea)`

	return addressCTEs, addressMVT
}

func HasColumns(database *sql.DB, tableName string, columnNames ...string) bool {
	for _, columnName := range columnNames {
		var exists bool
		err := database.QueryRow(`
			SELECT EXISTS (
				SELECT 1
				FROM information_schema.columns
				WHERE table_schema = 'public'
				AND table_name = $1
				AND column_name = $2
			)
		`, tableName, columnName).Scan(&exists)
		if err != nil || !exists {
			return false
		}
	}

	return true
}
