# Notion to Markdown Converter

A .NET console application that exports Notion databases and pages to Markdown files, preserving formatting, relationships, and images.

## Features

- Exports Notion databases and pages to Markdown format
- Preserves text formatting (bold, italic, strikethrough, code, etc.)
- Maintains page relationships with proper linking
- Downloads and stores images locally
- Converts database tables to Markdown tables
- Handles nested databases
- Preserves callouts, toggles, and other Notion-specific blocks
- Arranges column content vertically in exported files
- Configurable page limit for testing with subsets of data

## Prerequisites

- .NET 6.0 or later
- Notion API key (Integration Token)

## Setup

1. Clone this repository
2. Create a Notion integration at https://www.notion.so/my-integrations
3. Copy your integration token
4. Set the environment variable `NOTION_API_KEY` with your integration token:
   - Windows: `set NOTION_API_KEY=your_token_here`
   - macOS/Linux: `export NOTION_API_KEY=your_token_here`
5. Share your Notion database with your integration (in Notion, click "Share" on the database and add your integration)

## Usage

Run the application with:

```
dotnet run -- [options]
Or
dotnet run -- -d 12aa6a034791464aa49f8c4b13f5ff88 -p "Zig mouseless"
```


### Options

- `-h, --help`: Show help message
- `-o, --output <path>`: Specify the output directory (default: 'export')
- `-d, --database <id>`: Specify the Notion database ID to export
- `-l, --limit <number>`: Limit the number of pages to export (default: no limit)
- `-p, --pages <names>`: Export only pages with specific names (comma-separated)

### Examples

```
# Export a database to the 'my_notes' directory
dotnet run -- -d 12aa6a034791464aa49f8c4b13f5ff88 -o my_notes

# Export only the first 5 pages (useful for testing)
dotnet run -- -d 12aa6a034791464aa49f8c4b13f5ff88 -l 5

# Export only specific pages by name (useful for testing)
dotnet run -- -d 12aa6a034791464aa49f8c4b13f5ff88 -p "Zig mouseless,Another Page"
```

## How to Find Your Database ID

The database ID is the part of the URL after the workspace name and before the query parameters.

For example, in the URL:
```
https://www.notion.so/username/12aa6a034791464aa49f8c4b13f5ff88?v=...
```

The database ID is `12aa6a034791464aa49f8c4b13f5ff88`.

## Output Structure

The application creates:
- A folder for each database
- A Markdown file for each page
- A folder for each page's images
- Relative links between related pages

## Limitations and Known Issues

- Some complex blocks might not render perfectly in Markdown
- Databases with custom views might not preserve all visualization settings
- Embedded content like videos or external applications are represented as links
- API rate limits may affect performance when exporting large databases

## Security Note

For security, use the environment variable to store your API token rather than hardcoding it in the source. 

## Testing Tips

When testing with large databases, use the page limit option (`-l`) to export only a few pages first:

```
# Test with just 3 pages first
dotnet run -- -d your_database_id -l 3
```

This allows you to verify formatting and structure without waiting for a complete export. 




## Adding Support for Unsupported Block Types

This section provides step-by-step instructions for implementing support for currently unsupported Notion block types. The main block processing logic is in `SimpleNotionExporter.cs` in the `ProcessBlockToMarkdown` method (around line 158).

### How Block Processing Works

1. Each block type is handled in a switch statement based on `block.Type.ToString().ToLower()`
2. Text content is extracted using the `ExtractRichText` method
3. Complex blocks may require JSON parsing to access specific properties
4. Child blocks are processed recursively if `block.HasChildren` is true

### Implementation Guide by Block Type

#### 1. Code Block (`case "code":`)

**What it does:** Displays code with syntax highlighting and language specification.

**Implementation steps:**
```csharp
case "code":
    try
    {
        var codeJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string codeContent = "";
        string language = codeJson["code"]?["language"]?.ToString() ?? "";
        
        // Extract code content from rich_text
        if (codeJson["code"]?["rich_text"] is Newtonsoft.Json.Linq.JArray richTextArray)
        {
            codeContent = string.Join("", richTextArray.Select(rt => rt["plain_text"]?.ToString() ?? ""));
        }
        
        // Add code block to markdown
        markdown.AppendLine($"```{language}");
        markdown.AppendLine(codeContent);
        markdown.AppendLine("```");
        markdown.AppendLine();
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing code block: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 2. Quote Block (`case "quote":`)

**What it does:** Displays quoted text with proper markdown formatting.

**Implementation steps:**
```csharp
case "quote":
    string quoteText = ExtractRichText(block);
    if (!string.IsNullOrEmpty(quoteText))
    {
        markdown.AppendLine($"> {quoteText}");
        markdown.AppendLine();
    }
    break;
```

