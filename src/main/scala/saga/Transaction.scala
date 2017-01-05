package saga

trait Transaction[A] {

  def apply(): Either[Exception, A]

  def unapply(a: Option[A]): Unit

  def executeOrUndo(): ExecutionResult[A] =
    apply() match {
      case Right(value) =>
        SuccessfulExecution(this, value)
      case Left(error) =>
        unapply(None)
        FailedExecution(this, error)
    }

}

trait ExecutionResult[A] {
  def transaction: Transaction[A]
}

case class SuccessfulExecution[A](transaction: Transaction[A], value: A) extends ExecutionResult[A] {

  def undo() = transaction.unapply(Some(value))

}

case class FailedExecution[A](transaction: Transaction[A], error: Exception) extends ExecutionResult[A]
