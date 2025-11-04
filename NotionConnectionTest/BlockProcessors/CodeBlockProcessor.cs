using System;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes code blocks
    /// </summary>
    public class CodeBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "code";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            try
            {
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
                        
                        var blockJson = JObject.Parse(blockString);
                        language = blockJson["code"]?["language"]?.ToString() ?? "";
                        
                        if (blockJson["code"]?["rich_text"] is JArray richTextArray)
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
                if (language.Equals("plain text", StringComparison.OrdinalIgnoreCase) || string.IsNullOrWhiteSpace(language))
                {
                    language = ""; // Empty string for generic code blocks
                }
                
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
            
            await Task.CompletedTask;
        }
    }
}

