using System;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes to-do/checkbox blocks
    /// </summary>
    public class TodoBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "to_do";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string todoText = ExtractRichText(block);
            bool isChecked = false;

            // Try to extract if it's checked
            try
            {
                string blockString = block.ToString();
                // Replace NaN values that cause JSON parsing issues
                blockString = blockString.Replace(": NaN", ": null").Replace(":NaN", ":null");
                
                var todoObject = JObject.Parse(blockString);
                isChecked = todoObject["to_do"]?["checked"]?.ToObject<bool>() ?? false;
            }
            catch
            {
                // Ignore errors, default to unchecked
            }

            if (!string.IsNullOrEmpty(todoText))
            {
                markdown.AppendLine($"- [{(isChecked ? "x" : " ")}] {todoText}");
                markdown.AppendLine();
            }
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

