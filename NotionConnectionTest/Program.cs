// See https://aka.ms/new-console-template for more information
using System.Formats.Asn1;
using System.Net.Http.Headers;
using Microsoft.Extensions.DependencyInjection;
using Newtonsoft.Json.Linq;
using Notion.Client;
using Spectre.Console;
using NotionConnectionTest.Core;

namespace NotionConnectionTest;
public class Program
{
    [STAThread]
    public static async Task Main(string[] args)
    {
        AnsiConsole.MarkupLine("[bold green]Notion to Markdown Converter[/]");
        
        // Parse command line arguments
        string exportPath = "export";
        string databaseId = "12aa6a034791464aa49f8c4b13f5ff88";
        bool showHelp = false;
        int pageLimit = -1;
        List<string> pageNames = new List<string>();
        
        for (int i = 0; i < args.Length; i++)
        {
            if (args[i] == "-h" || args[i] == "--help")
            {
                showHelp = true;
                break;
            }
            else if ((args[i] == "-o" || args[i] == "--output") && i + 1 < args.Length)
            {
                exportPath = args[i + 1];
                i++;
            }
            else if ((args[i] == "-d" || args[i] == "--database") && i + 1 < args.Length)
            {
                databaseId = args[i + 1];
                i++;
            }
            else if ((args[i] == "-l" || args[i] == "--limit") && i + 1 < args.Length)
            {
                if (int.TryParse(args[i + 1], out int limit))
                {
                    pageLimit = limit;
                }
                else
                {
                    AnsiConsole.MarkupLine($"[bold red]Error:[/] Invalid page limit value: {args[i + 1]}");
                    return;
                }
                i++;
            }
            else if ((args[i] == "-p" || args[i] == "--pages") && i + 1 < args.Length)
            {
                // Split comma-separated page names
                string[] names = args[i + 1].Split(',', StringSplitOptions.RemoveEmptyEntries);
                foreach (string name in names)
                {
                    pageNames.Add(name.Trim());
                }
                i++;
            }
        }
        
        if (showHelp)
        {
            ShowHelp();
            return;
        }
        
        if (string.IsNullOrEmpty(databaseId))
        {
            // If no database ID provided via command line, prompt user
            databaseId = AnsiConsole.Ask<string>("Enter the [green]Notion database ID[/] to export:");
        }
        
        // Get Notion client
        NotionClient client = GetNotionClient();
        
        try
        {
            // Test connection by getting users
            AnsiConsole.Status()
                .Start("Testing Notion API connection...", ctx => 
                {
                    PaginatedList<User> usersList = GetNotionUsers(client).Result;
                    ctx.Status("Connection successful!");
                    ctx.Spinner(Spinner.Known.Star);
                    ctx.SpinnerStyle(Style.Parse("green"));
                    Thread.Sleep(1000); // Show success message briefly
                });
            
            // Setup dependency injection
            var services = new ServiceCollection();
            services.ConfigureServices();
            var serviceProvider = services.BuildServiceProvider();
            
            // Get processor factory from DI container
            var processorFactory = serviceProvider.GetRequiredService<IBlockProcessorFactory>();
            var httpClient = serviceProvider.GetRequiredService<HttpClient>();
            
            // Create exporter using modular architecture
            var exporter = new ModularNotionExporter(client, exportPath, processorFactory, httpClient, pageLimit, pageNames);
            
            // Start conversion with progress display
            await AnsiConsole.Progress()
                .Columns(new ProgressColumn[] 
                {
                    new TaskDescriptionColumn(),
                    new ProgressBarColumn(),
                    new SpinnerColumn(),
                })
                .StartAsync(async ctx => 
                {
                    var task = ctx.AddTask($"Exporting database [green]{databaseId}[/]");
                    task.IsIndeterminate = true;
                    
                    // Display limit info if applicable
                    string limitInfo = pageLimit > 0 ? $" (limit: {pageLimit} pages)" : "";
                    AnsiConsole.MarkupLine($"Starting export{limitInfo}...");
                    
                    await exporter.ExportDatabase(databaseId);
                    
                    task.Value = 100;
                    task.StopTask();
                });
            
            AnsiConsole.MarkupLine($"[bold green]Export completed![/] Files saved to [blue]{Path.GetFullPath(exportPath)}[/]");
        }
        catch (Exception ex)
        {
            AnsiConsole.MarkupLine($"[bold red]Error:[/] {ex.Message}");
            if (ex.InnerException != null)
            {
                AnsiConsole.MarkupLine($"[red]Inner Exception:[/] {ex.InnerException.Message}");
            }
        }
    }
    
