# ComicApiDod - Data-Oriented Design Implementation

This project is a data-oriented design (DOD) implementation of the Comic API, contrasting with the object-oriented programming (OOP) approach in ComicApiOop.

## Data-Oriented Design Principles Applied

### 1. **Separation of Data and Behavior**
- **Data structures** are in `Models/ComicData.cs` - they only contain data, no methods
- **Behavior/functions** are in `Data/VisibilityProcessor.cs` - pure functions that operate on data
- This separation allows for better testing, parallelization, and cache efficiency

### 2. **Value Types and Immutability**
- Most data structures use `readonly struct` for value semantics
- Immutable data reduces bugs and makes code more predictable
- Examples: `ChapterData`, `ContentRatingData`, `PricingData`, `GeographicRuleData`, etc.

### 3. **Array-Based Processing**
- Data is organized in arrays for better cache locality
- `ComicBatchData` uses arrays (`ChapterData[]`, `TagData[]`, etc.) instead of lists
- Functions use `ReadOnlySpan<T>` for zero-allocation iteration

### 4. **Pure Functions**
- Functions in `VisibilityProcessor` are static and pure
- They take data as input and return computed results without side effects
- Examples:
  - `EvaluateGeographicVisibility(in GeographicRuleData rule, DateTime currentTime)`
  - `CalculateCurrentPrice(in PricingData pricing, DateTime currentTime)`
  - `DetermineContentFlags(ContentFlag baseFlags, ReadOnlySpan<ChapterData> chapters, in PricingData? pricing)`

### 5. **Batch Processing**
- Database queries fetch data in batches
- Processing happens on batches of data, not individual objects
- `DatabaseQueryHelper` provides batch query methods

### 6. **Minimal Object-Oriented Overhead**
- No inheritance hierarchies (unlike OOP version with `VisibilityRule` base class)
- No virtual method calls
- No navigation properties with lazy loading

## Key Differences from OOP Version

| Aspect | OOP Version | DOD Version |
|--------|-------------|-------------|
| **Data Structures** | Classes with methods | Structs/classes with data only |
| **Behavior** | Methods on classes | Static pure functions |
| **Visibility Rules** | Abstract base class with virtual methods | Simple data structures + pure functions |
| **Price Calculation** | `GetCurrentPrice()` method on `ComicPricing` | `CalculateCurrentPrice()` static function |
| **Memory Layout** | Reference types scattered in heap | Value types with better cache locality |
| **Computation** | Object method calls | Function calls with data passing |

## Project Structure

```
ComicApiDod/
├── Models/
│   ├── Enums.cs              # Shared enumerations
│   └── ComicData.cs          # Pure data structures (structs and simple classes)
├── Data/
│   ├── ComicDbContext.cs     # EF Core database context
│   ├── DatabaseQueryHelper.cs # Helper functions for database queries
│   └── VisibilityProcessor.cs # Pure functions for processing visibility
├── Program.cs                # Minimal API setup
└── appsettings.json          # Configuration
```

## Running the API

### Option 1: Using Docker Compose (Recommended)

The easiest way to start the service with all dependencies (MySQL, Prometheus, Grafana) is using Docker Compose:

```bash
# Start all services (MySQL, API, Prometheus, Grafana)
docker-compose -f docker-compose.dod.yml up -d --build

# View logs
docker-compose -f docker-compose.dod.yml logs -f comic-api-dod

# Stop all services
docker-compose -f docker-compose.dod.yml down
```

**What happens when you start:**
1. **MySQL Database**: Starts and automatically initializes with schema and seed data
   - Database: `comicdb`
   - User: `comicuser`
   - Password: `comicpass`
   - Port: `3306`
   - Seed data is automatically loaded from `mysql/init/seed/SeedData.sql`

2. **ComicApiDod Service**: Starts after MySQL is healthy
   - Port: `8081` (mapped from container port `8080`)
   - Automatically runs database migrations on startup
   - Registers message queues and starts background processing

3. **Monitoring Services**: Prometheus and Grafana start for metrics collection

**Service URLs:**
- API: `http://localhost:8081`
- Health Check: `http://localhost:8081/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)

### Option 2: Running Locally (Development)

For local development without Docker:

```bash
# 1. Ensure MySQL is running locally
# Create database and user:
mysql -u root -p
CREATE DATABASE comicdb;
CREATE USER 'comicuser'@'localhost' IDENTIFIED BY 'comicpass';
GRANT ALL PRIVILEGES ON comicdb.* TO 'comicuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 2. Update connection string in appsettings.Development.json:
# ConnectionStrings__DefaultConnection=Server=localhost;Port=3306;Database=comicdb;User=comicuser;Password=comicpass;

