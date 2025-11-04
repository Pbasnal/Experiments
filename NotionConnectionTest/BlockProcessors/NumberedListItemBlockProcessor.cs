using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes numbered list item blocks
    /// </summary>
    public class NumberedListItemBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "numbered_list_item";
        
        public override bool CanProcess(string blockType)
        {
            return blockType.Equals("numbered_list_item", System.StringComparison.OrdinalIgnoreCase) ||
                   blockType.Equals("numberedlistitem", System.StringComparison.OrdinalIgnoreCase);
        }
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string numberedText = ExtractRichText(block);
            if (!string.IsNullOrEmpty(numberedText))
            {
                markdown.AppendLine($"1. {numberedText}");
                markdown.AppendLine();
            }
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