    private static void ShowHelp()
    {
        AnsiConsole.WriteLine("Notion to Markdown Converter");
        AnsiConsole.WriteLine("Usage: dotnet run [options]");
        AnsiConsole.WriteLine();
        AnsiConsole.WriteLine("Options:");
        AnsiConsole.WriteLine("  -h, --help                  Show this help message");
        AnsiConsole.WriteLine("  -o, --output <path>         Specify the output directory (default: 'export')");
        AnsiConsole.WriteLine("  -d, --database <id>         Specify the Notion database ID to export");
        AnsiConsole.WriteLine("  -l, --limit <number>        Limit the number of pages to export (default: no limit)");
        AnsiConsole.WriteLine("  -p, --pages <names>         Export only pages with specific names (comma-separated)");
        AnsiConsole.WriteLine();
        AnsiConsole.WriteLine("Examples:");
        AnsiConsole.WriteLine("  dotnet run -d 12aa6a034791464aa49f8c4b13f5ff88 -o my_notes");
        AnsiConsole.WriteLine("  dotnet run -d 12aa6a034791464aa49f8c4b13f5ff88 -l 5  # Export only 5 pages");
        AnsiConsole.WriteLine("  dotnet run -d 12aa6a034791464aa49f8c4b13f5ff88 -p \"Zig mouseless,Another Page\"");
    }

    public static void TestGraphicLibrary()
    {
        var adjacencyList = new Dictionary<string, HashSet<string>>
            {
                { "A", new HashSet<string> { "B", "C" } },
                { "B", new HashSet<string> { "A", "D" } },
                { "C", new HashSet<string> { "A" } },
                { "D", new HashSet<string> { "B", "C" } }
            };

        DrawGraph(adjacencyList);
    }

    static void DrawGraph(Dictionary<string, HashSet<string>> adjacencyList)
    {
        // Create a canvas
        var canvas = new Canvas(40, 20);
        var random = new Random();

        var positions = new Dictionary<string, (int x, int y)>();

        // Randomly position each vertex on the canvas
        foreach (var vertex in adjacencyList.Keys)
        {
            int x = random.Next(0, 40);
            int y = random.Next(0, 20);

            positions[vertex] = (x, y);

            // Draw the vertex
            canvas.SetPixel(x, y, Spectre.Console.Color.Red);
            canvas.SetPixel(x + 1, y, Spectre.Console.Color.White);
            canvas.SetPixel(x, y + 1, Spectre.Console.Color.White);
            canvas.SetPixel(x + 1, y + 1, Spectre.Console.Color.White);
        }

        // Draw the edges
        foreach (var edge in adjacencyList)
        {
            var start = positions[edge.Key];
            foreach (var target in edge.Value)
            {
                var end = positions[target];
                DrawLine(canvas, start.x, start.y, end.x, end.y);
            }
        }

        AnsiConsole.Write(canvas);
    }

