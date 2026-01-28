using Common.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace ComicApiDod.Data;

public class DesignTimeDbContextFactory : IDesignTimeDbContextFactory<ComicDbContext>
{
    public ComicDbContext CreateDbContext(string[] args)
    {
        var optionsBuilder = new DbContextOptionsBuilder<ComicDbContext>();
        var connectionString = "Server=localhost;Port=3306;Database=comicdb;User=comicuser;Password=comicpass;";

        optionsBuilder.UseMySql(connectionString, 
            new MySqlServerVersion(new Version(8, 0, 0)),
            mysqlOptions =>
            {
                mysqlOptions.EnableRetryOnFailure(
                    maxRetryCount: 5,
                    maxRetryDelay: TimeSpan.FromSeconds(30),
                    errorNumbersToAdd: null);
            });

        return new ComicDbContext(optionsBuilder.Options);
    }
}


