package skutek
package _internals


object Interpreter {

  def pure[A, U](eff: A !! U): A = { 
    @annotation.tailrec
    def loop[B](eff: B !! U): B = eff match {
      case Return(a) => a 
      case FlatMapped(eff, k) => eff match {
        case Return(x) => loop(k(x))
        case FlatMapped(eff, j) => loop(eff.flatMap(x => j(x).flatMap(k)))
        case Product(eff1, eff2) => loop(eff1.flatMap(a => eff2.flatMap(b => k((a, b)))))
        case _ => sys.error(s"Unhandled effect: $eff")
      }
      case _ => loop(eff.map(a => a))
    }

    loop(eff)
  }


  def impure[A, U, V, H <: Driver](h: H, myTag: Any, eff: A !! U with V): h.Result[A] !! U = {
    type UV = U with V

    def loop[B](eff: B !! UV): h.Secret[B, U] = {
      def loopTramp[C](eff: C !! UV) = h.onConceal(Return(), (_: Unit) => loop(eff))

      eff match {
        case Return(a) => h.onReturn(a)
        case FlatMapped(eff: Effectful[type_X, UV], k) => {
          type X = type_X
          def kk      = (x: X) => loop(k(x))
          def kkTramp = (x: X) => loopTramp(k(x))
          def suspend = h.onConceal(eff.downCast[U], kk)

          eff match {
            case Return(x) => loop(k(x))
            case FlatMapped(eff, j) => loop(eff.flatMap(x => j(x).flatMap(k)))
            case Product(eff1, eff2) => h.onProduct(loopTramp(eff1), loopTramp(eff2), kk)
            case op: AnyOperation[X, UV] => {
              if (op.tag == myTag) 
                h.onOperation(op.stripTag.asInstanceOf[h.Op[X]], kkTramp)
              else 
                suspend
            }
            case FilterFail() => h.onFilterFail match {
              case Some(op) => h.onOperation(op, kkTramp)
              case None => suspend
            }
          }
        }
        case _ => loop(eff.map(a => a))
      }
    }

    h.onReveal(loop(eff))
  }
}
