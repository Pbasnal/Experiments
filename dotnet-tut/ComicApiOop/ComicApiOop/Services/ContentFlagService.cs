using Common.Models;

namespace ComicApiOop.Services;

public static class ContentFlagService
{
    public static ContentFlag DetermineContentFlags(ContentFlag baseFlags,
        bool allChaptersFree,
        bool hasAnyFreeChapter,
        ComicPricing? pricing)
    {
        var flags = baseFlags;

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
        if ((hasAnyFreeChapter && !allChaptersFree) || (pricing?.BasePrice > 0))
        {
            flags |= ContentFlag.Freemium;
        }

        return flags;
    }
}