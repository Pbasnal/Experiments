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

```bash
# Restore dependencies
dotnet restore

# Build the project
dotnet build

# Run the API
dotnet run
```

The API will be available at `http://localhost:5000` (or as configured).

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




