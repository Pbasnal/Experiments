using System.Text;
using System.Threading.Tasks;
using Notion.Client;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Interface for processing different types of Notion blocks and converting them to Markdown
    /// </summary>
    public interface IBlockProcessor
    {
        /// <summary>
        /// Gets the block type that this processor handles
        /// </summary>
        string BlockType { get; }
        
        /// <summary>
        /// Processes a Notion block and appends the markdown representation to the StringBuilder
        /// </summary>
        /// <param name="block">The Notion block to process</param>
        /// <param name="markdown">The StringBuilder to append markdown to</param>
        /// <param name="context">Processing context with shared resources</param>
        /// <returns>Task for async operation</returns>
        Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context);
        
        /// <summary>
        /// Determines if this processor can handle the given block type
        /// </summary>
        /// <param name="blockType">The block type to check</param>
        /// <returns>True if this processor can handle the block type</returns>
        bool CanProcess(string blockType);
    }
}

