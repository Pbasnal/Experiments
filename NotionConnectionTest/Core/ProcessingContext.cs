using System.Net.Http;
using Notion.Client;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Implementation of processing context
    /// </summary>
    public class ProcessingContext : IProcessingContext
    {
        public NotionClient Client { get; }
        public HttpClient HttpClient { get; }
        public string ImagesFolder { get; }
        public IBlockProcessorFactory ProcessorFactory { get; }
        
        public ProcessingContext(
            NotionClient client,
            HttpClient httpClient,
            string imagesFolder,
            IBlockProcessorFactory processorFactory)
        {
            Client = client;
            HttpClient = httpClient;
            ImagesFolder = imagesFolder;
            ProcessorFactory = processorFactory;
        }
    }
}

