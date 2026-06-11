package tiles

import "math"

type TileBounds struct {
	MinLon float64 `json:"min_lon"`
	MinLat float64 `json:"min_lat"`
	MaxLon float64 `json:"max_lon"`
	MaxLat float64 `json:"max_lat"`
}

func TileToBounds(z, x, y int) TileBounds {
	n := math.Pow(2.0, float64(z))

	minLon := float64(x)/n*360.0 - 180.0
	maxLon := float64(x+1)/n*360.0 - 180.0

	minLat := tileYToLat(float64(y+1), n)
	maxLat := tileYToLat(float64(y), n)

	return TileBounds{
		MinLon: minLon,
		MinLat: minLat,
		MaxLon: maxLon,
		MaxLat: maxLat,
	}
}

func tileYToLat(y, n float64) float64 {
	latRad := math.Atan(math.Sinh(math.Pi * (1 - 2*y/n)))
	return latRad * 180.0 / math.Pi
}