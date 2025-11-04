using System.Net.Http;
using System.Threading.Tasks;
using Notion.Client;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Context for processing blocks, providing access to shared resources
    /// </summary>
    public interface IProcessingContext
    {
        /// <summary>
        /// Notion API client
        /// </summary>
        NotionClient Client { get; }
        
        /// <summary>
        /// HTTP client for downloading images
        /// </summary>
        HttpClient HttpClient { get; }
        
        /// <summary>
        /// Base path for images
        /// </summary>
        string ImagesFolder { get; }
        
        /// <summary>
        /// Processor factory for resolving child block processors
        /// </summary>
        IBlockProcessorFactory ProcessorFactory { get; }
    }
}

