# Cómo iniciar y verificar FintechDigital

## 0. Requisitos
- JDK 21, Node.js 20.19+ (o 22+), IntelliJ IDEA.
- Conexión a Internet la primera vez para descargar dependencias Maven y npm.

## 1. Base de datos
La aplicación trabaja en memoria y no requiere MySQL. Los scripts de
`DataBase/` son de referencia y no se usan aún; al integrar MySQL será
necesario alinear el esquema y reactivar la auto-configuración JPA.

## 2. Ejecutar pruebas del backend

Abre PowerShell en la carpeta raíz del proyecto:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Para un resultado exitoso, la última sección de Maven debe indicar
`BUILD SUCCESS`. Los reportes individuales quedan en
`backend/target/surefire-reports/`. En Linux/macOS:

```sh
cd backend
./mvnw test
```

El resultado real versionado en esta documentación fue 71 pruebas, 0 fallos y
0 errores; consulta `Docs/pruebas/casos_prueba.md`.

## 3. Iniciar el backend (puerto 8080)

**Desde IntelliJ:** ejecuta `FintechDigitalApplication`; no requiere
configuración de base de datos.

**Desde PowerShell:**

```
Set-Location backend
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**

```
cd backend
./mvnw spring-boot:run
```

Comprueba que el servicio responda en http://localhost:8080/api/health. En el
perfil por defecto se cargan datos de demostración en memoria; al reiniciar, los
cambios hechos desde la aplicación se pierden.

## 4. Iniciar el frontend (puerto 5173)

En otra terminal:

```
Set-Location frontend
npm ci
npm run dev
```

Abre http://localhost:5173. El frontend incluye Dashboard conectado a la API y
páginas para usuarios, billeteras, operaciones, historial, notificaciones,
programadas/simulación, recompensas, reversión, alertas/auditoría, analítica,
grafo y benchmark.

La URL por defecto de Axios es `http://localhost:8080/api`. Si el backend usa
otra URL, copia `frontend/.env.example` a `frontend/.env`, configura
`VITE_API_URL` y reinicia Vite.

## 5. Crear el build de producción

Desde `frontend`:

```powershell
npm run build
```

Los archivos de salida se generan en `frontend/dist/`.

## 6. Documentación del proyecto

- Rutas REST: `backend/docs/API.md`
- Casos y resultados reales de pruebas: `Docs/pruebas/casos_prueba.md`
- Diagrama y cambios UML: `Docs/diagrama_uml/`
- Arquitectura, complejidad, benchmark y limitaciones:
  `Docs/informe_tecnico/informe_tecnico.md`
