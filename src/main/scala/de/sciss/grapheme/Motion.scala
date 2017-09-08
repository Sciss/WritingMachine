/*
 *  Motion.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme

import impl.{MotionImpl => Impl}

object Motion {
  def constant(value: Double): Motion = Impl.constant(value)

  def linrand(lo: Double, hi: Double): Motion = Impl.linrand(lo, hi)

  def exprand(lo: Double, hi: Double): Motion = Impl.exprand(lo, hi)

  def sine(lo: Double, hi: Double, period: Int): Motion = Impl.sine(lo, hi, period)

  def walk(lo: Double, hi: Double, maxStep: Double): Motion = Impl.walk(lo, hi, maxStep)

  def linlin(in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double): Motion =
    Impl.linlin(in, inLo, inHi, outLo, outHi)

  def linexp(in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double): Motion =
    Impl.linexp(in, inLo, inHi, outLo, outHi)

  def coin(prob: Double, a: Motion, b: Motion): Motion = Impl.coin(prob, a, b)
}

trait Motion {
  def step(implicit tx: Tx): Double
}