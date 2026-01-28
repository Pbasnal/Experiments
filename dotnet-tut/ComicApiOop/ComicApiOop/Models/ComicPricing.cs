// namespace ComicApiOop.Models;
//
// public class ComicPricing
// {
//     public long Id { get; set; }
//     public long ComicId { get; set; }
//     public string RegionCode { get; set; } = string.Empty;
//     public decimal BasePrice { get; set; }
//     public bool IsFreeContent { get; set; }
//     public bool IsPremiumContent { get; set; }
//     public DateTime? DiscountStartDate { get; set; }
//     public DateTime? DiscountEndDate { get; set; }
//     public decimal? DiscountPercentage { get; set; }
//     
//     // Navigation property
//     public ComicBook? Comic { get; set; }
//     
//     public decimal GetCurrentPrice()
//     {
//         if (IsFreeContent) return 0;
//         
//         var now = DateTime.UtcNow;
//         if (DiscountStartDate.HasValue && DiscountEndDate.HasValue && 
//             now >= DiscountStartDate && now <= DiscountEndDate && 
//             DiscountPercentage.HasValue)
//         {
//             return BasePrice * (1 - (DiscountPercentage.Value / 100));
//         }
//         
//         return BasePrice;
//     }
// }
