# Modular Architecture Documentation

## Overview

The Notion to Markdown converter has been refactored into a clean, modular architecture using **dependency injection** and the **strategy pattern**. This makes it easy to add new block types, maintain existing code, and test individual components.

## Architecture Diagram

```
┌─────────────────────────────────────────────┐
│           Program.cs (Entry Point)          │
│  - Configures DI Container                  │
│  - Creates ModularNotionExporter            │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│      ModularNotionExporter                  │
│  - Orchestrates export process              │
│  - Uses IBlockProcessorFactory              │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│     IBlockProcessorFactory                  │
│  - Resolves appropriate processor           │
│  - Caches processor instances               │
└────────────────┬────────────────────────────┘
                 │
                 ▼
        ┌────────┴────────┐
        ▼                 ▼
┌────────────────┐  ┌───────────────┐
│ IBlockProcessor│  │ Concrete Block│
│  (Interface)   │  │  Processors   │
│                │  │               │
│ - CanProcess() │  │ - Paragraph   │
│ - ProcessAsync │  │ - Heading     │
│                │  │ - Code        │
└────────────────┘  │ - Lists       │
                    │ - Toggle      │
                    │ - Image       │
                    │ - Quote       │
                    │ - Callout     │
                    │ - etc...      │
                    └───────────────┘
```

## Core Components

### 1. Interfaces

#### `IBlockProcessor`
Defines the contract for all block processors.

```csharp
public interface IBlockProcessor
{
    string BlockType { get; }
    Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context);
    bool CanProcess(string blockType);
}
```

#### `IBlockProcessorFactory`
Factory for resolving the correct processor for a block type.

```csharp
public interface IBlockProcessorFactory
{
    IBlockProcessor? GetProcessor(string blockType);
    IEnumerable<IBlockProcessor> GetAllProcessors();
}
```

#### `IProcessingContext`
Provides shared resources to all processors.

```csharp
public interface IProcessingContext
{
    NotionClient Client { get; }
    HttpClient HttpClient { get; }
    string ImagesFolder { get; }
    IBlockProcessorFactory ProcessorFactory { get; }
}
```

### 2. Base Classes

#### `BlockProcessorBase`
Abstract base class providing common functionality:
- Rich text extraction
- Child block processing
- NaN value handling
- Pagination support

### 3. Block Processors

Each block type has its own dedicated processor:

| Processor | Block Type | Description |
|-----------|------------|-------------|
| `ParagraphBlockProcessor` | `paragraph` | Regular text paragraphs |
| `HeadingBlockProcessor` | `heading_1`, `heading_2`, `heading_3` | Markdown headings |
| `CodeBlockProcessor` | `code` | Code blocks with syntax highlighting |
| `BulletedListItemBlockProcessor` | `bulleted_list_item` | Bulleted lists |
| `NumberedListItemBlockProcessor` | `numbered_list_item` | Numbered lists |
| `TodoBlockProcessor` | `to_do` | Checkbox/todo items |
| `ToggleBlockProcessor` | `toggle` | Collapsible sections |
| `QuoteBlockProcessor` | `quote` | Block quotes |
| `CalloutBlockProcessor` | `callout` | Highlighted callout boxes |
| `DividerBlockProcessor` | `divider` | Horizontal dividers |
| `ImageBlockProcessor` | `image` | Images with download support |
| `DefaultBlockProcessor` | `*` | Fallback for unsupported types |

## Adding a New Block Type

Adding support for a new Notion block type is simple:

### Step 1: Create a New Processor

Create a new file in `BlockProcessors/` directory:

```csharp
using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    public class YourNewBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "your_block_type";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            // 1. Extract content from the block
            string content = ExtractRichText(block);
            
            // 2. Format as markdown
            if (!string.IsNullOrEmpty(content))
            {
                markdown.AppendLine($"Your markdown format: {content}");
                markdown.AppendLine();
            }
            
            // 3. Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}
```

### Step 2: Register in DI Container

Add the processor to `Core/ServiceConfiguration.cs`:

```csharp
services.AddTransient<IBlockProcessor, YourNewBlockProcessor>();
```

**That's it!** Your new block type is now supported.

## Dependency Injection

The application uses **Microsoft.Extensions.DependencyInjection** for IoC (Inversion of Control).

### Service Registration

All services are registered in `Core/ServiceConfiguration.cs`:

```csharp
public static class ServiceConfiguration
{
    public static IServiceCollection ConfigureServices(this IServiceCollection services)
    {
        // Register HttpClient
        services.AddSingleton<HttpClient>();
        
        // Register all block processors
        services.AddTransient<IBlockProcessor, ParagraphBlockProcessor>();
        // ... other processors ...
        services.AddTransient<IBlockProcessor, DefaultBlockProcessor>(); // Must be last
        
        // Register factory
        services.AddSingleton<IBlockProcessorFactory, BlockProcessorFactory>();
        
        return services;
    }
}
```

