using BenchmarkDotNet.Configs;
using BenchmarkDotNet.Jobs;
using BenchmarkDotNet.Toolchains.InProcess.Emit;

namespace ComicApiBenchmarks;

public class BenchmarkConfig : ManualConfig
{
    public BenchmarkConfig()
    {
        // Use InProcess toolchain to avoid SDK detection issues
        AddJob(Job.Default
            .WithToolchain(InProcessEmitToolchain.Instance)
            .WithId("InProcess"));
    }
}
