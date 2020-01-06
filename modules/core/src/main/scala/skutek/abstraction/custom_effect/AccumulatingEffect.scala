package skutek.abstraction.custom_effect
import skutek.utils.Accumulator


trait AccumulatingEffect[E] {
  type HandlerCtor[S]
  def handlerCtor[S](acc: Accumulator[E, S]): HandlerCtor[S]

  def handler = intoSeq
  def intoSeq = handlerCtor(Accumulator.intoSeq[E])
  def intoSet = handlerCtor(Accumulator.intoSet[E])
  def fold(z: E)(op: (E, E) => E) = handlerCtor(Accumulator.fold(z)(op))
  def reduceOption(op: (E, E) => E) = handlerCtor(Accumulator.reduceOption(op))
  def firstOption = handlerCtor(Accumulator.firstOption)
  def lastOption = handlerCtor(Accumulator.lastOption)
}


trait AccumulatingEffect_exports {
  implicit class AccumulatingEffectOfPairs_exports[K, V](val thiz: AccumulatingEffect[(K, V)]) {
    def intoMap = thiz.handlerCtor(Accumulator.intoMap[K, V])
  }
}
