package saga

/**
  * Un grupo de transacciones de tipo A es
  * un listado de transacciones en el que
  * la última transacción arroja algo de tipo A
  */
class TransactionGroup[A](val init: List[Transaction[_]], val last: Transaction[A]) {

  def execute(previousExecutions: SuccessfulExecutions): Either[FailedExecution[_], (A, SuccessfulExecutions)] = {
    def loop(executed: SuccessfulExecutions, txs: List[Transaction[_]]): Either[FailedExecution[_], SuccessfulExecutions] =
      txs match {
        case Nil        =>
          Right( executed )
        case tx :: rest =>
          tx.executeOrUndo() match {
            case execution : SuccessfulExecution[_] =>
              loop(execution :: executed, rest)
            case failure : FailedExecution[_] =>
              executed.foreach(_.undo())
              Left(failure)
          }
      }
    for {
      executed  <- loop(previousExecutions, init).right
      execution <- (last.executeOrUndo() match {
                       case success: SuccessfulExecution[A] =>
                         Right(success)
                       case failure: FailedExecution[A]     =>
                         executed.foreach(_.undo())
                         Left(failure)
                     }).right
    } yield (execution.value, execution :: executions)
  }

  def ++[B](other: TransactionGroup[B]): TransactionGroup[B] =
    new TransactionGroup(init ++ (last :: other.init), other.last)

}

object TransactionGroup {

  def just[A](transaction: Transaction[A]): TransactionGroup[A] =
    new TransactionGroup(List.empty, transaction)

}
