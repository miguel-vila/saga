package saga

object Example extends App {

  def tx1(n: Int) = new Transaction[String] {

    def apply() =
      if(n>0) {
        println("Applying tx1!")
        Right("something")
      } else {
        Left(new Exception(s"ouch tx1!"))
      }

    def unapply(idOpt: Option[String]) = {
      println(s"Unapplying tx1 $idOpt")
    }

  }

  def tx2(s: String) = new Transaction[String] {

    def apply() =
      if(s.length>3) {
        println("Applying tx2!")
        Right("something2")
      } else {
        Left(new Exception(s"ouch tx2!"))
      }

    def unapply(idOpt: Option[String]) = {
      println(s"Unapplying tx2 $idOpt")
    }

  }

  def tx3(s: String) = new Transaction[String] {

    def apply() =
      if(s == "WHATEV") {
        println("Applying tx3!")
        Right("something3")
      } else {
        Left(new Exception(s"ouch tx3!"))
      }

    def unapply(idOpt: Option[String]) = {
      println(s"Unapplying tx3 $idOpt")
    }

  }

  val saga1 = Saga(tx1(2)) ++ Saga(tx2("blabla"))

  println(s"saga1 result = ${saga1.run()}")

  val composedSaga = for {
    result1 <- saga1
    result2 <- Saga(tx3(result1))
  } yield result2

  println(s"composed saga result = ${composedSaga.run()}")

}
