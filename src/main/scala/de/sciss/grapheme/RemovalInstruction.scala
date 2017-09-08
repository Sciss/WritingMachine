/*
 *  RemovalInstruction.scala
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

final case class RemovalInstruction(span: Span, fade: Long) {
  def merge(b: RemovalInstruction): Option[RemovalInstruction] = {
    if (span.overlaps(b.span)) {
      val spanNew = span.union(b.span)
      Some(RemovalInstruction(spanNew, if (spanNew.start == span.start) fade else b.fade))
    } else None
  }
}