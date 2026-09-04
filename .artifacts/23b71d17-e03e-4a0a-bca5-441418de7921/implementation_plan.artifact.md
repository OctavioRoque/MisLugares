# Plan de Implementación: Funcionalidad de Lugares Cercanos (Overpass API)

Este plan detalla la adición de una funcionalidad para buscar y mostrar puntos de interés (POIs) cercanos a la ubicación del usuario utilizando OpenStreetMap a través de Overpass API.

## User Review Required

> [!IMPORTANT]
> **Modelo de Datos:** Se creará `LugarCercano` para los datos de la API. Se mantendrá la compatibilidad con el constructor de `Lugar` (longitud antes que latitud) al convertir un `LugarCercano` a `Lugar` persistente.
> **Categorías:** Se mapearán categorías de OSM (amenity, leisure, tourism) a los valores existentes de `TipoLugar` para mantener la consistencia visual y de datos.
> **Navegación:** Se añadirá un botón "BUSCAR CERCANOS" en la `MainActivity` debajo de "MIS FAVORITOS".

## Proposed Changes

### [Data Layer]

#### [NEW] [LugarCercano.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/data/LugarCercano.kt)
- Data class para representar los POIs de Overpass API.
- Campos: `osmId`, `nombre`, `categoria`, `geoPunto`, `direccion`, `distanciaMetros`.

#### [NEW] [OverpassApiService.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/data/OverpassApiService.kt)
- Interfaz Retrofit para definir los endpoints de Overpass.
- Incluye DTOs (`OverpassResponse`, `Element`) para el parseo del JSON.
- Implementación de la construcción de la query Overpass QL dinámica.

#### [NEW] [LugaresCercanosRepository.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/data/LugaresCercanosRepository.kt)
- Gestiona las llamadas a `OverpassApiService`.
- Implementa lógica de reintento/fallback entre servidores (`overpass-api.de` y `overpass.kumi.systems`).
- Cache simple en memoria para evitar llamadas redundantes en la misma sesión.

---

### [UI Layer]

#### [NEW] [LugaresCercanosViewModel.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/ui/LugaresCercanosViewModel.kt)
- Expone `LiveData<List<LugarCercano>>` y estados de carga/error.
- Función `buscarLugaresCercanos(lat: Double, lon: Double)` que interactúa con el repositorio.

#### [NEW] [LugaresCercanosActivity.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/ui/LugaresCercanosActivity.kt)
- Pantalla con `RecyclerView` que muestra los resultados.
- Implementa la navegación a `EdicionLugarActivity` prellenando los datos cuando el usuario selecciona un POI para guardarlo.

#### [NEW] [LugaresCercanosAdapter.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/ui/LugaresCercanosAdapter.kt)
- Adapter para el `RecyclerView`, siguiendo el estilo de `LugaresAdapter`.
- Calcula la distancia usando `Location.distanceBetween` para mostrarla en tiempo real.

#### [NEW] [activity_lugares_cercanos.xml](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/res/layout/activity_lugares_cercanos.xml)
- Layout con `Toolbar`, `ProgressBar` para carga y `RecyclerView`.

#### [NEW] [item_lugar_cercano.xml](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/res/layout/item_lugar_cercano.xml)
- Layout de fila similar a `item_lugar.xml` pero optimizado para datos de OSM (con botón de "Añadir").

---

### [Integration]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/java/com/example/mislugares/MainActivity.kt)
- Añadir listener para el nuevo botón `btnCercanos`.
- Manejo de navegación a `LugaresCercanosActivity`.

#### [MODIFY] [activity_main.xml](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/res/layout/activity_main.xml)
- Insertar `btnCercanos` en el diseño.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/t4vit/AndroidStudioProjects/MisLugares/app/src/main/AndroidManifest.xml)
- Registrar `LugaresCercanosActivity`.

## Verification Plan

### Automated Tests
- No se requieren tests automatizados específicos para esta fase, pero se verificará la compilación y el sync de Gradle.

### Manual Verification
1.  **Carga de API:** Abrir la nueva pantalla y verificar que se muestran lugares reales de OpenStreetMap.
2.  **Fallback:** Simular fallo de red o timeout para verificar que intenta usar el servidor alternativo.
3.  **Conversión:** Seleccionar un lugar cercano y verificar que abre `EdicionLugarActivity` con los campos correctamente mapeados (nombre, dirección, coordenadas).
4.  **Distancia:** Verificar que la distancia se muestra correctamente y se actualiza al cambiar la ubicación (si es posible simularlo).
