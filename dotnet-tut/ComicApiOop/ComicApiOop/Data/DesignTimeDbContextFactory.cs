using Common.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace ComicApiOop.Data;

public class DesignTimeDbContextFactory : IDesignTimeDbContextFactory<ComicDbContext>
{
    public ComicDbContext CreateDbContext(string[] args)
    {
        var optionsBuilder = new DbContextOptionsBuilder<ComicDbContext>();
        optionsBuilder.UseMySql(
            "Server=mysql;Database=ComicBookDb;User=comicuser;Password=comicpassword;",
            ServerVersion.AutoDetect("Server=mysql;Database=ComicBookDb;User=comicuser;Password=comicpassword;")
        );

        return new ComicDbContext(optionsBuilder.Options);
    }
}
