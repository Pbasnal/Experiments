using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes bulleted list item blocks
    /// </summary>
    public class BulletedListItemBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "bulleted_list_item";
        
        public override bool CanProcess(string blockType)
        {
            return blockType.Equals("bulleted_list_item", System.StringComparison.OrdinalIgnoreCase) ||
                   blockType.Equals("bulletedlistitem", System.StringComparison.OrdinalIgnoreCase);
        }
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string bulletText = ExtractRichText(block);
            if (!string.IsNullOrEmpty(bulletText))
            {
                markdown.AppendLine($"- {bulletText}");
                markdown.AppendLine();
            }
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

