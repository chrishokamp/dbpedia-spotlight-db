package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.DocFrequencyStore
import scala.Array
import org.dbpedia.spotlight.model.Token

/**
 * @author Chris Hokamp
 */

class MemoryDocFreqStore
  extends MemoryStore
  with DocFrequencyStore {

  var docFreq = Array[Int]()

  def size =  docFreq.size

  def getDocFreq (token: Token):  Int = {
    val i = token.id
    return docFreq(i);
  }


}
