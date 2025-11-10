namespace ComicApiDod.Models;

public enum ContentFlag
{
    None = 0,
    Violence = 1,
    Gore = 2,
    Nudity = 4,
    ProfaneLanguage = 8,
    DrugUse = 16,
    ChildrenFriendly = 32,
    Freemium = 64,
    Premium = 128,
    Free = 256
}

public enum AgeRating
{
    AllAges,
    Teen,
    Teen15Plus,
    Mature,
    Adult
}

public enum LicenseType
{
    Full,
    PreviewOnly,
    NoAccess
}


