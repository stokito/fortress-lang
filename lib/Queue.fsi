api Queue

  trait Queue[\T\]
    abstract isEmpty(self): Boolean 
    abstract snoc(self, x: T): Queue[\T\]
    abstract head(self): T
    abstract tail(self): Queue[\T\]
    print(self, T -> ()): ()
  end

  emptyNaiveQueue[\T\](): Queue[\T\]

end