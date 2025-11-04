using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest
{
    /// <summary>
    /// Modular Notion to Markdown exporter using dependency injection
    /// </summary>
    public class ModularNotionExporter
    {
        private readonly NotionClient _client;
        private readonly string _exportPath;
        private readonly HttpClient _httpClient;
        private readonly IBlockProcessorFactory _processorFactory;
        private int _pageCount = 0;
        private readonly int _pageLimit;
        private readonly List<string> _pageNames;

        public ModularNotionExporter(
            NotionClient client,
            string exportPath,
            IBlockProcessorFactory processorFactory,
            HttpClient httpClient,
            int pageLimit = -1,
            List<string>? pageNames = null)
        {
            _client = client;
            _exportPath = exportPath;
            _processorFactory = processorFactory;
            _httpClient = httpClient;
            _pageLimit = pageLimit;
            _pageNames = pageNames ?? new List<string>();

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
                    if (!ShouldExportPage((Page)page))
                        continue;

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
                        if (!ShouldExportPage((Page)page))
                            continue;

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

        private bool ShouldExportPage(Page page)
        {
            if (_pageNames.Count == 0)
                return true;

            string pageTitle = ExtractPageTitle(page);
            return _pageNames.Any(name =>
                pageTitle.Contains(name, StringComparison.OrdinalIgnoreCase) ||
                name.Contains(pageTitle, StringComparison.OrdinalIgnoreCase));
        }

        private async Task ExportPage(Page page, string folderPath)
        {
            try
            {
                // Get the page title
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
                    // Create processing context
                    var context = new ProcessingContext(_client, _httpClient, imagesFolder, _processorFactory);

                    // Get blocks
                    var blockResponse = await _client.Blocks.RetrieveChildrenAsync(
                        new BlockRetrieveChildrenRequest
                        {
                            BlockId = page.Id
                        });

                    // Process each block using appropriate processor
                    foreach (var block in blockResponse.Results)
                    {
                        await ProcessBlock((Block)block, markdown, context);
                    }

                    // Handle pagination for blocks
                    while (blockResponse.HasMore && !string.IsNullOrEmpty(blockResponse.NextCursor))
                    {
                        blockResponse = await _client.Blocks.RetrieveChildrenAsync(
                            new BlockRetrieveChildrenRequest
                            {
                                BlockId = page.Id,
                                StartCursor = blockResponse.NextCursor
                            });

                        foreach (var block in blockResponse.Results)
                        {
                            await ProcessBlock((Block)block, markdown, context);
                        }
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

        private async Task ProcessBlock(Block block, StringBuilder markdown, IProcessingContext context)
        {
            try
            {
                Console.WriteLine($"Processing block type: {block.Type} - ID: {block.Id}");

                // Get appropriate processor
                var processor = _processorFactory.GetProcessor(block.Type.ToString());
                if (processor != null)
                {
                    await processor.ProcessAsync(block, markdown, context);
                }
                else
                {
                    Console.WriteLine($"No processor found for block type: {block.Type}");
                    markdown.AppendLine($"*Unsupported block type: {block.Type}*");
                    markdown.AppendLine();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error processing block {block.Id}: {ex.Message}");
                markdown.AppendLine($"*Error processing block: {ex.Message}*");
                markdown.AppendLine();
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
                            return $"{dateProp.Date.Start?.ToString("yyyy-MM-dd")} to {dateProp.Date.End?.ToString("yyyy-MM-dd")}";
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

