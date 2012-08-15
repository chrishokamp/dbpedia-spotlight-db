package org.dbpedia.spotlight.db.memory.util

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.lang.Integer

/**
 * @author Joachim Daiber
 */

object StringToIDMap {

  def createTrove(expectedSize: Int): java.util.Map[String, Integer]    = new TroveStringToIDMap(expectedSize)
  def createFastUtil(expectedSize: Int): java.util.Map[String, Integer] = new Object2IntOpenHashMap[String](expectedSize)
  def createDefault(expectedSize: Int): java.util.Map[String, Integer]  = createTrove(expectedSize)

}
