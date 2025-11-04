using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Notion.Client;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Base class for block processors with common functionality
    /// </summary>
    public abstract class BlockProcessorBase : IBlockProcessor
    {
        /// <inheritdoc/>
        public abstract string BlockType { get; }
        
        /// <inheritdoc/>
        public abstract Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context);
        
        /// <inheritdoc/>
        public virtual bool CanProcess(string blockType)
        {
            return blockType.Equals(BlockType, StringComparison.OrdinalIgnoreCase);
        }
        
        /// <summary>
        /// Extracts rich text from a block and formats it as markdown
        /// </summary>
        protected string ExtractRichText(Block block)
        {
            try
            {
                // Use JSON parsing to handle all block types consistently, with NaN handling
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var blockJson = JObject.Parse(blockString);
                string blockType = block.Type.ToString().ToLower();
                
                JArray? richTextArray = null;
                
                // Get rich text array based on block type
                switch (blockType)
                {
                    case "paragraph":
                        richTextArray = blockJson["paragraph"]?["rich_text"] as JArray;
                        break;
                    case "heading_1":
                        richTextArray = blockJson["heading_1"]?["rich_text"] as JArray;
                        break;
                    case "heading_2":
                        richTextArray = blockJson["heading_2"]?["rich_text"] as JArray;
                        break;
                    case "heading_3":
                        richTextArray = blockJson["heading_3"]?["rich_text"] as JArray;
                        break;
                    case "bulleted_list_item":
                    case "bulletedlistitem":
                        richTextArray = blockJson["bulleted_list_item"]?["rich_text"] as JArray;
                        break;
                    case "numbered_list_item":
                    case "numberedlistitem":
                        richTextArray = blockJson["numbered_list_item"]?["rich_text"] as JArray;
                        break;
                    case "to_do":
                        richTextArray = blockJson["to_do"]?["rich_text"] as JArray;
                        break;
                    case "toggle":
                        richTextArray = blockJson["toggle"]?["rich_text"] as JArray;
                        break;
                    case "quote":
                        richTextArray = blockJson["quote"]?["rich_text"] as JArray;
                        break;
                    case "callout":
                        richTextArray = blockJson["callout"]?["rich_text"] as JArray;
                        break;
                    default:
                        // Try to find rich_text in the block object generically
                        var blockContent = blockJson[blockType];
                        if (blockContent != null)
                        {
                            richTextArray = blockContent["rich_text"] as JArray;
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
                    string? href = textItem["href"]?.ToString();
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
        
        /// <summary>
        /// Processes child blocks recursively
        /// </summary>
        protected async Task ProcessChildBlocksAsync(Block parentBlock, StringBuilder markdown, IProcessingContext context)
        {
            if (!parentBlock.HasChildren)
                return;
                
            try
            {
                var childBlocks = await context.Client.Blocks.RetrieveChildrenAsync(
                    new BlockRetrieveChildrenRequest
                    {
                        BlockId = parentBlock.Id
                    });
                    
                foreach (var childBlock in childBlocks.Results)
                {
                    var processor = context.ProcessorFactory.GetProcessor(childBlock.Type.ToString());
                    if (processor != null)
                    {
                        await processor.ProcessAsync((Block)childBlock, markdown, context);
                    }
                }
                
                // Handle pagination for child blocks
                while (childBlocks.HasMore && !string.IsNullOrEmpty(childBlocks.NextCursor))
                {
                    childBlocks = await context.Client.Blocks.RetrieveChildrenAsync(
                        new BlockRetrieveChildrenRequest
                        {
                            BlockId = parentBlock.Id,
                            StartCursor = childBlocks.NextCursor
                        });
                    
                    foreach (var childBlock in childBlocks.Results)
                    {
                        var processor = context.ProcessorFactory.GetProcessor(childBlock.Type.ToString());
                        if (processor != null)
                        {
                            await processor.ProcessAsync((Block)childBlock, markdown, context);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error retrieving child blocks: {ex.Message}");
                markdown.AppendLine($"*Error retrieving child blocks: {ex.Message}*");
                markdown.AppendLine();
            }
        }
    }
}