### Service Lifetimes

- **Singleton**: `HttpClient`, `IBlockProcessorFactory` - created once and reused
- **Transient**: `IBlockProcessor` - new instance created each time

## Benefits of This Architecture

### ✅ **Extensibility**
- Add new block types without modifying existing code
- Each processor is independent and focused

### ✅ **Maintainability**
- Small, focused classes (Single Responsibility Principle)
- Easy to locate and fix issues
- Clear separation of concerns

### ✅ **Testability**
- Each processor can be unit tested independently
- Mock dependencies easily with interfaces
- Test processors in isolation

### ✅ **Flexibility**
- Swap implementations without changing callers
- Easy to add new features (e.g., different output formats)
- Configure behavior through DI

### ✅ **Reusability**
- Share common logic through base class
- Processors can be used in different contexts
- Factory pattern allows dynamic resolution

## Project Structure

```
NotionConnectionTest/
├── Core/                           # Core interfaces and implementations
│   ├── IBlockProcessor.cs          # Processor interface
│   ├── IBlockProcessorFactory.cs   # Factory interface
│   ├── IProcessingContext.cs       # Context interface
│   ├── BlockProcessorBase.cs       # Base implementation
│   ├── BlockProcessorFactory.cs    # Factory implementation
│   ├── ProcessingContext.cs        # Context implementation
│   └── ServiceConfiguration.cs     # DI configuration
│
├── BlockProcessors/                # Individual block processors
│   ├── ParagraphBlockProcessor.cs
│   ├── HeadingBlockProcessor.cs
│   ├── CodeBlockProcessor.cs
│   ├── BulletedListItemBlockProcessor.cs
│   ├── NumberedListItemBlockProcessor.cs
│   ├── TodoBlockProcessor.cs
│   ├── ToggleBlockProcessor.cs
│   ├── QuoteBlockProcessor.cs
│   ├── CalloutBlockProcessor.cs
│   ├── DividerBlockProcessor.cs
│   ├── ImageBlockProcessor.cs
│   └── DefaultBlockProcessor.cs
│
├── ModularNotionExporter.cs        # Main exporter using DI
├── Program.cs                      # Entry point
└── SimpleNotionExporter.cs         # Legacy (deprecated)
```

## Migration Notes

### Old vs New

| Aspect | Old (SimpleNotionExporter) | New (ModularNotionExporter) |
|--------|---------------------------|----------------------------|
| **Architecture** | Monolithic, single class | Modular, multiple classes |
| **Block Handling** | Switch statement | Strategy pattern |
| **Extensibility** | Edit large switch case | Add new processor class |
| **Dependencies** | Tightly coupled | Injected via DI |
| **Testing** | Hard to unit test | Easy to unit test |
| **Lines of Code** | ~800 lines in one file | ~100 lines per file |

### Backward Compatibility

The old `SimpleNotionExporter` is still available but **deprecated**. It will be removed in a future version.

## Testing Example

Here's how you can unit test a processor:

```csharp
[Test]
public async Task ParagraphProcessor_ProcessesTextCorrectly()
{
    // Arrange
    var processor = new ParagraphBlockProcessor();
    var markdown = new StringBuilder();
    var mockContext = Mock.Of<IProcessingContext>();
    var mockBlock = CreateMockParagraphBlock("Test content");
    
    // Act
    await processor.ProcessAsync(mockBlock, markdown, mockContext);
    
    // Assert
    Assert.Contains("Test content", markdown.ToString());
}
```

## Performance Considerations

- **Caching**: ProcessorFactory caches processor lookups for fast resolution
- **Async/Await**: All I/O operations are async for better performance
- **Pagination**: Properly handles large pages with many blocks
- **Memory**: Processors are lightweight; transient lifetime is appropriate

## Future Enhancements

Possible improvements to the architecture:

1. **Configuration System**: Add options for markdown formatting preferences
2. **Plugin System**: Load processors from external assemblies
3. **Custom Formatters**: Support different output formats (HTML, PDF, etc.)
4. **Middleware Pipeline**: Add pre/post-processing hooks
5. **Async Streaming**: Stream large pages instead of loading all in memory
6. **Metrics/Telemetry**: Add logging and performance tracking

## Conclusion

This modular architecture makes the codebase:
- **Clean**: Well-organized, focused classes
- **Extensible**: Add features without breaking existing code
- **Maintainable**: Easy to understand and modify
- **Testable**: Unit test individual components
- **Professional**: Follows industry best practices

For questions or contributions, refer to the main README.md file.

