package tiles

import "math"

type TileCoordinate struct {
	Z int `json:"z"`
	X int `json:"x"`
	Y int `json:"y"`
}

func LatLonToTile(lat, lon float64, zoom int) TileCoordinate {
	n := math.Pow(2.0, float64(zoom))

	x := int((lon + 180.0) / 360.0 * n)

	latRad := lat * math.Pi / 180.0
	y := int((1.0 - math.Log(math.Tan(latRad)+1.0/math.Cos(latRad))/math.Pi) / 2.0 * n)

	return TileCoordinate{
		Z: zoom,
		X: x,
		Y: y,
	}
}