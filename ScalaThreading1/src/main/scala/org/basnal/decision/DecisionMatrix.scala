package org.basnal.decision

// Base trait for all possible outcomes
sealed trait DecisionOutcome
case object Success extends DecisionOutcome
case object Failure extends DecisionOutcome
case object PartialSuccess extends DecisionOutcome
case object Skipped extends DecisionOutcome

// Base trait for all decision data
trait DecisionData {
  def message: String
  def isEmpty: Boolean
}

// Empty data for terminated branches
case object EmptyDecisionData extends DecisionData {
  override def message: String = "No data available - branch terminated"
  override def isEmpty: Boolean = true
}

// Example decision data types
case class ValidationData(
  isValid: Boolean,
  validationMessage: String,
  details: Map[String, String] = Map.empty
) extends DecisionData {
  override def message: String = validationMessage
  override def isEmpty: Boolean = false
}

case class ProcessingData(
  processedItems: Seq[String],
  errorCount: Int,
  processingMessage: String
) extends DecisionData {
  override def message: String = processingMessage
  override def isEmpty: Boolean = processedItems.isEmpty
}

// Decision Matrix that accumulates results
class DecisionMatrix[T <: DecisionData] {
  private var matrix = Map.empty[DecisionOutcome, T]
  
  def record(outcome: DecisionOutcome, data: T): Unit = {
    matrix = matrix + (outcome -> data)
  }
  
  def recordEmpty(outcome: DecisionOutcome): Unit = {
    matrix = matrix + (outcome -> EmptyDecisionData.asInstanceOf[T])
  }
  
  def get(outcome: DecisionOutcome): Option[T] = matrix.get(outcome)
  
  def hasOutcome(outcome: DecisionOutcome): Boolean = matrix.contains(outcome)
  
  def getAllMessages: Map[DecisionOutcome, String] = 
    matrix.view.mapValues(_.message).toMap
  
  def clear(): Unit = matrix = Map.empty
}

// Example usage
object DecisionMatrixExample extends App {
  // Example validation workflow
  def validateOrder(items: Seq[String]): DecisionMatrix[DecisionData] = {
    val matrix = new DecisionMatrix[DecisionData]
    
    // Check if order is empty
    if (items.isEmpty) {
      matrix.record(
        Failure,
        ValidationData(
          isValid = false,
          validationMessage = "Order cannot be empty"
        )
      )
      return matrix
    }
    
    // Check for invalid items
    val invalidItems = items.filter(_.trim.isEmpty)
    if (invalidItems.nonEmpty) {
      matrix.record(
        PartialSuccess,
        ValidationData(
          isValid = false,
          validationMessage = "Some items are invalid",
          details = Map("invalidCount" -> invalidItems.size.toString)
        )
      )
    }
    
    // Process valid items
    val validItems = items.filter(_.trim.nonEmpty)
    matrix.record(
      Success,
      ProcessingData(
        processedItems = validItems,
        errorCount = invalidItems.size,
        processingMessage = s"Processed ${validItems.size} items successfully"
      )
    )
    
    matrix
  }
  
  // Test the decision matrix
  println("Test 1: Empty Order")
  val emptyOrderResult = validateOrder(Seq.empty)
  println(s"Failure outcome: ${emptyOrderResult.get(Failure).map(_.message).getOrElse("N/A")}")
  
  println("\nTest 2: Mixed Valid/Invalid Order")
  val mixedOrder = Seq("item1", "", "item2", "  ", "item3")
  val mixedOrderResult = validateOrder(mixedOrder)
  println("All outcomes:")
  mixedOrderResult.getAllMessages.foreach { case (outcome, message) =>
    println(s"$outcome: $message")
  }
}

// More complex example with Option handling
object AdvancedExample {
  // Example data classes
  case class User(id: String, name: String)
  case class Order(id: String, items: Seq[String])
  
  // More specific decision data
  case class UserValidationData(
    user: Option[User],
    message: String,
    details: Map[String, String] = Map.empty
  ) extends DecisionData {
    override def isEmpty: Boolean = user.isEmpty
  }
  
  def processUserOrder(
    maybeUser: Option[User],
    maybeOrder: Option[Order]
  ): DecisionMatrix[DecisionData] = {
    val matrix = new DecisionMatrix[DecisionData]
    
    // Handle missing user
    if (maybeUser.isEmpty) {
      matrix.record(
        Failure,
        UserValidationData(
          user = None,
          message = "User not found"
        )
      )
      return matrix
    }
    
    // Handle missing order
    if (maybeOrder.isEmpty) {
      matrix.record(
        Skipped,
        ValidationData(
          isValid = false,
          validationMessage = "No order found for processing"
        )
      )
      return matrix
    }
    
    val user = maybeUser.get
    val order = maybeOrder.get
    
    // Process order items
    val (validItems, invalidItems) = order.items.partition(_.trim.nonEmpty)
    
    // Record validation result
    if (invalidItems.nonEmpty) {
      matrix.record(
        PartialSuccess,
        ValidationData(
          isValid = false,
          validationMessage = s"Order ${order.id} has invalid items",
          details = Map(
            "userId" -> user.id,
            "invalidCount" -> invalidItems.size.toString
          )
        )
      )
    }
    
    // Record processing result
    matrix.record(
      Success,
      ProcessingData(
        processedItems = validItems,
        errorCount = invalidItems.size,
        processingMessage = s"Processed order ${order.id} for user ${user.name}"
      )
    )
    
    matrix
  }
}
