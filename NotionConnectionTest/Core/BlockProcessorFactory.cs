using System;
using System.Collections.Generic;
using System.Linq;

namespace NotionConnectionTest.Core
{
    /// <summary>
    /// Factory for resolving block processors
    /// </summary>
    public class BlockProcessorFactory : IBlockProcessorFactory
    {
        private readonly IEnumerable<IBlockProcessor> _processors;
        private readonly Dictionary<string, IBlockProcessor> _processorCache;
        
        public BlockProcessorFactory(IEnumerable<IBlockProcessor> processors)
        {
            _processors = processors;
            _processorCache = new Dictionary<string, IBlockProcessor>(StringComparer.OrdinalIgnoreCase);
        }
        
        public IBlockProcessor? GetProcessor(string blockType)
        {
            // Check cache first
            if (_processorCache.TryGetValue(blockType, out var cachedProcessor))
            {
                return cachedProcessor;
            }
            
            // Find processor that can handle this block type
            var processor = _processors.FirstOrDefault(p => p.CanProcess(blockType));
            
            // Cache result
            if (processor != null)
            {
                _processorCache[blockType] = processor;
            }
            
            return processor;
        }
        
        public IEnumerable<IBlockProcessor> GetAllProcessors()
        {
            return _processors;
        }
    }
}

