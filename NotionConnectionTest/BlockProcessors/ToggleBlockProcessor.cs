using System;
using System.Text;
using System.Threading.Tasks;
using Notion.Client;
using NotionConnectionTest.Core;

namespace NotionConnectionTest.BlockProcessors
{
    /// <summary>
    /// Processes toggle/collapsible blocks
    /// </summary>
    public class ToggleBlockProcessor : BlockProcessorBase
    {
        public override string BlockType => "toggle";
        
        public override async Task ProcessAsync(Block block, StringBuilder markdown, IProcessingContext context)
        {
            Console.WriteLine($"Processing toggle block - ID: {block.Id}, HasChildren: {block.HasChildren}");
            string toggleText = ExtractRichText(block);
            Console.WriteLine($"Toggle text: '{toggleText}'");
            
            if (!string.IsNullOrEmpty(toggleText))
            {
                // Use HTML details tag for collapsible sections
                markdown.AppendLine($"<details><summary>{toggleText}</summary>");
                markdown.AppendLine();

                if (block.HasChildren)
                {
                    try
                    {
                        Console.WriteLine($"Retrieving toggle child blocks...");
                        var childBlocks = await context.Client.Blocks.RetrieveChildrenAsync(
                            new BlockRetrieveChildrenRequest
                            {
                                BlockId = block.Id
                            });
                        
                        Console.WriteLine($"Found {childBlocks.Results.Count} child blocks in toggle");
                        
                        foreach (var childBlock in childBlocks.Results)
                        {
                            var processor = context.ProcessorFactory.GetProcessor(childBlock.Type.ToString());
                            if (processor != null)
                            {
                                await processor.ProcessAsync((Block)childBlock, markdown, context);
                            }
                        }
                        
                        // Handle pagination for child blocks
                        while (childBlocks.HasMore && !string.IsNullOrEmpty(childBlocks.NextCursor))
                        {
                            Console.WriteLine($"Fetching more toggle child blocks (cursor: {childBlocks.NextCursor})...");
                            childBlocks = await context.Client.Blocks.RetrieveChildrenAsync(
                                new BlockRetrieveChildrenRequest
                                {
                                    BlockId = block.Id,
                                    StartCursor = childBlocks.NextCursor
                                });
                            
                            Console.WriteLine($"Found {childBlocks.Results.Count} more child blocks");
                            
                            foreach (var childBlock in childBlocks.Results)
                            {
                                var processor = context.ProcessorFactory.GetProcessor(childBlock.Type.ToString());
                                if (processor != null)
                                {
                                    await processor.ProcessAsync((Block)childBlock, markdown, context);
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Error retrieving toggle content: {ex.Message}");
                        markdown.AppendLine($"*Error retrieving toggle content: {ex.Message}*");
                    }
                }
                else
                {
                    Console.WriteLine("Toggle has no children marked");
                    markdown.AppendLine("*Toggle content not available*");
                }

                markdown.AppendLine("</details>");
                markdown.AppendLine();
            }
            else
            {
                Console.WriteLine("Toggle has no text - skipping");
            }
        }
    }
}

