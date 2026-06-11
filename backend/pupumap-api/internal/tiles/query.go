package tiles

import (
	"database/sql"
)

type RoadFeature struct {
	ID      int64  `json:"id"`
	Name    string `json:"name"`
	Highway string `json:"highway"`
}

type PlaceFeature struct {
	ID      int64  `json:"id"`
	Name    string `json:"name"`
	Place   string `json:"place"`
	Amenity string `json:"amenity"`
	Shop    string `json:"shop"`
}

type BuildingFeature struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	Building string `json:"building"`
}

type WaterFeature struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	Waterway string `json:"waterway"`
	Natural  string `json:"natural"`
	Landuse  string `json:"landuse"`
}

func QueryRoadsInBounds(database *sql.DB, bounds TileBounds, zoom int) ([]RoadFeature, error) {
	var rows *sql.Rows
	var err error

	if zoom <= 8 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(highway, '')
			FROM planet_osm_roads
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND highway IN ('trunk', 'primary', 'secondary')
			AND name IS NOT NULL
			AND name <> ''
			LIMIT 50
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else if zoom <= 12 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(highway, '')
			FROM planet_osm_roads
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND (
				(highway IN ('primary', 'secondary') AND name IS NOT NULL AND name <> '')
				OR (highway = 'trunk')
			)
			LIMIT 35
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(highway, '')
			FROM planet_osm_roads
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND highway IN (
				'trunk', 'trunk_link',
				'primary', 'primary_link',
				'secondary', 'secondary_link',
				'tertiary', 'tertiary_link',
				'residential', 'service', 'living_street'
			)
			LIMIT 80
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	}

	if err != nil {
		return nil, err
	}
	defer rows.Close()

	roads := make([]RoadFeature, 0)

	for rows.Next() {
		var road RoadFeature
		if err := rows.Scan(&road.ID, &road.Name, &road.Highway); err != nil {
			return nil, err
		}
		roads = append(roads, road)
	}

	return roads, rows.Err()
}

func QueryPlacesInBounds(database *sql.DB, bounds TileBounds, zoom int) ([]PlaceFeature, error) {
	var rows *sql.Rows
	var err error

	if zoom <= 8 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(place, ''),
				COALESCE(amenity, ''),
				COALESCE(shop, '')
			FROM planet_osm_point
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND place IN ('city', 'town', 'suburb')
			AND name IS NOT NULL
			AND name <> ''
			LIMIT 50
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else if zoom <= 12 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(place, ''),
				COALESCE(amenity, ''),
				COALESCE(shop, '')
			FROM planet_osm_point
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND (
				place IN ('city', 'town', 'suburb', 'neighbourhood', 'locality')
				OR amenity IN ('school', 'bank', 'fuel')
				OR shop IN ('supermarket')
				OR (amenity = 'hospital' AND name IS NOT NULL AND name <> '')
			)
			AND name IS NOT NULL
			AND name <> ''
			LIMIT 35
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(place, ''),
				COALESCE(amenity, ''),
				COALESCE(shop, '')
			FROM planet_osm_point
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND (
				place IN ('neighbourhood', 'locality', 'suburb')
				OR amenity IN ('hospital', 'clinic', 'doctors', 'bank', 'fuel', 'restaurant', 'fast_food')
				OR shop IN ('supermarket', 'convenience')
			)
			AND name IS NOT NULL
			AND name <> ''
			LIMIT 60
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	}

	if err != nil {
		return nil, err
	}
	defer rows.Close()

	places := make([]PlaceFeature, 0)

	for rows.Next() {
		var place PlaceFeature
		if err := rows.Scan(&place.ID, &place.Name, &place.Place, &place.Amenity, &place.Shop); err != nil {
			return nil, err
		}
		places = append(places, place)
	}

	return places, rows.Err()
}

func QueryBuildingsInBounds(database *sql.DB, bounds TileBounds, zoom int) ([]BuildingFeature, error) {
	var rows *sql.Rows
	var err error

	if zoom <= 8 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(building, '')
			FROM planet_osm_polygon
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND building IS NOT NULL
			AND building <> ''
			AND name IS NOT NULL
			AND name <> ''
			AND building IN ('hospital', 'hotel', 'commercial', 'office', 'stadium', 'school', 'temple')
			LIMIT 20
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else if zoom <= 12 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(building, '')
			FROM planet_osm_polygon
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND building IS NOT NULL
			AND building <> ''
			AND (
				(name IS NOT NULL AND name <> '')
				OR building IN ('hospital', 'school', 'commercial', 'residential', 'apartments', 'hotel', 'retail', 'office')
			)
			LIMIT 50
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, ''),
				COALESCE(building, '')
			FROM planet_osm_polygon
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND building IS NOT NULL
			AND building <> ''
			AND (
				(name IS NOT NULL AND name <> '')
				OR building IN (
					'hospital', 'clinic', 'school', 'college',
					'commercial', 'residential', 'apartments',
					'hotel', 'retail', 'office', 'house',
					'temple', 'dormitory', 'healthcare'
				)
			)
			LIMIT 80
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	}

	if err != nil {
		return nil, err
	}
	defer rows.Close()

	buildings := make([]BuildingFeature, 0)

	for rows.Next() {
		var building BuildingFeature
		if err := rows.Scan(&building.ID, &building.Name, &building.Building); err != nil {
			return nil, err
		}
		buildings = append(buildings, building)
	}

	return buildings, rows.Err()
}

func QueryWaterInBounds(database *sql.DB, bounds TileBounds, zoom int) ([]WaterFeature, error) {
	var rows *sql.Rows
	var err error

	if zoom <= 8 {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				'' AS waterway,
				COALESCE("natural", '') AS natural_value,
				COALESCE(landuse, '') AS landuse
			FROM planet_osm_polygon
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND (
				"natural" = 'water'
				OR landuse = 'reservoir'
			)
			AND (
				(name IS NOT NULL AND name <> '')
				OR landuse = 'reservoir'
			)
			LIMIT 30
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	} else {
		rows, err = database.Query(`
			SELECT
				osm_id,
				COALESCE(name, '') AS name,
				'' AS waterway,
				COALESCE("natural", '') AS natural_value,
				COALESCE(landuse, '') AS landuse
			FROM planet_osm_polygon
			WHERE way && ST_Transform(
				ST_MakeEnvelope($1, $2, $3, $4, 4326),
				3857
			)
			AND (
				"natural" = 'water'
				OR landuse = 'reservoir'
			)
			LIMIT 50
		`,
			bounds.MinLon,
			bounds.MinLat,
			bounds.MaxLon,
			bounds.MaxLat,
		)
	}

	if err != nil {
		return nil, err
	}
	defer rows.Close()

	waters := make([]WaterFeature, 0)

	for rows.Next() {
		var water WaterFeature
		if err := rows.Scan(
			&water.ID,
			&water.Name,
			&water.Waterway,
			&water.Natural,
			&water.Landuse,
		); err != nil {
			return nil, err
		}
		waters = append(waters, water)
	}

	return waters, rows.Err()
}