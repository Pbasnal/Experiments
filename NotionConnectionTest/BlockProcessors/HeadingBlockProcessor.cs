using System;
using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes heading blocks (heading_1, heading_2, heading_3)
    /// </summary>
    public class HeadingBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "heading"; // This is a base - we handle all heading types
        
        public override bool CanProcess(string blockType)
        {
            return blockType.StartsWith("heading", StringComparison.OrdinalIgnoreCase);
        }
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string headingText = ExtractRichText(block);
            if (string.IsNullOrEmpty(headingText))
                return;
                
            string blockType = block.Type.ToString().ToLower();
            
            switch (blockType)
            {
                case "heading_1":
                    markdown.AppendLine($"# {headingText}");
                    break;
                case "heading_2":
                    markdown.AppendLine($"## {headingText}");
                    break;
                case "heading_3":
                    markdown.AppendLine($"### {headingText}");
                    break;
                default:
                    markdown.AppendLine($"## {headingText}"); // Default to h2
                    break;
            }
            
            markdown.AppendLine();
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