# 3. Run database migrations
dotnet ef database update --project ComicApiDod.csproj

# 4. Seed the database (optional - if you have seed data)
mysql -u comicuser -pcomicpass comicdb < mysql/init/seed/SeedData.sql

# 5. Restore dependencies
dotnet restore

# 6. Build the project
dotnet build

# 7. Run the API
dotnet run --project ComicApiDod.csproj
```

The API will be available at `http://localhost:5000` (or as configured in `appsettings.Development.json`).

### Option 3: Hybrid Approach - Dependencies in Docker, API in IDE Debug Mode

This approach is ideal for debugging endpoints in your IDE while using containerized dependencies:

```bash
# 1. Start MySQL first (required for API)
docker-compose -f docker-compose.dod.yml up -d mysql

# 2. Wait for MySQL to be healthy, then start monitoring services
# Note: Prometheus may show warnings about API not being available, but it will still work
docker-compose -f docker-compose.dod.yml up -d prometheus grafana node_exporter

# Verify dependencies are running
docker-compose -f docker-compose.dod.yml ps

# Alternative: Start all dependencies at once (Prometheus will retry API connection)
docker-compose -f docker-compose.dod.yml up -d mysql prometheus grafana node_exporter
```

**Note:** Prometheus is configured to scrape metrics from `comic-api-dod:8080`. When running the API locally in your IDE, Prometheus won't be able to scrape metrics (this is fine for debugging). If you want metrics collection while debugging, you can temporarily update `prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'comic-api-oop'
    static_configs:
      - targets: ['host.docker.internal:5000']  # For Windows/Mac
      # - targets: ['172.17.0.1:5000']  # For Linux, use host IP
    metrics_path: '/metrics'
```

Then restart Prometheus: `docker-compose -f docker-compose.dod.yml restart prometheus`

**What this starts:**
- **MySQL**: Available at `localhost:3306` (same credentials as Docker setup)
- **Prometheus**: Available at `http://localhost:9090`
- **Grafana**: Available at `http://localhost:3000`
- **Node Exporter**: Available at `http://localhost:9100`

**Configuration for IDE Debugging:**

1. **Update `appsettings.Development.json`** (already configured):
   ```json
   {
     "ConnectionStrings": {
       "DefaultConnection": "Server=localhost;Port=3306;Database=comicdb;User=comicuser;Password=comicpass;"
     }
   }
   ```

2. **Set up launch configuration in your IDE:**

   **JetBrains Rider:**
   
   **Method 1: Using Run/Debug Configuration (Recommended)**
   1. Click on the run configuration dropdown (top toolbar, next to the green play button)
   2. Select "Edit Configurations..." (or press `Alt+Shift+F10` then `0`)
   3. In the Run/Debug Configurations dialog:
      - Select your configuration (or create a new one by clicking `+` → `.NET Executable`)
      - Set **Executable**: Browse to `ComicApiDod/bin/Debug/net8.0/ComicApiDod.dll`
      - Set **Working directory**: `ComicApiDod` folder
      - In the **Environment variables** section, click `+` and add:
        - **Name**: `ASPNETCORE_ENVIRONMENT`
        - **Value**: `Development`
   4. Click **OK** to save
   5. The environment variable will now be set when you run/debug from Rider
   
   **Method 2: Using launchSettings.json**
   1. Navigate to `ComicApiDod/Properties/launchSettings.json` (create if it doesn't exist)
   2. Add or update the configuration:
   ```json
   {
     "profiles": {
       "ComicApiDod": {
         "commandName": "Project",
         "environmentVariables": {
           "ASPNETCORE_ENVIRONMENT": "Development"
         },
         "applicationUrl": "http://localhost:5000"
       }
     }
   }
   ```
   3. Rider will automatically pick up these settings
   
   **Method 3: Quick Set via Environment Variables**
   - Right-click on the project in Solution Explorer
   - Select **Run** → **Edit Configurations...**
   - Add environment variable as described in Method 1
   
   **Verification:**
   - Set a breakpoint in `ProgramDod.cs` and check `Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT")` or check the logs for "Development" environment
   - Ensure `appsettings.Development.json` is being loaded (check logs for configuration source)

   **VS Code:**
   - Create/update `.vscode/launch.json`:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "name": ".NET Core Launch (ComicApiDod)",
         "type": "coreclr",
         "request": "launch",
         "preLaunchTask": "build",
         "program": "${workspaceFolder}/ComicApiDod/bin/Debug/net8.0/ComicApiDod.dll",
         "args": [],
         "cwd": "${workspaceFolder}/ComicApiDod",
         "stopAtEntry": false,
         "env": {
           "ASPNETCORE_ENVIRONMENT": "Development"
         },
         "sourceFileMap": {
           "/Views": "${workspaceFolder}/Views"
         }
       }
     ]
   }
   ```

3. **Run database migrations** (if needed):
   ```bash
   dotnet ef database update --project ComicApiDod.csproj
   ```

4. **Start debugging in your IDE:**
   - Set breakpoints in your code (e.g., `ComicRequestHandler.cs`, `ComicVisibilityService.cs`)
   - Press F5 or click "Start Debugging"
   - The API will start on `http://localhost:5000` (or configured port)

