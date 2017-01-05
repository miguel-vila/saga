package saga

/**
  * Un valor de tipo `Saga[A]` representa la ejecución secuencial
  * de multiples transacciones que resultan en computar un valor
  * de tipo A
  *
  * El corazón de esto es el método `execute` que o bien puede fallar
  * o puede ser exitoso retornando un valor de tipo A y los detalles
  * de las ejecuciones exitosas. Esto es clave para poder implementar
  * `flatMap` de modo que se puedan deshacer las transacciones pasadas
  * cuando se da una falla
  */
sealed trait Saga[A] { self =>

  def execute(previousExecutions: SuccessfulExecutions): Either[FailedExecution[_], (A, SuccessfulExecutions)]

  def map[B](f: A => B): Saga[B] = new Saga[B] {
    def execute(previousExecutions: SuccessfulExecutions) =
      self.execute(previousExecutions).right.map { case (a,execs) => (f(a), execs) }
  }

  def flatMap[B](f: A => Saga[B]): Saga[B] = new Saga[B] {
    def execute(previousExecutions: SuccessfulExecutions) = {
      self.execute(previousExecutions).right.flatMap { case (b, previousExecutions2) =>
        f(b).execute(previousExecutions2 ++ previousExecutions)
      }
    }
  }

  def run(): Either[FailedExecution[_], A] =
    execute(List.empty).right.map(_._1)

}

case class GroupSaga[G,A](transactionGroup: TransactionGroup[G], callback: G => A) extends Saga[A] {

  override def execute(previousExecutions: SuccessfulExecutions): Either[FailedExecution[_], (A, SuccessfulExecutions)] =
    transactionGroup.execute(previousExecutions).right.map { case (g,execs) => (callback(g), execs) }

  override def map[B](f: A => B): Saga[B] =
    new GroupSaga(transactionGroup, { g: G => f(callback(g))} )

  def ++[H,B](other: GroupSaga[H,B]): GroupSaga[H,B] =
    GroupSaga(transactionGroup ++ other.transactionGroup, other.callback)

}

object Saga {

  def unit[A](a: A): Saga[A] = new Saga[A] {
    def execute(previousExecutions: SuccessfulExecutions) =
      Right((a,previousExecutions))
  }

  def apply[A](tx: Transaction[A]): GroupSaga[A,A] =
    GroupSaga(TransactionGroup.just(tx), identity)

}
