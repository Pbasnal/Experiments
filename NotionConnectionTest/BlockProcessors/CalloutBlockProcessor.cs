using System;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes callout blocks
    /// </summary>
    public class CalloutBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "callout";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            try
            {
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var calloutJson = JObject.Parse(blockString);
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
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

