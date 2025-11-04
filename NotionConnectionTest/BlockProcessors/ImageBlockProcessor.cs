using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes image blocks
    /// </summary>
    public class ImageBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "image";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            try
            {
                // Try to extract image URL using JSON parsing with NaN handling
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var blockJson = JObject.Parse(blockString);

                string? imageUrl = null;
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
                    if (captionArray is JArray arr && arr.Count > 0)
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
                    Directory.CreateDirectory(context.ImagesFolder);

                    // Generate a filename
                    string imageExtension = Path.GetExtension(new Uri(imageUrl).AbsolutePath);
                    if (string.IsNullOrEmpty(imageExtension)) imageExtension = ".jpg";

                    string imageFileName = $"image_{Guid.NewGuid().ToString("N").Substring(0, 8)}{imageExtension}";
                    string localImagePath = Path.Combine(context.ImagesFolder, imageFileName);

                    // Download image
                    byte[] imageData = await context.HttpClient.GetByteArrayAsync(imageUrl);
                    await File.WriteAllBytesAsync(localImagePath, imageData);

                    // Generate relative path for the markdown file
                    string relativePath = Path.GetFileName(context.ImagesFolder) + "/" + imageFileName;

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
    }
}

