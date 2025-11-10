# Food Delivery Application Documentation

## Overview
This application demonstrates a data-oriented approach to handling state mutations in a food delivery system using columnar data structures and pure functional programming concepts in Scala. The system manages items, restaurants, and delivery areas using an index-based relationship model for efficient data manipulation.

## Architecture

### Core Components

#### 1. Domain Models
- `ItemId`: Unique identifier for food items
- `RestaurantId`: Unique identifier for restaurants
- `AreaId`: Unique identifier for delivery areas

#### 2. Data Structure
The application uses a columnar data structure approach with three main tables:

##### ItemTable
- Stores item information
- Uses indices to reference restaurants and areas
- Tracks item availability status
- Key columns: itemId, restaurantIdx, areaIdx, inStock

##### RestaurantTable
- Maintains restaurant information
- Links to areas via areaIdx
- Key columns: restaurantId, areaIdx

##### AreaTable
- Manages delivery area information
- Key columns: areaId

#### 3. State Management
- `StateMutation`: Provides pure state threading capabilities
- `Pipeline`: Enables composable effects and transformations

### Key Features

1. **Index-based Relationships**
   - Tables are linked using indices instead of direct ID references
   - Improves performance by reducing lookups
   - Maintains referential integrity through index relationships

2. **Pure Functional Approach**
   - Immutable data structures
   - Pure functions for state transformations
   - Composable operations using for-comprehensions

3. **Type-safe Operations**
   - Leverages Scala's type system
   - Ensures compile-time safety for data operations

## Workflows

### 1. Item Out-of-Stock (OOS) Workflow
**Purpose**: Mark individual items as unavailable
**Implementation**:
- Updates item's inStock status to false
- Preserves all other relationships
- Affects only the targeted item

### 2. Restaurant Closure Workflow
**Purpose**: Mark all items from a specific restaurant as unavailable
**Implementation**:
- Identifies all items belonging to the restaurant using restaurantIdx
- Updates inStock status for all affected items
- Maintains area relationships

### 3. Area Unavailable Workflow
**Purpose**: Mark all items in a specific area as unavailable
**Implementation**:
- Identifies all items in the area using areaIdx
- Updates inStock status for all affected items
- Affects items across multiple restaurants in the area

## Example Usage

```scala
// Initial state shows all items in stock
item 1a43 | rest 3278 | area d609 | inStock = true
item 8fd5 | rest 3278 | area d609 | inStock = true
item 534d | rest 9139 | area a3ab | inStock = true
item e15c | rest 9139 | area a3ab | inStock = true

// After marking item 8fd5 out-of-stock
item 1a43 | rest 3278 | area d609 | inStock = true
item 8fd5 | rest 3278 | area d609 | inStock = false  // Changed
item 534d | rest 9139 | area a3ab | inStock = true
item e15c | rest 9139 | area a3ab | inStock = true

// After closing restaurant 9139
item 1a43 | rest 3278 | area d609 | inStock = true
item 8fd5 | rest 3278 | area d609 | inStock = false
item 534d | rest 9139 | area a3ab | inStock = false  // Changed
item e15c | rest 9139 | area a3ab | inStock = false  // Changed

// After closing area d609
item 1a43 | rest 3278 | area d609 | inStock = false  // Changed
item 8fd5 | rest 3278 | area d609 | inStock = false
item 534d | rest 9139 | area a3ab | inStock = false
item e15c | rest 9139 | area a3ab | inStock = false
```

## Project Structure

```
src/main/scala/org/basnal/foodapp/
├── Domain.scala           # Core domain models and types
├── StateMutation.scala    # Pure state threading implementation
├── Pipeline.scala         # Composable effects framework
├── Database.scala         # In-memory database implementation
├── Tables/
│   ├── ItemTable.scala
│   ├── RestaurantTable.scala
│   └── AreaTable.scala
└── Workflows/
    ├── ItemOOSWorkflow.scala
    ├── RestaurantCloseWorkflow.scala
    └── AreaUnavailableWorkflow.scala
```

## Dependencies
- Scala 2.13.14
- cats-core 2.12.0: For functional programming abstractions
- cats-effect 3.5.1: For effectful computations

## Running the Application
Execute the following command to run the demo:
```bash
sbt "runMain org.basnal.foodapp.FoodAppDemo"
```

## Future Improvements
1. Add persistence layer for data storage
2. Implement transaction support for atomic operations
3. Add validation rules for business logic
4. Implement event sourcing for state changes
5. Add metrics and monitoring
6. Implement caching for frequently accessed data 