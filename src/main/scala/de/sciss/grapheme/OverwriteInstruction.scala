/*
 *  OverwriteInstruction.scala
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

import de.sciss.span.Span

final case class OverwriteInstruction(span: Span, newLength: Long) {

  import GraphemeUtil._

  def printFormat: String = {
    val s = formatSpan(span)
    val p = formatPercent(newLength.toDouble / span.length)
    s"over($s -> $p)"
  }
}