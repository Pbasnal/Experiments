# Visibility Computation Benchmarks

This project benchmarks the performance of DoD vs OOP visibility computation using mocked data (no database queries).

## Purpose

This benchmark isolates the **computation logic** from database query performance to determine if performance differences are due to:
1. **Computation algorithms** (DoD vs OOP logic)
2. **Database queries** (EF Core query performance)

## Prerequisites

- .NET 8.0 SDK or later
- The benchmark uses InProcess toolchain to avoid SDK detection issues

## Running the Benchmarks

### Standard Run:
```bash
cd ComicApiBenchmarks
dotnet run -c Release
```

### If you get SDK detection errors:

**Option 1: Specify dotnet CLI path**
```bash
dotnet run -c Release -- --cli "C:\Program Files\dotnet\dotnet.exe"
```

**Option 2: Use full path to dotnet**
```bash
dotnet run -c Release -- --cli "$(where.exe dotnet)"
```

**Option 3: Install .NET 8.0 SDK**
If you only have .NET 9.0 SDK, you may need to install .NET 8.0 SDK as well, or update the project to target net9.0.

## Benchmark Methods

1. **OopComputation** - Current OOP implementation (baseline)
2. **DodComputation** - Current DoD implementation
3. **OopComputationWithCaching** - Optimized OOP version with cached values

## Results Location

Results are saved in the `BenchmarkDotNet.Artifacts` folder with detailed timing and memory metrics.

## Interpreting Results

- **Mean**: Average execution time
- **Error**: Standard deviation
- **Ratio**: Performance ratio compared to baseline (OOP)
- **Gen0/Gen1/Gen2**: Garbage collection counts
- **Allocated**: Memory allocated during execution

If DoD is slower even with mocked data, the issue is in the computation logic.
If DoD is faster with mocked data but slower in production, the issue is in database queries.

## Troubleshooting

If you see "BenchmarkDotNet requires dotnet SDK to be installed":
1. Verify dotnet is installed: `dotnet --version`
2. Try running with explicit CLI path: `dotnet run -c Release -- --cli <path-to-dotnet>`
3. The benchmark uses InProcess toolchain which should work without separate SDK builds
