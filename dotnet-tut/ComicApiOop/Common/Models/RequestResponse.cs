
using Common.SimpleDodFramework;

namespace Common.Models;

/// <summary>
/// Example request message that can be queued and processed.
/// </summary>
public class ComicRequest
{
    public int RequestId { get; set; }
    public long ComicId { get; set; }
    public string? Region { get; set; }
    public string? CustomerSegment { get; set; }
}

/// <summary>
/// Example response that implements IValue so it can be stored in SimpleMap.
/// </summary>
public class ComicResponse : IValue
{
    public int Id { get; set; } // This is the RequestId from the original request
    public long ComicId { get; set; }
    public bool IsVisible { get; set; }
    public decimal CurrentPrice { get; set; }
    public string? ContentFlags { get; set; }
    public DateTime ProcessedAt { get; set; }
    
    public ComicResponse(int requestId)
    {
        Id = requestId;
        ProcessedAt = DateTime.UtcNow;
    }
}

/// <summary>
/// DTO for returning response to HTTP clients.
/// </summary>
public class ComicResponseDto
{
    public long ComicId { get; set; }
    public bool IsVisible { get; set; }
    public decimal CurrentPrice { get; set; }
    public string? ContentFlags { get; set; }
    public DateTime ProcessedAt { get; set; }
}


