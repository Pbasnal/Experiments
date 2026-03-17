using ComicApiOop.Endpoints;
using ComicApiOop.Extensions;
using ComicApiOop.Services;

var builder = WebApplication.CreateBuilder(args);

// Configure services
builder.Services.AddComicApiServices(builder.Configuration);

// Register services
builder.Services.AddScoped<VisibilityComputationService>();

var app = builder.Build();

// Configure pipeline
app.UseComicApiPipeline(app.Environment);

// Map endpoints
app.MapComicEndpoints();
app.MapHealthEndpoints();

app.Run();
