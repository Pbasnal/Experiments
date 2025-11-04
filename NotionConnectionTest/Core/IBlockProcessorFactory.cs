using System.Collections.Generic;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Factory for resolving block processors based on block type
    /// </summary>
    public interface IBlockProcessorFactory
    {
        /// <summary>
        /// Gets a processor for the specified block type
        /// </summary>
        /// <param name="blockType">The block type to get a processor for</param>
        /// <returns>The processor, or null if no processor is available</returns>
        IBlockProcessor? GetProcessor(string blockType);
        
        /// <summary>
        /// Gets all registered processors
        /// </summary>
        /// <returns>Collection of all registered processors</returns>
        IEnumerable<IBlockProcessor> GetAllProcessors();
    }
}

