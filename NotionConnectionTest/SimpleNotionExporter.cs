using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Notion.Client;

namespace NotionConnectionTest
{
    public class SimpleNotionExporter
    {
        private readonly NotionClient _client;
        private readonly string _exportPath;
        private readonly HttpClient _httpClient;
        private int _pageCount = 0;
        private int _pageLimit = -1; // -1 means no limit
        private readonly List<string> _pageNames; // List of specific page names to export

        public SimpleNotionExporter(NotionClient client, string exportPath, int pageLimit = -1, List<string> pageNames = null)
        {
            _client = client;
            _exportPath = exportPath;
            _pageLimit = pageLimit;
            _pageNames = pageNames ?? new List<string>();
            _httpClient = new HttpClient();

            // Create export directory if it doesn't exist
            Directory.CreateDirectory(_exportPath);
        }

        public async Task ExportDatabase(string databaseId)
        {
            try
            {
                Console.WriteLine($"Exporting database: {databaseId}");

                // Create folder for this database
                string databaseFolder = Path.Combine(_exportPath, databaseId);
                Directory.CreateDirectory(databaseFolder);

                // Query the database to get pages
                var queryParams = new DatabasesQueryParameters();
                var response = await _client.Databases.QueryAsync(databaseId, queryParams);

                // Process pages
                foreach (var page in response.Results)
                {
                    // Check if we should filter by page names
                    if (_pageNames.Count > 0)
                    {
                        string pageTitle = ExtractPageTitle((Page)page);
                        bool shouldExport = _pageNames.Any(name => 
                            pageTitle.Contains(name, StringComparison.OrdinalIgnoreCase) ||
                            name.Contains(pageTitle, StringComparison.OrdinalIgnoreCase));
                        
                        if (!shouldExport)
                        {
                            continue; // Skip this page
                        }
                    }

                    await ExportPage((Page)page, databaseFolder);

                    // Check if we've hit the page limit
                    if (_pageLimit > 0 && _pageCount >= _pageLimit)
                    {
                        Console.WriteLine($"Reached page limit of {_pageLimit}. Stopping export.");
                        return;
                    }
                }

                // Handle pagination if there are more pages
                while (response.HasMore && !string.IsNullOrEmpty(response.NextCursor))
                {
                    queryParams = new DatabasesQueryParameters { StartCursor = response.NextCursor };
                    response = await _client.Databases.QueryAsync(databaseId, queryParams);

                    foreach (var page in response.Results)
                    {
                        // Check if we should filter by page names
                        if (_pageNames.Count > 0)
                        {
                            string pageTitle = ExtractPageTitle((Page)page);
                            bool shouldExport = _pageNames.Any(name => 
                                pageTitle.Contains(name, StringComparison.OrdinalIgnoreCase) ||
                                name.Contains(pageTitle, StringComparison.OrdinalIgnoreCase));
                            
                            if (!shouldExport)
                            {
                                continue; // Skip this page
                            }
                        }

                        await ExportPage((Page)page, databaseFolder);

                        // Check if we've hit the page limit
                        if (_pageLimit > 0 && _pageCount >= _pageLimit)
                        {
                            Console.WriteLine($"Reached page limit of {_pageLimit}. Stopping export.");
                            return;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error exporting database {databaseId}: {ex.Message}");
                throw;
            }
        }

        private async Task ExportPage(Page page, string folderPath)
        {
            try
            {
                // Try to get the page title
                string pageTitle = ExtractPageTitle(page);
                if (string.IsNullOrWhiteSpace(pageTitle))
                {
                    pageTitle = page.Id;
                }

                // Create a safe filename
                string safeFileName = MakeSafeFileName(pageTitle);
                string filePath = Path.Combine(folderPath, $"{safeFileName}.md");

                // Create a folder for images
                string imagesFolder = Path.Combine(folderPath, $"{safeFileName}_images");

                // Create markdown content
                StringBuilder markdown = new StringBuilder();

                // Add title
                markdown.AppendLine($"# {pageTitle}");
                markdown.AppendLine();

                // Add metadata section
                markdown.AppendLine("## Metadata");
                foreach (var prop in page.Properties)
                {
                    string propName = prop.Key;
                    string propValue = await ConvertPropertyToText(prop.Value);

                    if (!string.IsNullOrEmpty(propValue))
                    {
                        markdown.AppendLine($"- **{propName}**: {propValue}");
                    }
                }

                markdown.AppendLine();

                // Get page content
                try
                {
                    // Get blocks
                    var blockResponse = await _client.Blocks.RetrieveChildrenAsync(
                        new BlockRetrieveChildrenRequest
                        {
                            BlockId = page.Id
                        });

                    // Process each block
                    foreach (var block in blockResponse.Results)
                    {
                        await ProcessBlockToMarkdown((Block)block, markdown, imagesFolder);
                    }
                }
                catch (Exception blockEx)
                {
                    markdown.AppendLine($"*Error retrieving page content: {blockEx.Message}*");
                }

                // Write the markdown file
                await File.WriteAllTextAsync(filePath, markdown.ToString());

                _pageCount++;
                Console.WriteLine($"Exported page {_pageCount}: {pageTitle} at {filePath}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error exporting page {page.Id}: {ex.Message}");
                // Continue with other pages
            }
        }

        private async Task ProcessBlockToMarkdown(Block block, StringBuilder markdown, string imagesFolder)
        {
            try
            {
                // Debug: Log block type to help identify missing content
                Console.WriteLine($"Processing block type: {block.Type} - ID: {block.Id}");
                
                // Handle different block types
                switch (block.Type.ToString().ToLower())
                {
                    case "paragraph":
                        string paragraphText = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(paragraphText))
                        {
                            markdown.AppendLine(paragraphText);
                            markdown.AppendLine();
                        }

                        break;

                    case "heading_1":
                        string h1Text = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(h1Text))
                        {
                            markdown.AppendLine($"# {h1Text}");
                            markdown.AppendLine();
                        }

                        break;

                    case "heading_2":
                        string h2Text = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(h2Text))
                        {
                            markdown.AppendLine($"## {h2Text}");
                            markdown.AppendLine();
                        }

                        break;

                    case "heading_3":
                        string h3Text = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(h3Text))
                        {
                            markdown.AppendLine($"### {h3Text}");
                            markdown.AppendLine();
                        }

                        break;

                    case "code":
                        try
                        {
                            // Debug: Log code block processing
                            Console.WriteLine($"Processing code block - ID: {block.Id}");
                            
                            // Try using native block properties first
                            var codeBlock = block as CodeBlock;
                            string language = "";
                            string codeContent = "";
                            
                            if (codeBlock?.Code != null)
                            {
                                // Use native properties when available
                                language = codeBlock.Code.Language ?? "";
                                if (codeBlock.Code.RichText != null)
                                {
                                    codeContent = string.Join("", codeBlock.Code.RichText.Select(rt => rt.PlainText ?? ""));
                                }
                            }
                            else
                            {
                                // Fallback to JSON parsing with NaN handling
                                try
                                {
                                    string blockString = block.ToString();
                                    // Replace NaN values that cause JSON parsing issues
                                    blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                                    
                                    var blockJson = Newtonsoft.Json.Linq.JObject.Parse(blockString);
                                    language = blockJson["code"]?["language"]?.ToString() ?? "";
                                    
                                    if (blockJson["code"]?["rich_text"] is Newtonsoft.Json.Linq.JArray richTextArray)
                                    {
                                        foreach (var textItem in richTextArray)
                                        {
                                            codeContent += textItem["plain_text"]?.ToString() ?? "";
                                        }
                                    }
                                }
                                catch
                                {
                                    // If all else fails, try to extract content generically
                                    codeContent = ExtractRichText(block);
                                    language = "";
                                }
                            }
                            
                            // Clean up language - remove "plain text" and use proper language identifiers
                            if (language.ToLowerInvariant() == "plain text" || string.IsNullOrWhiteSpace(language))
                            {
                                language = ""; // Empty string for generic code blocks
                            }
                            
                            // Debug: Log extracted content
                            Console.WriteLine($"Code block content: '{codeContent}' (length: {codeContent?.Length ?? 0})");

                            if (!string.IsNullOrEmpty(codeContent) && codeContent.Trim().Length > 0)
                            {
                                // Only create code blocks for substantial content
                                // Skip if content is just a title or very short phrase
                                if (codeContent.Trim().Length > 3 && !codeContent.Trim().Equals("Platform Interface Module", StringComparison.OrdinalIgnoreCase))
                                {
                                    markdown.AppendLine($"```{language}");
                                    markdown.AppendLine(codeContent);
                                    markdown.AppendLine("```");
                                    markdown.AppendLine();
                                }
                                else
                                {
                                    // Treat short or title-like content as regular text
                                    markdown.AppendLine(codeContent);
                                    markdown.AppendLine();
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            // Last resort: just add the error and continue
                            markdown.AppendLine("```");
                            markdown.AppendLine($"// Error processing code block: {ex.Message}");
                            markdown.AppendLine("```");
                            markdown.AppendLine();
                        }
                        break;

                    case "bulletedlistitem":
                    case "bulleted_list_item":
                        string bulletText = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(bulletText))
                        {
                            markdown.AppendLine($"- {bulletText}");
                            markdown.AppendLine();
                        }

                        break;

                    case "numbered_list_item":
                    case "numberedlistitem":
                        try
                        {
                            // Try direct casting first
                            var numberedBlock = block as NumberedListItemBlock;
                            string numberedText = "";
                            
                            if (numberedBlock != null && numberedBlock.NumberedListItem?.RichText != null)
                            {
                                numberedText = string.Join("", numberedBlock.NumberedListItem.RichText.Select(rt => rt.PlainText ?? ""));
                            }
                            else
                            {
                                // Fallback to ExtractRichText
                                numberedText = ExtractRichText(block);
                            }

                            if (!string.IsNullOrEmpty(numberedText))
                            {
                                markdown.AppendLine($"1. {numberedText}");
                                
                                // Process child blocks if any (for nested lists)
                                if (block.HasChildren)
                                {
                                    markdown.AppendLine();
                                    var childBlocks = await _client.Blocks.RetrieveChildrenAsync(
                                        new BlockRetrieveChildrenRequest { BlockId = block.Id });
                                    
                                    foreach (var childBlock in childBlocks.Results)
                                    {
                                        // Add indentation for nested items
                                        if (childBlock.Type.ToString().ToLower() == "numbered_list_item")
                                        {
                                            markdown.Append("   "); // 3 spaces for indentation
                                        }
                                        await ProcessBlockToMarkdown((Block)childBlock, markdown, imagesFolder);
                                    }
                                }
                                else
                                {
                                    markdown.AppendLine();
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            markdown.AppendLine($"*Error processing numbered list item: {ex.Message}*");
                            markdown.AppendLine();
                        }
                        break;

                    case "to_do":
                        string todoText = ExtractRichText(block);
                        bool isChecked = false;

                        // Try to extract if it's checked
                        try
                        {
                            string blockString = block.ToString();
                            // Replace NaN values that cause JSON parsing issues
                            blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                            
                            var todoObject = Newtonsoft.Json.Linq.JObject.Parse(blockString);
                            isChecked = todoObject["to_do"]?["checked"]?.ToObject<bool>() ?? false;
                        }
                        catch
                        {
                        }

                        if (!string.IsNullOrEmpty(todoText))
                        {
                            markdown.AppendLine($"- [{(isChecked ? "x" : " ")}] {todoText}");
                            markdown.AppendLine();
                        }

                        break;

                    case "toggle":
                        Console.WriteLine($"Processing toggle block - ID: {block.Id}, HasChildren: {block.HasChildren}");
                        string toggleText = ExtractRichText(block);
                        Console.WriteLine($"Toggle text: '{toggleText}'");
                        
                        if (!string.IsNullOrEmpty(toggleText))
                        {
                            // Use HTML details tag for better compatibility
                            // Alternative: Use markdown-style with indentation
                            markdown.AppendLine($"<details><summary>{toggleText}</summary>");
                            markdown.AppendLine();
                            
                            // Alternative markdown format (commented out):
                            // markdown.AppendLine($"▶ **{toggleText}**");
                            // markdown.AppendLine();

                            if (block.HasChildren)
                            {
                                try
                                {
                                    Console.WriteLine($"Retrieving toggle child blocks...");
                                    var childBlocks = await _client.Blocks.RetrieveChildrenAsync(
                                        new BlockRetrieveChildrenRequest
                                        {
                                            BlockId = block.Id
                                        });
                                    
                                    Console.WriteLine($"Found {childBlocks.Results.Count} child blocks in toggle");
                                    
                                    foreach (var childBlock in childBlocks.Results)
                                    {
                                        await ProcessBlockToMarkdown((Block)childBlock, markdown, imagesFolder);
                                    }
                                    
                                    // Handle pagination for child blocks
                                    while (childBlocks.HasMore && !string.IsNullOrEmpty(childBlocks.NextCursor))
                                    {
                                        Console.WriteLine($"Fetching more toggle child blocks (cursor: {childBlocks.NextCursor})...");
                                        childBlocks = await _client.Blocks.RetrieveChildrenAsync(
                                            new BlockRetrieveChildrenRequest
                                            {
                                                BlockId = block.Id,
                                                StartCursor = childBlocks.NextCursor
                                            });
                                        
                                        Console.WriteLine($"Found {childBlocks.Results.Count} more child blocks");
                                        
                                        foreach (var childBlock in childBlocks.Results)
                                        {
                                            await ProcessBlockToMarkdown((Block)childBlock, markdown, imagesFolder);
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    Console.WriteLine($"Error retrieving toggle content: {ex.Message}");
                                    markdown.AppendLine($"*Error retrieving toggle content: {ex.Message}*");
                                }
                            }
                            else
                            {
                                Console.WriteLine("Toggle has no children marked");
                                markdown.AppendLine("*Toggle content not available*");
                            }

                            markdown.AppendLine("</details>");
                            markdown.AppendLine();
                        }
                        else
                        {
                            Console.WriteLine("Toggle has no text - skipping");
                        }

                        break;

                    case "quote":
                        string quoteText = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(quoteText))
                        {
                            markdown.AppendLine($"> {quoteText}");
                            markdown.AppendLine();
                        }
                        break;

                    case "callout":
                        try
                        {
                            string blockString = block.ToString();
                            // Replace NaN values that cause JSON parsing issues
                            blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                            
                            var calloutJson = Newtonsoft.Json.Linq.JObject.Parse(blockString);
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

                    case "divider":
                        markdown.AppendLine("---");
                        markdown.AppendLine();
                        break;

                    case "image":
                        await ProcessImageBlock(block, markdown, imagesFolder);
                        break;

                    default:
                        // For unsupported block types, add a note with more debugging info
                        string blockContent = ExtractRichText(block);
                        if (!string.IsNullOrEmpty(blockContent))
                        {
                            // If there's content, treat it as a paragraph
                            markdown.AppendLine(blockContent);
                            markdown.AppendLine();
                        }
                        else
                        {
                            // Add debugging info for empty unsupported blocks
                            markdown.AppendLine($"*Unsupported block type: {block.Type}*");
                            markdown.AppendLine();
                        }
                        break;
                }

                // Process child blocks (if any and not already processed)
                if (block.HasChildren && block.Type.ToString() != "toggle")
                {
                    try
                    {
                        var childBlocks = await _client.Blocks.RetrieveChildrenAsync(
                            new BlockRetrieveChildrenRequest
                            {
                                BlockId = block.Id
                            });
                        foreach (var childBlock in childBlocks.Results)
                        {
                            await ProcessBlockToMarkdown((Block)childBlock, markdown, imagesFolder);
                        }
                        
                        // Handle pagination for child blocks
                        while (childBlocks.HasMore && !string.IsNullOrEmpty(childBlocks.NextCursor))
                        {
                            childBlocks = await _client.Blocks.RetrieveChildrenAsync(
                                new BlockRetrieveChildrenRequest
                                {
                                    BlockId = block.Id,
                                    StartCursor = childBlocks.NextCursor
                                });
                            
                            foreach (var childBlock in childBlocks.Results)
                            {
                                await ProcessBlockToMarkdown((Block)childBlock, markdown, imagesFolder);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        markdown.AppendLine($"*Error retrieving child blocks: {ex.Message}*");
                        markdown.AppendLine();
                    }
                }
            }
            catch (Exception ex)
            {
                markdown.AppendLine($"*Error processing block: {ex.Message}*");
                markdown.AppendLine();
            }
        }

        private async Task ProcessImageBlock(Block block, StringBuilder markdown, string imagesFolder)
        {
            try
            {
                // Try to extract image URL using JSON parsing with NaN handling
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var blockJson = Newtonsoft.Json.Linq.JObject.Parse(blockString);

                string imageUrl = null;
                string caption = string.Empty;

                // Try to get URL based on image type
                if (blockJson["image"]?["type"]?.ToString() == "file")
                {
                    imageUrl = blockJson["image"]?["file"]?["url"]?.ToString();
                }
                else if (blockJson["image"]?["type"]?.ToString() == "external")
                {
                    imageUrl = blockJson["image"]?["external"]?["url"]?.ToString();
                }

                // Try to get caption
                if (blockJson["image"]?["caption"] != null)
                {
                    var captionArray = blockJson["image"]?["caption"];
                    if (captionArray is Newtonsoft.Json.Linq.JArray arr && arr.Count > 0)
                    {
                        caption = string.Join("", arr.Select(rt => rt["plain_text"]?.ToString() ?? ""));
                    }
                }

                if (string.IsNullOrEmpty(imageUrl))
                {
                    markdown.AppendLine("*Image URL could not be extracted*");
                    markdown.AppendLine();
                    return;
                }

                // Download and save image
                try
                {
                    // Create images folder if it doesn't exist
                    Directory.CreateDirectory(imagesFolder);

                    // Generate a filename
                    string imageExtension = Path.GetExtension(new Uri(imageUrl).AbsolutePath);
                    if (string.IsNullOrEmpty(imageExtension)) imageExtension = ".jpg";

                    string imageFileName = $"image_{Guid.NewGuid().ToString("N").Substring(0, 8)}{imageExtension}";
                    string localImagePath = Path.Combine(imagesFolder, imageFileName);

                    // Download image
                    byte[] imageData = await _httpClient.GetByteArrayAsync(imageUrl);
                    await File.WriteAllBytesAsync(localImagePath, imageData);

                    // Generate relative path for the markdown file
                    string relativePath = Path.GetFileName(imagesFolder) + "/" + imageFileName;

                    // Add image to markdown
                    markdown.AppendLine($"![{caption}]({relativePath})");
                    markdown.AppendLine();
                }
                catch (Exception ex)
                {
                    // Fallback to using the original URL
                    markdown.AppendLine($"![{caption}]({imageUrl})");
                    markdown.AppendLine($"*Error downloading image: {ex.Message}*");
                    markdown.AppendLine();
                }
            }
            catch (Exception ex)
            {
                markdown.AppendLine($"*Error processing image block: {ex.Message}*");
                markdown.AppendLine();
            }
        }

        private string ExtractRichText(Block block)
        {
            try
            {
                // Use JSON parsing to handle all block types consistently, with NaN handling
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var blockJson = Newtonsoft.Json.Linq.JObject.Parse(blockString);
                string blockType = block.Type.ToString().ToLower();
                
                Newtonsoft.Json.Linq.JArray richTextArray = null;
                
                // Get rich text array based on block type
                switch (blockType)
                {
                    case "paragraph":
                        richTextArray = blockJson["paragraph"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "heading_1":
                        richTextArray = blockJson["heading_1"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "heading_2":
                        richTextArray = blockJson["heading_2"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "heading_3":
                        richTextArray = blockJson["heading_3"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "bulleted_list_item":
                    case "bulletedlistitem":
                        richTextArray = blockJson["bulleted_list_item"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "numbered_list_item":
                    case "numberedlistitem":
                        richTextArray = blockJson["numbered_list_item"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "to_do":
                        richTextArray = blockJson["to_do"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "toggle":
                        richTextArray = blockJson["toggle"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "quote":
                        richTextArray = blockJson["quote"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    case "callout":
                        richTextArray = blockJson["callout"]?["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        break;
                    default:
                        // Try to find rich_text in the block object generically
                        var blockContent = blockJson[blockType];
                        if (blockContent != null)
                        {
                            richTextArray = blockContent["rich_text"] as Newtonsoft.Json.Linq.JArray;
                        }
                        break;
                }

                if (richTextArray == null || richTextArray.Count == 0)
                {
                    return string.Empty;
                }

                // Extract plain text and basic formatting
                var textParts = new List<string>();
                foreach (var textItem in richTextArray)
                {
                    string plainText = textItem["plain_text"]?.ToString() ?? "";
                    var annotations = textItem["annotations"];

                    // Apply basic formatting
                    if (annotations != null)
                    {
                        bool isBold = annotations["bold"]?.ToObject<bool>() ?? false;
                        bool isItalic = annotations["italic"]?.ToObject<bool>() ?? false;
                        bool isStrikethrough = annotations["strikethrough"]?.ToObject<bool>() ?? false;
                        bool isCode = annotations["code"]?.ToObject<bool>() ?? false;
                        
                        if (isBold)
                            plainText = $"**{plainText}**";
                        if (isItalic)
                            plainText = $"*{plainText}*";
                        if (isStrikethrough)
                            plainText = $"~~{plainText}~~";
                        if (isCode)
                            plainText = $"`{plainText}`";
                    }

                    // Add links
                    string href = textItem["href"]?.ToString();
                    if (!string.IsNullOrEmpty(href))
                    {
                        plainText = $"[{plainText}]({href})";
                    }

                    textParts.Add(plainText);
                }

                return string.Join("", textParts);
            }
            catch
            {
                return string.Empty;
            }
        }

        private async Task<string> ConvertPropertyToText(PropertyValue property)
        {
            try
            {
                switch (property)
                {
                    case TitlePropertyValue titleProp:
                        return string.Join("", titleProp.Title.Select(t => t.PlainText));

                    case RichTextPropertyValue textProp:
                        return string.Join("", textProp.RichText.Select(t => t.PlainText));

                    case NumberPropertyValue numberProp:
                        return numberProp.Number?.ToString() ?? "";

                    case SelectPropertyValue selectProp:
                        return selectProp.Select?.Name ?? "";

                    case MultiSelectPropertyValue multiSelectProp:
                        return string.Join(", ", multiSelectProp.MultiSelect.Select(s => s.Name));

                    case DatePropertyValue dateProp:
                        if (dateProp.Date == null) return "";
                        if (dateProp.Date.End != null)
                            return
                                $"{dateProp.Date.Start?.ToString("yyyy-MM-dd")} to {dateProp.Date.End?.ToString("yyyy-MM-dd")}";
                        return dateProp.Date.Start?.ToString("yyyy-MM-dd") ?? "";

                    case CheckboxPropertyValue checkboxProp:
                        return checkboxProp.Checkbox ? "✅" : "❌";

                    case UrlPropertyValue urlProp:
                        return urlProp.Url ?? "";

                    case EmailPropertyValue emailProp:
                        return emailProp.Email ?? "";

                    case PhoneNumberPropertyValue phoneProp:
                        return phoneProp.PhoneNumber ?? "";

                    case RelationPropertyValue relationProp:
                        return $"Related items: {relationProp.Relation.Count}";

                    default:
                        // For unsupported property types, use ToString
                        return property.ToString();
                }
            }
            catch (Exception ex)
            {
                return $"[Error: {ex.Message}]";
            }
        }

        private string ExtractPageTitle(Page page)
        {
            // Try to find a title property
            foreach (var prop in page.Properties)
            {
                if (prop.Value is TitlePropertyValue titleProp && titleProp.Title.Count > 0)
                {
                    return string.Join("", titleProp.Title.Select(t => t.PlainText));
                }
            }

            return page.Id;
        }

        private string MakeSafeFileName(string fileName)
        {
            // Remove invalid characters from filename
            foreach (char c in Path.GetInvalidFileNameChars())
            {
                fileName = fileName.Replace(c, '_');
            }

            // Limit filename length
            if (fileName.Length > 100)
            {
                fileName = fileName.Substring(0, 100);
            }

            return fileName;
        }
    }
}