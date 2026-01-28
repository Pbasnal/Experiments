using BenchmarkDotNet.Running;
using BenchmarkDotNet.Toolchains.InProcess.Emit;
using ComicApiBenchmarks;

Console.WriteLine("Starting Visibility Computation Benchmarks...");
Console.WriteLine("This will compare DoD vs OOP visibility computation performance with mocked data.");
Console.WriteLine();

try
{
    var summary = BenchmarkRunner.Run<VisibilityComputationBenchmarks>();
    
    Console.WriteLine();
    Console.WriteLine("Benchmark completed!");
    Console.WriteLine("Check the 'BenchmarkDotNet.Artifacts' folder for detailed results.");
}
catch (Exception ex)
{
    Console.WriteLine($"Error running benchmarks: {ex.Message}");
    Console.WriteLine();
    Console.WriteLine("If you see SDK-related errors, try running with:");
    Console.WriteLine("  dotnet run -c Release -- --cli <path-to-dotnet>");
    Console.WriteLine();
    Console.WriteLine("Or install .NET 8.0 SDK if you don't have it.");
    throw;
}