#### 3. Callout Block (`case "callout":`)

**What it does:** Displays highlighted text with an emoji icon.

**Implementation steps:**
```csharp
case "callout":
    try
    {
        var calloutJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string icon = calloutJson["callout"]?["icon"]?["emoji"]?.ToString() ?? "💡";
        string calloutText = ExtractRichText(block);
        
        if (!string.IsNullOrEmpty(calloutText))
        {
            markdown.AppendLine($"> {icon} **{calloutText}**");
            markdown.AppendLine();
        }
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing callout: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 4. Table Block (`case "table":`)

**What it does:** Creates markdown tables with headers and rows.

**Implementation steps:**
```csharp
case "table":
    try
    {
        if (block.HasChildren)
        {
            var tableRows = await _client.Blocks.RetrieveChildrenAsync(
                new BlockRetrieveChildrenRequest { BlockId = block.Id });
            
            var rows = new List<List<string>>();
            foreach (var row in tableRows.Results)
            {
                if (row.Type.ToString().ToLower() == "table_row")
                {
                    var rowJson = Newtonsoft.Json.Linq.JObject.Parse(row.ToString());
                    var cells = new List<string>();
                    
                    if (rowJson["table_row"]?["cells"] is Newtonsoft.Json.Linq.JArray cellsArray)
                    {
                        foreach (var cell in cellsArray)
                        {
                            if (cell is Newtonsoft.Json.Linq.JArray richTextArray)
                            {
                                string cellText = string.Join("", richTextArray.Select(rt => rt["plain_text"]?.ToString() ?? ""));
                                cells.Add(cellText);
                            }
                        }
                    }
                    rows.Add(cells);
                }
            }
            
            // Generate markdown table
            if (rows.Count > 0)
            {
                // Header row
                markdown.AppendLine("| " + string.Join(" | ", rows[0]) + " |");
                markdown.AppendLine("| " + string.Join(" | ", rows[0].Select(_ => "---")) + " |");
                
                // Data rows
                for (int i = 1; i < rows.Count; i++)
                {
                    markdown.AppendLine("| " + string.Join(" | ", rows[i]) + " |");
                }
                markdown.AppendLine();
            }
        }
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing table: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 5. Table Row Block (`case "table_row":`)

**What it does:** Processed by the table block above - usually doesn't need separate handling.

**Implementation:** This is typically handled as part of the table block processing. If you need to handle it separately:
```csharp
case "table_row":
    // Usually processed by parent table block
    // Individual row processing can be added here if needed
    break;
```

#### 6. Link Preview Block (`case "link_preview":`)

**What it does:** Shows a preview of a linked webpage.

**Implementation steps:**
```csharp
case "link_preview":
    try
    {
        var linkJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string url = linkJson["link_preview"]?["url"]?.ToString() ?? "";
        
        if (!string.IsNullOrEmpty(url))
        {
            markdown.AppendLine($"[🔗 Link Preview]({url})");
            markdown.AppendLine();
        }
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing link preview: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 7. Video Block (`case "video":`)

**What it does:** Embeds or links to video content.

**Implementation steps:**
```csharp
case "video":
    try
    {
        var videoJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string videoUrl = "";
        
        // Check if it's an external video or uploaded file
        if (videoJson["video"]?["type"]?.ToString() == "external")
        {
            videoUrl = videoJson["video"]?["external"]?["url"]?.ToString() ?? "";
        }
        else if (videoJson["video"]?["type"]?.ToString() == "file")
        {
            videoUrl = videoJson["video"]?["file"]?["url"]?.ToString() ?? "";
        }
        
        if (!string.IsNullOrEmpty(videoUrl))
        {
            markdown.AppendLine($"🎥 [Video]({videoUrl})");
            markdown.AppendLine();
        }
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing video: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 8. Column List Block (`case "column_list":`)

**What it does:** Container for columns - processes child column blocks.

**Implementation steps:**
```csharp
case "column_list":
    // Process child columns
    if (block.HasChildren)
    {
        try
        {
            var columns = await _client.Blocks.RetrieveChildrenAsync(
                new BlockRetrieveChildrenRequest { BlockId = block.Id });
            
            markdown.AppendLine("<div style=\"display: flex; gap: 20px;\">");
            
            foreach (var column in columns.Results)
            {
                await ProcessBlockToMarkdown((Block)column, markdown, imagesFolder);
            }
            
            markdown.AppendLine("</div>");
            markdown.AppendLine();
        }
        catch (Exception ex)
        {
            markdown.AppendLine($"*Error processing column list: {ex.Message}*");
            markdown.AppendLine();
        }
    }
    break;
```

#### 9. Column Block (`case "column":`)

**What it does:** Individual column within a column list.

**Implementation steps:**
```csharp
case "column":
    markdown.AppendLine("<div>");
    
    // Process child blocks within the column
    if (block.HasChildren)
    {
        try
        {
            var columnContent = await _client.Blocks.RetrieveChildrenAsync(
                new BlockRetrieveChildrenRequest { BlockId = block.Id });
            
            foreach (var columnBlock in columnContent.Results)
            {
                await ProcessBlockToMarkdown((Block)columnBlock, markdown, imagesFolder);
            }
        }
        catch (Exception ex)
        {
            markdown.AppendLine($"*Error processing column content: {ex.Message}*");
            markdown.AppendLine();
        }
    }
    
    markdown.AppendLine("</div>");
    break;
```

#### 10. Child Database Block (`case "child_database":`)

**What it does:** Links to a child database within the page.

**Implementation steps:**
```csharp
case "child_database":
    try
    {
        var dbJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string title = dbJson["child_database"]?["title"]?.ToString() ?? "Child Database";
        
        markdown.AppendLine($"📊 **{title}**");
        markdown.AppendLine("*(Child database - see Notion for full content)*");
        markdown.AppendLine();
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing child database: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

#### 11. Synced Block (`case "synced_block":`)

**What it does:** References content from another synced block.

**Implementation steps:**
```csharp
case "synced_block":
    try
    {
        var syncedJson = Newtonsoft.Json.Linq.JObject.Parse(block.ToString());
        string syncedFromId = syncedJson["synced_block"]?["synced_from"]?.ToString();
        
        if (string.IsNullOrEmpty(syncedFromId))
        {
            // This is the original synced block - process its children
            if (block.HasChildren)
            {
                var syncedContent = await _client.Blocks.RetrieveChildrenAsync(
                    new BlockRetrieveChildrenRequest { BlockId = block.Id });
                
                foreach (var syncedBlock in syncedContent.Results)
                {
                    await ProcessBlockToMarkdown((Block)syncedBlock, markdown, imagesFolder);
                }
            }
        }
        else
        {
            // This is a reference to another synced block
            markdown.AppendLine("🔄 *(Synced content - see original block)*");
            markdown.AppendLine();
        }
    }
    catch (Exception ex)
    {
        markdown.AppendLine($"*Error processing synced block: {ex.Message}*");
        markdown.AppendLine();
    }
    break;
```

### Testing Your Implementation

1. **Find test data:** Create a Notion page with the block type you're implementing
2. **Run with limit:** Use `dotnet run -- -d your_database_id -l 1` to test with just one page
3. **Check output:** Verify the markdown output looks correct
4. **Handle errors:** Make sure your implementation includes try-catch blocks
5. **Test edge cases:** Try empty blocks, blocks with special characters, etc.

### Debugging Tips

- **JSON structure:** Add `Console.WriteLine(block.ToString())` to see the raw JSON structure
- **Notion API docs:** Check the [Notion API documentation](https://developers.notion.com/reference) for block structure details
- **Rich text extraction:** Use the existing `ExtractRichText` method when possible
- **Child blocks:** Remember to process child blocks with `RetrieveChildrenAsync` when `block.HasChildren` is true

### Note on NumberedListItem

The `numbered_list_item` case is already implemented (line 215 in the code), but you may see this error if there's a formatting issue. Check that the case matches exactly: `"numbered_list_item"` (with underscores, not camelCase).

## Implementation Status

This checklist tracks the implementation status of different Notion block types:

### Basic Blocks
- [x] Paragraph
- [x] Heading 1-3
- [x] Bulleted List Item
- [x] Numbered List Item (improved)
- [x] To-do List
- [x] Code Block
- [ ] Quote Block
- [ ] Callout Block
- [x] Image Block
- [x] Toggle Block
- [x] Divider Block

### Advanced Blocks
- [ ] Table Block
- [ ] Table Row Block
- [ ] Link Preview Block
- [ ] Video Block
- [ ] Column List Block
- [ ] Column Block
- [ ] Child Database Block
- [ ] Synced Block

### Additional Features
- [x] Rich Text Formatting (bold, italic, strikethrough, code)
- [x] External Links (via rich text)
- [x] Image Downloads
- [x] Database Export
- [ ] Child Page Export (only as links)

### Planned Enhancements
- [ ] Bookmark Block
- [ ] File Block
- [ ] PDF Block
- [ ] Equation Block
- [ ] Template Block
- [ ] Breadcrumb Block
- [ ] Table of Contents Block
- [ ] Divider Block
- [ ] Link to Page Block
- [ ] Embedded Content (iframes)
