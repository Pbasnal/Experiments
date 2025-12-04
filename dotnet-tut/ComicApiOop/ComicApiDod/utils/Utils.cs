namespace ComicApiDod.utils;

public static class Utils
{
    public static void PrintCollection<T>(ICollection<T> collection)
    {
        if (collection == null || collection.Count == 0) return;
        
        Console.Write("[");
        foreach (var col in collection)
        {
            Console.Write($"{col}, ");
        }
        
        Console.WriteLine("\b\b]");
    }
}