using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes quote blocks
    /// </summary>
    public class QuoteBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "quote";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            string quoteText = ExtractRichText(block);
            if (!string.IsNullOrEmpty(quoteText))
            {
                markdown.AppendLine($"> {quoteText}");
                markdown.AppendLine();
            }
            
            // Process child blocks if any
            await ProcessChildBlocksAsync(block, markdown, context);
        }
    }
}

