namespace ComicApiDod.Handlers;

public static class HealthRequestHandler
{
    public static IResult HandleHealthCheck()
    {
        var healthStatus = new 
        { 
            Status = "Healthy", 
            Api = "ComicApiDod",
            MemoryAllocated = GC.GetTotalMemory(false),
            GCGeneration0Count = GC.CollectionCount(0),
            GCGeneration1Count = GC.CollectionCount(1),
            GCGeneration2Count = GC.CollectionCount(2)
        };
        return Results.Ok(healthStatus);
    }
}