    static void DrawLine(Canvas canvas, int x0, int y0, int x1, int y1)
    {
        int dx = Math.Abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.Abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy, e2;
        while (true)
        {
            canvas.SetPixel(x0, y0, Spectre.Console.Color.Green);
            if (x0 == x1 && y0 == y1) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    public static Dictionary<Page, HashSet<Page>> FetchNotionData(NotionClient client)
    {
        PaginatedList<User> usersList = GetNotionUsers(client).Result;

        foreach (User user in usersList.Results)
        {
            Console.WriteLine(user.Name);
        }

        Console.WriteLine("Hello, World!");

        // querying database
        // Date filter for page property called "When"
        DateFilter dateFilter = new("When", onOrAfter: DateTime.Now);
        string databaseId = "12aa6a034791464aa49f8c4b13f5ff88"; // database Id. If it's not working, click on 3 dots and add connection 

        var queryParams = new DatabasesQueryParameters();
        var response = client.Databases.QueryAsync(databaseId, queryParams).Result;
        var pages = response.Results;

        Dictionary<string, HashSet<string>> pageGraph = new();
        Dictionary<string, Page> pageRegister = new();

        int limit = -1;
        int currentPageCount = 0;
        foreach (Page page in pages)
        {
            // Console.WriteLine($"Reading page");
            RelationPropertyValue relationProperty = (RelationPropertyValue)page.Properties["Related Notes"];

            if (!pageRegister.ContainsKey(page.Id))
            {
                pageRegister.Add(page.Id, page);
                pageGraph.Add(page.Id, new());
            }

            if (relationProperty.Relation.Count == 0) continue;
            // Console.WriteLine(page.Url);
            foreach (ObjectId relatedPageId in relationProperty.Relation)
            {
                Console.WriteLine(relatedPageId.Id);
                if (!pageGraph[page.Id].Contains(relatedPageId.Id))
                {
                    pageGraph[page.Id].Add(relatedPageId.Id);
                }
            }

            // string[] pageNameWords = page.PublicUrl.Split('/').Last().Split('-');
            // string pageName = string.Join(" ", pageNameWords);
            // ID of related page - relationProperty.Relation[0].Id
            // Console.WriteLine(pageName);
            // Console.WriteLine("\t" + page.Properties["Related Notes"].Id);

            if (limit == -1) continue;
            else if (currentPageCount >= limit) break;
            else currentPageCount++;
        }

        int missingParentPageIds = 0;
        int missingChildPageIds = 0;
        Dictionary<Page, HashSet<Page>> pageGraphComplete = new();
        foreach (string pageId in pageGraph.Keys)
        {
            if (pageRegister.ContainsKey(pageId))
            {
                Page parentPage = pageRegister[pageId];
                HashSet<Page> childPages = new();
                foreach (string childPageId in pageGraph[pageId])
                {
                    if (pageRegister.ContainsKey(childPageId))
                    {
                        Page childPage = pageRegister[childPageId];
                        childPages.Add(childPage);
                    }
                    else
                    {
                        missingChildPageIds++;
                    }
                }
                pageGraphComplete.Add(parentPage, childPages);
            }
            else
            {
                missingParentPageIds++;
            }
        }

        Console.WriteLine($"Page graph contains {pageGraph.Count} elements");
        //https://www.notion.so/basnal17/12aa6a034791464aa49f8c4b13f5ff88?v=90b6d34cd9ae4051a2c8781f1776e020&pvs=4

        return pageGraphComplete;
    }

    public static NotionClient GetNotionClient()
    {
        // For production use, you should store this in a secure configuration
        // and not hardcode it in the source code
        string authToken = Environment.GetEnvironmentVariable("NOTION_API_KEY");
        
        if (string.IsNullOrEmpty(authToken))
        {
            // Fallback to hardcoded token if environment variable not set
            authToken = "";
            
            // Print warning about environment variable
            AnsiConsole.MarkupLine("[yellow]Warning:[/] Using hardcoded API token. For security, set the NOTION_API_KEY environment variable instead.");
        }
        
        return NotionClientFactory.Create(new ClientOptions
        {
            AuthToken = authToken
        });
    }

    public static async Task<PaginatedList<User>> GetNotionUsers(NotionClient client)
    {
        return await client.Users.ListAsync();
    }
}