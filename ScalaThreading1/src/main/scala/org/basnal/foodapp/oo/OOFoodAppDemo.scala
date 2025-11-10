package org.basnal.foodapp.oo

import cats.effect.{IO, IOApp}
import java.util.UUID

object OOFoodAppDemo extends IOApp.Simple {
  
  def createTestData(
    areaRepo: Repository[AreaId, Area],
    restaurantRepo: Repository[RestaurantId, Restaurant],
    itemRepo: Repository[ItemId, Item]
  ): IO[AreaId] = {
    // Create IDs
    val areaId = AreaId(UUID.randomUUID())
    val restaurant1Id = RestaurantId(UUID.randomUUID())
    val restaurant2Id = RestaurantId(UUID.randomUUID())
    val item1Id = ItemId(UUID.randomUUID())
    val item2Id = ItemId(UUID.randomUUID())
    val item3Id = ItemId(UUID.randomUUID())
    
    // Create area with empty lists
    val area = Area(areaId, isOpen = true)
    
    // Create restaurants with empty lists
    val restaurant1 = Restaurant(restaurant1Id, areaId, isOpen = true)
    val restaurant2 = Restaurant(restaurant2Id, areaId, isOpen = true)
    
    // Create items
    val item1 = Item(item1Id, restaurant1Id, inStock = true, 1000)
    val item2 = Item(item2Id, restaurant1Id, inStock = true, 1500)
    val item3 = Item(item3Id, restaurant2Id, inStock = true, 2000)
    
    // Add item IDs to restaurants
    val restaurant1WithItems = restaurant1.addItemId(item1Id).addItemId(item2Id)
    val restaurant2WithItems = restaurant2.addItemId(item3Id)
    
    // Add restaurant IDs to area
    val completeArea = area.addRestaurantId(restaurant1Id).addRestaurantId(restaurant2Id)
    
    // Save all entities
    for {
      _ <- areaRepo.save(areaId, completeArea)
      _ <- restaurantRepo.save(restaurant1Id, restaurant1WithItems)
      _ <- restaurantRepo.save(restaurant2Id, restaurant2WithItems)
      _ <- itemRepo.save(item1Id, item1)
      _ <- itemRepo.save(item2Id, item2)
      _ <- itemRepo.save(item3Id, item3)
    } yield areaId
  }
  
  override def run: IO[Unit] = {
    val areaRepo = Repository.inMemory[AreaId, Area]
    val restaurantRepo = Repository.inMemory[RestaurantId, Restaurant]
    val itemRepo = Repository.inMemory[ItemId, Item]
    val areaService = new AreaService(areaRepo, restaurantRepo, itemRepo)
    
    for {
      // Create test data
      areaId <- createTestData(areaRepo, restaurantRepo, itemRepo)
      
      // Display initial status
      _ <- IO.println("Initial Status:")
      initialStatus <- areaService.displayAreaStatus(areaId)
      _ <- IO.println(initialStatus)
      
      // Mark area as unavailable
      _ <- IO.println("\nMarking area as unavailable...")
      _ <- areaService.markAreaUnavailable(areaId)
      
      // Display final status
      _ <- IO.println("\nFinal Status:")
      finalStatus <- areaService.displayAreaStatus(areaId)
      _ <- IO.println(finalStatus)
    } yield ()
  }
} 