using Microsoft.Extensions.DependencyInjection;
using NotionConnectionTest.BlockProcessors;
using System.Net.Http;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Configuration for dependency injection
    /// </summary>
    public static class ServiceConfiguration
    {
        /// <summary>
        /// Registers all services for dependency injection
        /// </summary>
        public static IServiceCollection ConfigureServices(this IServiceCollection services)
        {
            // Register HttpClient
            services.AddSingleton<HttpClient>();
            
            // Register block processors - order matters! DefaultBlockProcessor should be last
            services.AddTransient<IBlockProcessor, ParagraphBlockProcessor>();
            services.AddTransient<IBlockProcessor, HeadingBlockProcessor>();
            services.AddTransient<IBlockProcessor, CodeBlockProcessor>();
            services.AddTransient<IBlockProcessor, BulletedListItemBlockProcessor>();
            services.AddTransient<IBlockProcessor, NumberedListItemBlockProcessor>();
            services.AddTransient<IBlockProcessor, TodoBlockProcessor>();
            services.AddTransient<IBlockProcessor, ToggleBlockProcessor>();
            services.AddTransient<IBlockProcessor, QuoteBlockProcessor>();
            services.AddTransient<IBlockProcessor, CalloutBlockProcessor>();
            services.AddTransient<IBlockProcessor, DividerBlockProcessor>();
            services.AddTransient<IBlockProcessor, ImageBlockProcessor>();
            services.AddTransient<IBlockProcessor, DefaultBlockProcessor>(); // Fallback - must be last
            
            // Register factory
            services.AddSingleton<IBlockProcessorFactory, BlockProcessorFactory>();
            
            return services;
        }
    }
}

