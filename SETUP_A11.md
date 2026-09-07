# Puesta en marcha en Galaxy Tab A11

La Galaxy Tab A11 oficial utiliza pantalla 1340×800 y existe en variantes de 4/64 GB y 8/128 GB; las variantes LTE incluyen GPS. El proyecto usa minSdk 26 y está pensado para la orientación horizontal de la tablet.

### 1. Android Studio
Instalar Android Studio en una PC y abrir `RentableZone`.

### 2. SDK
Instalar Android SDK 35 y aceptar las licencias.

### 3. Google Maps
Crear una API key de Maps SDK for Android y sustituir el valor vacío de `manifestPlaceholders["MAPS_API_KEY"]` en `app/build.gradle.kts`.

### 4. APK
Build > Build APK(s).

### 5. Tablet
Copiar el APK a la A11 e instalarlo. Si Android solicita permiso para instalar desde esa fuente, habilitarlo.

### 6. Uso
Abrir Visor Rentable → Configurar superposición → Parámetros y filtros → Mapa y zonas → Iniciar visor → aceptar la captura de pantalla de Android → abrir Uber o Cabify.
