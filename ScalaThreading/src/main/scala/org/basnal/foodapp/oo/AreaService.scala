package org.basnal.foodapp.oo

import cats.effect.IO
import cats.implicits._

class AreaService(
  areaRepo: Repository[AreaId, Area],
  restaurantRepo: Repository[RestaurantId, Restaurant],
  itemRepo: Repository[ItemId, Item]
) {
  
  def markAreaUnavailable(areaId: AreaId): IO[Unit] = {
    for {
      // Get and update area
      areaOpt <- areaRepo.get(areaId)
      area <- IO.fromOption(areaOpt)(new RuntimeException(s"Area $areaId not found"))
      closedArea = area.markClosed()
      _ <- areaRepo.save(areaId, closedArea)
      
      // Get and update all restaurants in the area
      restaurants <- area.restaurantIds.traverse { restaurantId =>
        restaurantRepo.get(restaurantId).flatMap {
          case Some(restaurant) => 
            val closedRestaurant = restaurant.markClosed()
            restaurantRepo.save(restaurantId, closedRestaurant).as(closedRestaurant)
          case None => 
            IO.raiseError(new RuntimeException(s"Restaurant $restaurantId not found"))
        }
      }
      
      // Get and update all items in those restaurants
      _ <- restaurants.flatMap(_.itemIds).traverse { itemId =>
        itemRepo.get(itemId).flatMap {
          case Some(item) => 
            val outOfStockItem = item.copy(inStock = false)
            itemRepo.save(itemId, outOfStockItem)
          case None => 
            IO.raiseError(new RuntimeException(s"Item $itemId not found"))
        }
      }
    } yield ()
  }

  def displayAreaStatus(areaId: AreaId): IO[String] = {
    for {
      // Get area
      areaOpt <- areaRepo.get(areaId)
      area <- IO.fromOption(areaOpt)(new RuntimeException(s"Area $areaId not found"))
      
      // Get all restaurants in the area
      restaurantStatuses <- area.restaurantIds.traverse { restaurantId =>
        restaurantRepo.get(restaurantId).flatMap {
          case Some(restaurant) =>
            // Get all items in the restaurant
            restaurant.itemIds.traverse { itemId =>
              itemRepo.get(itemId).map {
                case Some(item) =>
                  s"    - Item ${item.id.value}: ${if (item.inStock) "In Stock" else "Out of Stock"}"
                case None =>
                  s"    - Item $itemId: Not Found"
              }
            }.map { itemStatuses =>
              s"""  Restaurant ${restaurant.id.value}: ${if (restaurant.isOpen) "Open" else "Closed"}
                 |${itemStatuses.mkString("\n")}""".stripMargin
            }
          case None =>
            IO.pure(s"  Restaurant $restaurantId: Not Found")
        }
      }
      
      status = s"""Area ${area.id.value}: ${if (area.isOpen) "Open" else "Closed"}
                  |${restaurantStatuses.mkString("\n")}""".stripMargin
    } yield status
  }
} 