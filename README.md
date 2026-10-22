# GeoRegistro

Hola, soy Francisca Herrera y esta es mi entrega de la Evaluación N.º 2 de **Taller de Nuevas Tecnologías** (PA26-04724, profesor Pablo Calderón, semestre 2026-2).

## ¿Qué es GeoRegistro?

Es una app Android, hecha en Java, pensada para quien hace inspecciones en terreno y necesita dejar constancia de lo que encuentra. Con ella puedes anotar una incidencia (por ejemplo, un poste caído o una fuga de agua), guardar el lugar exacto donde está, revisar después todo el historial y ver cada incidencia marcada en un mapa.

**Lo que puedes hacer en la app:**

- **Registrar una incidencia:** escribes un título, una descripción y eliges una categoría. La fecha y la hora se agregan solas. La app revisa que no falte nada importante antes de guardar.
- **Guardar dónde ocurrió:** con un botón, la app pide permiso para usar tu ubicación y toma las coordenadas del momento. Si rechazas el permiso, te explica por qué lo necesita y te lleva a los ajustes para activarlo, sin cerrarse.
- **Ver el historial:** las incidencias quedan guardadas en la nube (Cloud Firestore) y se muestran de la más nueva a la más antigua. Si no hay internet o falla algo, la app te avisa en vez de cerrarse.
- **Verlas en el mapa:** cada incidencia aparece como un marcador de Google Maps. Al tocarlo, ves su título, su categoría y cuándo se registró.
- **Modo noche automático:** en la pantalla del mapa uso el sensor de luz del celular. Si estás en un lugar oscuro, el mapa se oscurece solo para no encandilar, y muestra cuántos lux detecta. Si tu celular no tiene ese sensor, la app lo dice y todo lo demás sigue funcionando normal.

## Versión de Android Studio

La desarrollé y probé con esto:

| | Versión |
|---|---|
| Android Studio | Quail 3 · 2026.1.3 |
| Android Gradle Plugin | 9.3.3 |
| `compileSdk` / `targetSdk` | 37 |
| `minSdk` | 27 (Android 8.1) |
| Lenguaje | Java 11 |
| Firebase BoM | 35.0.2 |
| Maps SDK for Android | `play-services-maps` 20.1.0 |

## Cómo ejecutar el proyecto

1. Clona el repositorio:

   ```bash
   git clone https://github.com/Fran-hp/GeoRegistro_Cert02.git
   ```

2. Ábrelo con Android Studio y espera a que Gradle termine de sincronizar.
3. Configura Firebase y la clave de Maps como se explica en la siguiente sección. Esto es importante: no subí mis credenciales, así que sin este paso la app no guarda datos y el mapa se ve gris.
4. Conecta un celular con depuración USB (o abre un emulador con Google Play) y presiona **Run ▶**.

Ten en cuenta que la app necesita **internet** para guardar y leer las incidencias.

## Configuración de Firebase y Google Maps

Por seguridad, **no subí a GitHub mis credenciales**: ni el archivo de Firebase ni la clave de Maps. Para que el proyecto funcione hay que crearlas con tu propia cuenta. Ambos archivos están en el `.gitignore`, así que no se suben por accidente.

### Firebase (Cloud Firestore)

1. Entra a [console.firebase.google.com](https://console.firebase.google.com) y crea un proyecto.
2. Agrega una app **Android** usando como nombre de paquete `com.example.georegistro`.
3. Descarga el archivo `google-services.json` y cópialo dentro de la carpeta **`app/`**, junto a `app/build.gradle.kts`.
4. En **Compilación → Firestore Database**, crea la base de datos.
5. No tienes que crear nada más: las incidencias se guardan en una colección llamada `incidencias`, que aparece sola cuando guardas la primera.

### Google Maps

1. En [console.cloud.google.com](https://console.cloud.google.com), habilita **Maps SDK for Android** y crea una clave de API (APIs y servicios → Credenciales). Te recomiendo restringirla a ese servicio.
2. En la carpeta principal del proyecto, crea o abre el archivo `local.properties` y agrega esta línea, sin comillas ni espacios:

   ```properties
   MAPS_API_KEY=AIzaSyD53rxilqOv3wiD7wqDXBGzITLghxZA-D0
   ```

3. Sincroniza Gradle con **Sync Now**.

El archivo `local.properties` no se sube a GitHub. El proyecto lee la clave desde ahí y la pasa al `AndroidManifest.xml` mediante `manifestPlaceholders` (`${MAPS_API_KEY}`), de modo que la clave nunca queda escrita en el código.




