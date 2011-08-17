(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Random

import RangeInternals.{...}

(** Any random number generator that can be used for generating any desired
    random generators. It doesn't expose the raw random number generator
    interface. **)
trait AnyRandomGen
    (** See `RandomGen.distributed`. **)
    abstract distributed[\U\](dist:RandomDistribution[\U\]): RandomGen[\U\]
end

(** Abstract interface for (possibly pseudo-) random number generators.

    Minimal complete definition of a `RandomGen` is `min`, `max` and `random`
    methods. It is recommended, but not required, for generators to override
    `randomMany`, `randomR` and `randomManyR` methods with efficient versions
    if possible. **)
trait RandomGen[\T extends Number\] extends AnyRandomGen excludes Indexed[\T,ZZ32\]
    (** The lower bound (infimum) of the output of the generator, or `Nothing`
        if it is unbounded. **)
    getter left(): Maybe[\T\]
    (** The minimum value of the output of the generator, or `Nothing`
        if the lower bound doesn't exist or it exists but it is excluded
        from the range. **)
    abstract getter min(): Maybe[\T\]
    (** The maximum value of the output of the generator, or `Nothing`
        if the upper bound doesn't exist or it exists but it is excluded
        from the range. **)
    abstract getter max(): Maybe[\T\]
    (** The upper bound (supremum) of the output of the generator, or `Nothing`
        if it is unbounded. **)
    getter right(): Maybe[\T\]

    (** Generates a random number. **)
    abstract random(): T
    (** Generates `n` random numbers. The generator may override `randomMany`
        method if it has certain means to generate multiple random numbers
        efficiently. **)
    randomMany(n:ZZ32): ReadableArray[\T,ZZ32\]

    (** Generates a uniform random number from the given `range`. It makes
        use of `UniformDistribution` distribution defined below. **)
    randomR(range:FullScalarRange[\ZZ32\]): ZZ32
    (** Same as `randomR` but generates multiple random numbers. **)
    randomManyR(n:ZZ32, range:FullScalarRange[\ZZ32\]): ReadableArray[\ZZ32,ZZ32\]

    (** A proxy `RandomGen` that is adapted to the given `RandomDistribution`.
        The original generator can be used later, but both the original
        generator and new generator share the same state. **)
    distributed[\U\](dist:RandomDistribution[\U\]): RandomGen[\U\]
end

(** Similar to `AnyRandomGen`, but also includes `perturbed` method in
    `SeededRandomGen`. **)
trait AnySeededRandomGen extends AnyRandomGen
    (** See `SeededRandomGen.perturbed`. **)
    abstract perturbed(perturbvec:Generator[\ZZ32\]): AnySeededRandomGen
    abstract perturbed(perturbvec:ZZ32...): AnySeededRandomGen
end

(** `RandomGen` with a capability to initialize states deterministically
    ("reseeding"). By convention `SeededRandomGen` objects receives the
    initial seed as the first argument to the constructor.

    The notable example of non-reseedable `RandomGen` would be an external
    random source. **)
trait SeededRandomGen[\T,Seed\] extends { RandomGen[\T\], AnySeededRandomGen }
    (** The minimal size of `seedvec` argument to `reseed` method. It is
        unrelated to the size of internal states. **)
    abstract getter seedSize(): ZZ32

    (** Reseed the generator with given seed vector.

        The implementation of `reseed` should:
        * Accept all the possible `seedvec` of size equal to or greater than
          `self.seedSize`;
        * Return itself (not a copy of itself), and
        * Behave deterministically, i.e. for any valid `seedvec`, `reseed`
          call followed by a series of `random`-like calls should result in
          the same result. **)
    abstract reseed(seedvec:Indexed[\Seed,ZZ32\]): SeededRandomGen[\T,Seed\]
    (** Reseed the generator with given seed value. It is only applicable
        when `self.seedSize` is 1, and equivalent to `reseed` method with
        a vector of size 1. **)
    reseed(seedval:Seed): SeededRandomGen[\T,Seed\]

    (** Perturb the generator with given perturbation vector. `perturbed` is
        a generalization of `split` method found in Haskell: one can always
        write `split` method by calling `perturbed` method twice with arbitrary
        different numbers (say, 1 and 2).

        The implementation of `perturbed` should:
        * Accept all the possible `perturbvec` of size 1 or more;
        * Take and perturb the internal state of the generator with respect to
          `perturbvec`;
        * Return a **copy** of the generator, i.e. `perturbed` call should not
          affect the other `random`-like calls, and
        * Behave deterministically, i.e. for any valid `seedvec` and
          `perturbvec`, `reseed` call followed by `perturbed` call and a series
          of `random`-like calls to the new generator should result in the same
          result.

        The implementation may choose to return the copy of itself
        if `perturbvec` is a vector with one element equal to 0, but it doesn't
        have to do so. `perturbed` calls are also not composable; for example,
        `self.perturbed(1).perturbed(2)` may differ from `self.perturbed(1, 2)`.
        **)
    abstract perturbed(perturbvec:Generator[\ZZ32\]): SeededRandomGen[\T,Seed\]
    (** Perturb the generator with given perturbation values. It is a shortcut
        to the generic `perturbed` method. **)
    perturbed(perturbvals:ZZ32...): SeededRandomGen[\T,Seed\]

    (** For `SeededRandomGen`, a proxy generator from `distributed` behaves
        as like the original generator for `reseed` and `perturbed` calls. **)
    distributed[\U\](dist:RandomDistribution[\U\]): SeededRandomGen[\U,Seed\]
end

(** A random distribution, which takes a "raw" generator to generate random
    numbers with the desired population. **)
trait RandomDistribution[\U\]
    (** The lower bound (infimum) of the output of the resulting generator. **)
    left[\T extends Integral[\T\]\](gen:RandomGen[\T\]): Maybe[\U\]
    (** The minimum value of the output of the resulting generator. **)
    abstract min[\T extends Integral[\T\]\](gen:RandomGen[\T\]): Maybe[\U\]
    (** The maximum value of the output of the resulting generator. **)
    abstract max[\T extends Integral[\T\]\](gen:RandomGen[\T\]): Maybe[\U\]
    (** The upper bound (supremum) of the output of the resulting generator. **)
    right[\T extends Integral[\T\]\](gen:RandomGen[\T\]): Maybe[\U\]

    (** Take a `RandomGen` and generates a random number. It may call
        `random`-like methods zero or multiple times. **)
    abstract generate[\T extends Integral[\T\]\](gen:RandomGen[\T\]): U
end

(** A proxy generator. **)
object RandomGenWithDistribution[\T,U\](gen:RandomGen[\T\], dist:RandomDistribution[\U\])
    extends RandomGen[\U\]

    getter left(): Maybe[\T\]
    getter min(): Maybe[\T\]
    getter max(): Maybe[\T\]
    getter right(): Maybe[\T\]

    random(): T
    distributed[\U2\](newdist:RandomDistribution[\U2\]): RandomGen[\U2\]
end

(** A proxy generator with the "reseeding" capability. **)
object SeededRandomGenWithDistribution[\T,U,Seed\](gen:SeededRandomGen[\T,Seed\],
        dist:RandomDistribution[\U\])
    extends SeededRandomGen[\U,Seed\]

    getter left(): Maybe[\T\]
    getter min(): Maybe[\T\]
    getter max(): Maybe[\T\]
    getter right(): Maybe[\T\]

    random(): T
    perturbed(perturbvec:Generator[\ZZ32\]): SeededRandomGen[\T,Seed\]
    distributed[\U2\](newdist:RandomDistribution[\U2\]): SeededRandomGen[\U2,Seed\]
end

(**********************************************************)

(** System-provided random number generator, based on `random` built-in
    function. Note that only 53 random bits are available (not 64 bits). **)
object SystemRandomGen extends RandomGen[\ZZ64\]
    getter min(): Just[\ZZ64\]
    getter max(): Just[\ZZ64\]

    random(): ZZ64
end

(** Linear congruential generator.

    It's not appropriate for applications where high-quality randomness is
    critical. Thus it is mainly used for legacy applications and an initial
    vector of other high-quality random number generator, such as
    `MersenneTwister`. **)
object LinearCongruential[\N\](seed:N, mult:N, add:N, modulus:N)
    extends SeededRandomGen[\N,N\]

    getter min(): Just[\N\]
    getter max(): Just[\N\]
    getter seedSize(): ZZ32

    state: N

    reseed(seedvec:Indexed[\N,ZZ32\]): LinearCongruential[\N\]
    random(): N
    perturbed(perturbvec:Generator[\ZZ32\]): LinearCongruential[\N\]
end

(** A particular random number generator which is used in Java.
    Period: $2^{48}$. **)
linearCongruential(seed:ZZ64): LinearCongruential[\ZZ64\]
(** Same to `linearCongruential` but uses a random seed. **)
linearCongruential(): LinearCongruential[\ZZ64\]

(** Mersenne twister, as proposed by Makoto Matsumoto and Takuji Nishimura in 1997.

    It is recommended for most applications, as it is reasonably fast and
    still produces high-quality random numbers. Mostly used with the parameters
    in `mersenneTwister` below. **)
object MersenneTwister[\N, nat wordsize, nat degree\]
        (seed:Vector[\N,degree\], coeff:N, middle:ZZ32, pivot:ZZ32,
         tampershift0:ZZ32, tampershift1:ZZ32, tampershift2:ZZ32, tampershift3:ZZ32,
         tampermask1:N, tampermask2:N)
    extends SeededRandomGen[\N,N\]

    getter min(): Just[\N\]
    getter max(): Just[\N\]
    getter seedSize(): ZZ32

    state: Vector[\N,degree\]
    current: ZZ32

    reseed(seedvec:Indexed[\N,ZZ32\]): MersenneTwister[\N,wordsize,degree\]
    random(): N
    perturbed(perturbvec:Generator[\ZZ32\]): MersenneTwister[\N,wordsize,degree\]
end

(** A modified linear congruential generator used for the initialization of
    MT19937 generator. **)
object MersenneTwisterInit(seed:ZZ64) extends SeededRandomGen[\ZZ64,ZZ64\]
    getter min(): Just[\ZZ64\]
    getter max(): Just[\ZZ64\]
    getter seedSize(): ZZ32

    state: ZZ64

    reseed(seedvec:Indexed[\ZZ64,ZZ32\]): MersenneTwisterInit
    random(): ZZ64
    perturbed(perturbvec:Indexed[\ZZ32,ZZ32\]): MersenneTwisterInit
end

(** 32-bit MT19937 generator. (Its seed vector is of `ZZ64` due to the overflow
    mechanism.) Period: $2^{19937} - 1$, equidistributed in ~623 dimensions.

    The size of the seed vector should be 624. **)
mersenneTwister(seed:Vector[\ZZ64,624\]): MersenneTwister[\ZZ64,32,624\]
(** Same to `mersenneTwister` but uses a single number plus modified LCG to
    seed the generator. This is equivalent to the reference implementation. **)
mersenneTwister(seed:ZZ64): MersenneTwister[\ZZ64,32,624\]
(** Same to `mersenneTwister` but uses a random seed. **)
mersenneTwister(): MersenneTwister[\ZZ64,32,624\]

(**********************************************************)

(** Uniform random distribution within the given `range`. It uses a classic
    retrial method. **)
object UniformDistribution[\T\](range:FullScalarRange[\T\])
    extends RandomDistribution[\T\]

    min[\N extends Integral[\N\]\](gen:RandomGen[\N\]): Just[\T\]
    max[\N extends Integral[\N\]\](gen:RandomGen[\N\]): Just[\T\]

    (* XXX the current implementation requires the range of N is greater than
       the range of T *)
    generate[\N extends Integral[\N\]\](gen:RandomGen[\N\]): T
end

end