5. **Test endpoints with debugging:**
   ```bash
   # Health check
   curl http://localhost:5000/health
   
   # Compute visibilities (will hit your breakpoints)
   curl "http://localhost:5000/api/comics/compute-visibilities?startId=1&limit=5"
   ```

**Benefits of this approach:**
- ✅ Full IDE debugging support (breakpoints, step-through, variable inspection)
- ✅ Hot reload/Edit and Continue support
- ✅ No need to rebuild Docker images for code changes
- ✅ Faster development cycle
- ✅ All dependencies still containerized and isolated

**Stopping dependencies:**
```bash
# Stop only dependencies (API is running in IDE)
docker-compose -f docker-compose.dod.yml stop mysql prometheus grafana node_exporter

# Or stop and remove containers
docker-compose -f docker-compose.dod.yml down
```

**Troubleshooting:**

- **Connection refused to MySQL:**
  - Verify MySQL container is running: `docker ps | grep mysql`
  - Check MySQL is listening on port 3306: `docker-compose -f docker-compose.dod.yml ps mysql`
  - Verify connection string uses `localhost:3306` (not `mysql`)

- **Port already in use:**
  - If port 5000 is in use, update `appsettings.Development.json` or `launchSettings.json` to use a different port
  - Or stop any other services using that port

- **Database not found:**
  - Ensure MySQL container initialized properly: `docker-compose -f docker-compose.dod.yml logs mysql`
  - Run migrations: `dotnet ef database update --project ComicApiDod.csproj`

## Seed Data

### Automatic Seeding (Docker)

When using Docker Compose, seed data is automatically inserted when MySQL starts for the first time:
- Seed script location: `mysql/init/seed/SeedData.sql`
- The script runs automatically via the MySQL initialization process
- Data includes sample comics, publishers, genres, chapters, pricing, and visibility rules

### Manual Seeding

If you need to manually insert seed data:

```bash
# Using Docker MySQL container
docker exec -i comic_mysql mysql -u comicuser -pcomicpass comicdb < mysql/init/seed/SeedData.sql

# Or using local MySQL
mysql -u comicuser -pcomicpass comicdb < mysql/init/seed/SeedData.sql
```

### Verifying Seed Data

Check that data was inserted correctly:

```bash
# Using Docker
docker exec -it comic_mysql mysql -u comicuser -pcomicpass comicdb -e "SELECT COUNT(*) FROM Comics;"

# Or using local MySQL
mysql -u comicuser -pcomicpass comicdb -e "SELECT COUNT(*) FROM Comics;"
```

### Resetting the Database

To start fresh with seed data:

```bash
# Stop services
docker-compose -f docker-compose.dod.yml down

# Remove MySQL volume (this deletes all data)
docker volume rm comicapi_dod_mysql_data

# Start again (will re-seed automatically)
docker-compose -f docker-compose.dod.yml up -d --build
```

## Testing the Service

### 1. Health Check

Verify the service is running:

```bash
curl http://localhost:8081/health
```

Expected response:
```json
{
  "status": "Healthy",
  "timestamp": "2025-01-26T..."
}
```

### 2. Compute Visibilities Endpoint

Test the main endpoint:

```bash
# Compute visibilities for comics starting from ID 1, limit 5
curl "http://localhost:8081/api/comics/compute-visibilities?startId=1&limit=5"
```

This endpoint:
- Enqueues a visibility computation request
- Processes it asynchronously using the SimpleQueue framework
- Returns the computed visibility results

