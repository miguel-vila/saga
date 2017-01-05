# saga

Una prueba de concepto para una monada que represente una computación compuesta de multiples transacciones que al fallar debe deshacer todas las exitosas. ¿Es eso lo que llaman una _saga_?

## API básico

La idea es que primero uno describa cada acción como algo de tipo `Transaction`, que es algo que sabe como aplicar una transacción pero también como deshacerla:

```scala
trait Transaction[A] {

  def apply(): Either[Exception, A]

  def unapply(a: Option[A]): Unit`
  
}
```

Y después se puede construir algo de tipo `Saga` concatenando transacciones individuales:

```scala
val saga = Saga(tx1) ++ Saga(tx2) ++ Saga(tx3)
```

Después de haber hecho la composición se puede ejecutar:

```scala
saga.run()
// Right(myValue)
// Left(FailedExecution(failingTx, throwedError))
```
En cuyo caso retornara o bien un valor exitoso con el valor de retorno de la última transacción o retornará un valor fallido con el detalle de qué transacción falló y con qué error.

## `flatMap`

Si hay una transacción, cuyo valor de retorno determina como va ser la siguiente transacción uno puede usar `flatMap`:

```scala
for {
  cartId <- Saga(createShoppingCart(userId))
  _      <- Saga(addProduct(productId, cartId))
} yield ()
```

La semántica de `run` es la misma: si una transacción falla se ejecuta el código correctivo de las que alcanzaron a ejecutarse correctamente.

## @TODO

Detalles que se pueden afinar:

* En ciertos puntos hay algunos _side effects_, por ej. en la implementación de `execute` en `TransactionGroup`.
* Incluir soporte para cuando la transacción retorna un valor dentro de algún contexto, por ej. en un `Future`.
* Afinar la definición de `unapply` -> ¿si es lo ms general posible?
