using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes paragraph blocks
    /// </summary>
    public class ParagraphBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "paragraph";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string paragraphText = ExtractRichText(block);
            if (!string.IsNullOrEmpty(paragraphText))
            {
                markdown.AppendLine(paragraphText);
                markdown.AppendLine();
            }
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