### 3. View Logs

Monitor service activity:

```bash
# View all logs
docker-compose -f docker-compose.dod.yml logs -f

# View only API logs
docker-compose -f docker-compose.dod.yml logs -f comic-api-dod

# View MySQL logs
docker-compose -f docker-compose.dod.yml logs -f mysql
```

### 4. Check Database

Verify data in the database:

```bash
# Connect to MySQL
docker exec -it comic_mysql mysql -u comicuser -pcomicpass comicdb

# Run queries
SELECT * FROM Comics LIMIT 5;
SELECT * FROM ComputedVisibilities LIMIT 5;
```

## Troubleshooting

### Service Won't Start

1. **Check MySQL is healthy:**
   ```bash
   docker-compose -f docker-compose.dod.yml ps
   ```

2. **View error logs:**
   ```bash
   docker-compose -f docker-compose.dod.yml logs comic-api-dod
   ```

3. **Verify connection string** in `docker-compose.dod.yml` matches MySQL credentials

### Database Migration Issues

If migrations fail:
```bash
# Manually run migrations
docker exec -it comic-api-dod dotnet ef database update --project ComicApiDod.csproj
```

### Seed Data Not Loading

1. **Check MySQL initialization logs:**
   ```bash
   docker-compose -f docker-compose.dod.yml logs mysql
   ```

2. **Verify seed file exists:**
   ```bash
   ls -la mysql/init/seed/SeedData.sql
   ```

3. **Manually run seed script** (see Manual Seeding above)

## API Endpoints

### Compute Visibilities
```
GET /api/comics/compute-visibilities?startId={id}&limit={limit}
```

Computes visibility for comics using DOD approach:
1. Fetches comic IDs in batch
2. Loads all data for each comic in parallel queries
3. Processes data using pure functions
4. Saves results in batch

### Health Check
```
GET /health
```

Returns API health status.

## Performance Considerations

### Advantages of DOD Approach:
1. **Better cache locality** - contiguous memory layout with structs
2. **Easier parallelization** - pure functions with no shared state
3. **Predictable performance** - no virtual method calls or runtime polymorphism
4. **Lower GC pressure** - value types allocated on stack when possible

### When to Use DOD:
- High-performance scenarios requiring optimal cache usage
- Data-heavy computations with many transformations
- Systems requiring high parallelization
- When data flow is more important than object relationships

### When OOP Might Be Better:
- Complex domain models with rich business rules
- Systems where object identity and state management are crucial
- When leveraging polymorphism simplifies design
- Teams more familiar with OOP patterns

## Example: Processing Flow

```csharp
// 1. Fetch data (pure data, no behavior)
var batchData = await DatabaseQueryHelper.GetComicBatchDataAsync(db, comicId);

// 2. Process with pure functions
var visibilities = VisibilityProcessor.ComputeVisibilities(batchData, DateTime.UtcNow);

// 3. Save results
await DatabaseQueryHelper.SaveComputedVisibilitiesAsync(db, visibilities);
```

Compare with OOP version:
```csharp
// OOP: Objects with behavior
var comic = await db.Comics
    .Include(c => c.ContentRating)
    .Include(c => c.RegionalPricing)
    // ... more includes
    .FirstOrDefaultAsync();

var visibility = new ComputedVisibility { /* ... */ };
visibility.CurrentPrice = pricing.GetCurrentPrice(); // Method call on object
```

## Testing Benefits

DOD makes testing easier:
- Pure functions can be tested in isolation
- No need for mocking complex object hierarchies
- Easy to create test data with struct initializers

```csharp
// Easy to test pure functions
var pricing = new PricingData { BasePrice = 10.0m, IsFreeContent = false, /* ... */ };
var price = VisibilityProcessor.CalculateCurrentPrice(pricing, DateTime.UtcNow);
Assert.Equal(10.0m, price);
```

## Future Enhancements

Potential DOD optimizations:
1. SIMD processing for bulk calculations
2. Memory pooling for large arrays
3. Parallel batch processing with `Parallel.ForEach`
4. Custom allocators for hot paths
5. Structure-of-Arrays (SoA) layout for even better cache performance

## Learning Resources

- [Data-Oriented Design Book](https://www.dataorienteddesign.com/dodbook/)
- Mike Acton's "Data-Oriented Design" talks
- Unity DOTS (Data-Oriented Technology Stack)
- Game Engine Architecture patterns




