using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Default fallback processor for unsupported block types
    /// </summary>
    public class DefaultBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "default";
        
        public override bool CanProcess(string blockType)
        {
            // This processor accepts any block type as fallback
            return true;
        }
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            // Try to extract any text content
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
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

