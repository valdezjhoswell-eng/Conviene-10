# ¿Conviene? — Conviene-10

Aplicación Android para analizar visualmente ofertas de Uber/Cabify mediante captura de pantalla autorizada por Android y OCR. La aplicación no pulsa Aceptar/Rechazar ni controla las apps de transporte.

## Cambios de esta versión
- Corrige el procesamiento de imágenes de `MediaProjection` teniendo en cuenta `rowStride/pixelStride`, evitando cuadros corruptos y bloqueos del OCR.
- Mantiene el visor flotante y muestra explícitamente cuando detecta Uber o Cabify aunque todavía no haya podido leer el precio.
- El análisis de destino consulta la localidad y aplica primero las localidades seleccionadas en el mapa.
- El mapa permite **seleccionar múltiples localidades directamente tocándolas**; tocar nuevamente una localidad la quita.
- Cada localidad puede quedar como Permitida, Precaución o Peligrosa.
- Conserva compatibilidad con las zonas poligonales antiguas.
- GitHub Actions instala Gradle 8.9 y compila sin depender de un `gradlew` guardado en el repositorio.

## Mapa
Google Maps necesita una API key. Para una compilación local, configurarla en `app/build.gradle.kts` o adaptar el proyecto a otro proveedor de mapas.

## Permisos
1. Permitir mostrar sobre otras aplicaciones.
2. Iniciar el visor.
3. Aceptar la captura de pantalla de Android.
4. Mantener activo el servicio mientras se usa Uber/Cabify.

## Compilación
En Android Studio: abrir esta carpeta, sincronizar Gradle y ejecutar `assembleDebug`.

En GitHub Actions: el workflow `.github/workflows/android.yml` usa Java 17 + Gradle 8.9 y deja el APK en el artefacto `CONVIENE-debug`.
