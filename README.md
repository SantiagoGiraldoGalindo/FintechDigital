# FintechDigital

Plataforma fintech universitaria para gestionar **billeteras digitales, transacciones, recompensas y analítica financiera**.

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.5.7 · Maven · Spring Web, Data JPA, Validation |
| Persistencia actual | En memoria (no requiere MySQL) |
| Frontend | React 19 · Vite · JavaScript · Tailwind CSS 4 · React Router · Axios |
| Control de versiones | Git |

## Estructura

```
FintechDigital/
├── backend/    API REST (com.fintech.backend: controller, model, repository, service, structures, enums, dto, config)
├── frontend/   React + Vite
├── DataBase/   scripts SQL de referencia, aún no usados por la aplicación
├── Docs/       diagramas UML, pruebas e informe técnico
├── INSTRUCCIONES_INICIO.md
└── README.md
```

## Requisitos previos
- JDK 21
- Node.js 20.19 o superior (probado con Node 22)
- MySQL 8.x (opcional; aún no está integrado con la aplicación)
- IntelliJ IDEA (Community o Ultimate) y, opcionalmente, VS Code
- Git (opcional)

## Paso a paso

### 1. Descargar y extraer
Descarga `FintechDigital.zip`, extráelo donde prefieras (por ejemplo `C:\proyectos\`). Debe quedar una carpeta `FintechDigital` con `backend`, `frontend`, etc. directamente dentro.

### 2. Base de datos (no usada aún)

El backend trabaja en memoria y arranca sin configurar ni iniciar MySQL. Los
archivos `DataBase/script.sql` y `DataBase/datos_prueba.sql` son de referencia
y no corresponden todavía al modelo activo de la aplicación.

Para una integración futura, la configuración MySQL/JPA está en
`backend/src/main/resources/application.properties`; se debe quitar allí la
exclusión de auto-configuración indicada en los comentarios y alinear el
esquema antes de usar estos scripts.

<!-- Instrucciones originales para la futura integración:
```
mysql -u root -p < DataBase/script.sql
mysql -u root -p < DataBase/datos_prueba.sql
```
(o abre ambos archivos en MySQL Workbench y ejecútalos en ese orden). -->

### 3. Abrir en IntelliJ IDEA e importar el backend Maven
1. *File > Open* y elige la carpeta `FintechDigital/backend` (donde está `pom.xml`).
   - Si abres la carpeta raíz `FintechDigital`, haz clic derecho sobre `backend/pom.xml` > **Add as Maven Project**.
2. Cuando aparezca la notificación, pulsa **Load Maven Changes** y espera a que se descarguen las dependencias.
3. *File > Project Structure > Project*: selecciona **SDK 21** (descárgalo desde ahí si no lo tienes).
4. Abre `FintechDigitalApplication` y pulsa **Run**. No requiere variables de entorno de base de datos.
5. Comprueba http://localhost:8080/api/health

### 4. Ejecutar el frontend (VS Code o terminal)
```
cd frontend
npm install
npm run dev
```
Abre http://localhost:5173. En VS Code: *File > Open Folder* en `frontend` y usa la terminal integrada. Si cambias la URL del backend, copia `.env.example` a `.env` y edita `VITE_API_URL`.

### 5. Funcionalidades disponibles

La navegación del frontend incluye Dashboard con datos reales de la API,
usuarios, billeteras, operaciones, historial, notificaciones, programadas y
simulación del reloj, recompensas/canjes, reversión, alertas/auditoría,
analítica, grafo de transferencias y benchmark.

La lista completa de rutas está en [`backend/docs/API.md`](backend/docs/API.md).
En el perfil Spring `default` se cargan usuarios, billeteras y transacciones de
demostración. Los datos se mantienen solo mientras el backend está activo.

### 6. Pruebas y documentación

Desde `backend`, ejecuta:

```powershell
.\mvnw.cmd test
```

En Linux/macOS, usa `./mvnw test`. Los casos probados y el resultado real de la
ejecución completa están en [`Docs/pruebas/casos_prueba.md`](Docs/pruebas/casos_prueba.md).
La compilación de producción del frontend se comprueba desde `frontend` con
`npm run build`.

Documentación complementaria:

- [Diagrama UML PlantUML](Docs/diagrama_uml/diagrama_clases.puml) y
  [cambios UML](Docs/diagrama_uml/CAMBIOS_UML.md).
- [Informe técnico](Docs/informe_tecnico/informe_tecnico.md).
- [Benchmark](backend/docs/pruebas/BENCHMARK.md).

## Variables de entorno del backend

| Variable | Defecto | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | Reservada para la futura integración MySQL (no usada actualmente) |
| `DB_PORT` | `3306` | Reservada para la futura integración MySQL (no usada actualmente) |
| `DB_NAME` | `fintechdigital` | Reservada para la futura integración MySQL (no usada actualmente) |
| `DB_USERNAME` | `root` | Reservada para la futura integración MySQL (no usada actualmente) |
| `DB_PASSWORD` | *(vacío)* | Reservada para la futura integración MySQL (no usada actualmente) |
| `SERVER_PORT` | `8080` | Puerto de la API |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Orígenes permitidos (CORS) |

> No guardes contraseñas en el repositorio. `.env` ya está en `.gitignore`.

## Comandos útiles

```
cd backend
./mvnw test                 # pruebas unitarias (Windows: .\mvnw.cmd test)
./mvnw clean package        # genera el JAR
cd ../frontend
npm run build               # build de producción
```

## Notas
- `mvnw` / `mvnw.cmd` son scripts sencillos que descargan Apache Maven 3.9.11 la primera vez. Si prefieres el wrapper oficial, ejecuta en `backend`: `mvn wrapper:wrapper`. IntelliJ ya incluye Maven, así que no los necesitas para abrir el proyecto.
- Los paquetes `structures/*` contienen las implementaciones propias de lista, pila, colas, AVL, tabla hash y grafo; su uso y complejidad están documentados en el informe técnico.
- La aplicación inicia en memoria; MySQL y los scripts de `DataBase/` no se usan aún.
- Más detalle de arranque: ver `INSTRUCCIONES_INICIO.md`.
