package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Token

/**
 * @author Chris Hokamp
 */

trait DocFrequencyStore {

  def getDocFreq (token: Token): Int

}
