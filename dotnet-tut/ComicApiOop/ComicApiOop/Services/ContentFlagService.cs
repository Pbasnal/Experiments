using ComicApiOop.Models;

namespace ComicApiOop.Services;

public static class ContentFlagService
{
    /// <summary>
    /// Determines content flags based on business rules including chapter pricing and comic pricing
    /// </summary>
    public static ContentFlag DetermineContentFlags(ContentFlag baseFlags, List<Chapter> chapters, ComicPricing? pricing)
    {
        var flags = baseFlags;

        // Check if all chapters are free
        bool allChaptersFree = chapters.All(c => c.IsFree);
        bool hasAnyFreeChapter = chapters.Any(c => c.IsFree);
        bool hasPaidChapters = chapters.Any(c => !c.IsFree);

        // Add Free flag if all chapters are free
        if (allChaptersFree)
        {
            flags |= ContentFlag.Free;
        }

        // Add Premium flag if no free chapters
        if (!hasAnyFreeChapter)
        {
            flags |= ContentFlag.Premium;
        }

        // Add Freemium flag if the comic has both free and paid chapters or has a price
        if ((hasAnyFreeChapter && hasPaidChapters) || (pricing?.BasePrice > 0))
        {
            flags |= ContentFlag.Freemium;
        }

        return flags;
    }
}
