# Modern Map UI Design Proposal - Pupumap

To transform Pupumap into a state-of-the-art interactive map experience, we propose a design that blends elegant visuals with a highly responsive, user-friendly overlay.

## Visual Design Reference

Here is a mockup of the proposed modern mobile map interface:

![Map UI Design Mockup](file:///C:/Users/subham/.gemini/antigravity-cli/brain/5a46b4d0-f7f1-4e8d-92ec-ad3443c02ce3/map_ui_design_mockup_1779873145012.png)

---

## 🛠️ Key UI/UX Components

### 1. Floating Search & Filtering Overlay (Top)
* **Floating Search Bar**: A card featuring rounded corners, drop shadows, and a semi-translucent glassmorphic style. It includes search and voice search/mic icons.
* **Category Chips (Horizontal Scroll)**: Quick filter chips placed directly under the search bar (e.g., "All", "Parks", "Food", "Monuments", "Historic"). Selecting a chip instantly filters markers on the map.

### 2. Interactive Place Detail Card (Bottom Sheet)
Instead of standard, hard-to-style MapLibre popup windows, we recommend a Compose-based sliding bottom sheet:
* **Collapsed State**: Shows basic information (Name, Rating, Distance, Category) to avoid obstructing the map.
* **Expanded State**: Displays a cover image (fetched dynamically or placeholder), detailed description, contact information, and review details.
* **Quick Action Row**: Prominent action buttons for **Directions**, **Save/Favorite**, and **Share**.

### 3. Floating Map Controls (Right Center/Bottom)
Floating Action Buttons (FABs) positioned on the right side of the screen:
* **My Location Button**: Re-centers the camera on the user's location with a smooth animation.
* **Zoom In/Out Controls**: Easy zoom control for one-handed operation.
* **Layer/Style Switcher**: Switches map themes (e.g., Light Vector, Dark Vector, Satellite Hybrid, OpenStreetMap classic).

### 4. Custom Map Pins / Markers
* Replace the generic red MapLibre pins with custom-styled vector pins.
* Category-specific pin icons (e.g., a tree icon for parks, a fork/knife icon for food).

---

## 💻 Technical Implementation Strategy

We can structure [MapScreen.kt](file:///C:/Users/subham/Documents/subham-map/android/app/src/main/java/com/subham/pupumap/ui/MapScreen.kt) to place Compose UI components on top of the MapLibre `AndroidView` using a `Box` layout.

### Proposed Code Architecture

```kotlin
@Composable
fun MapScreen() {
    val osmPlaces = remember { mutableStateListOf<OsmPlace>() }
    var selectedPlace by remember { mutableStateOf<OsmPlace?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Bottom Sheet state
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. MapLibre View (Base Layer)
        MapLibreView(
            places = osmPlaces,
            selectedPlace = selectedPlace,
            onMarkerClick = { place ->
                selectedPlace = place
                showBottomSheet = true
            }
        )

        // 2. Search & Category Filters Overlay (Top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            CategoryFilters(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        // 3. Floating Control Panel (Right Side)
        MapControlsColumn(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            onMyLocationClick = { /* Recenter on User Location */ },
            onStyleSwitchClick = { /* Change style.json url */ }
        )

        // 4. Place Details Panel (Bottom Sheet)
        if (showBottomSheet && selectedPlace != null) {
            PlaceDetailBottomSheet(
                place = selectedPlace!!,
                sheetState = sheetState,
                onDismissRequest = { showBottomSheet = false }
            )
        }
    }
}
```

---

## 🎨 Design Theme Parameters

To align with the premium colors defined in [AppTheme.kt](file:///C:/Users/subham/Documents/subham-map/android/app/src/main/java/com/subham/pupumap/theme/AppTheme.kt):

| Light Theme Value | Dark Theme Value | UI Component |
| :--- | :--- | :--- |
| `#F7FAFF` (Soft Light Grey) | `#0B0F14` (Deep Navy Black) | Map background & search bar border |
| `#FFFFFF` (Pure White) | `#121821` (Premium Dark Blue) | Bottom Sheet / Cards background |
| `#1565C0` (Vibrant Cobalt) | `#4FC3F7` (Bright Sky Blue) | Primary button accent (e.g. Directions) |
| `#0288D1` (Midtone Sky Blue) | `#81D4FA` (Secondary Blue) | Active filter chips, toggle states |
