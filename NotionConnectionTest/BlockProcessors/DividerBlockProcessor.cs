using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes divider blocks
    /// </summary>
    public class DividerBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "divider";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            markdown.AppendLine("---");
            markdown.AppendLine();
            
            await Task.CompletedTask;
        }
    }
}

