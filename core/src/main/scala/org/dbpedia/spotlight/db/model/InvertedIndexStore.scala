package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{DBpediaResource, Token}

/**
 * @author Chris Hokamp
 */

trait InvertedIndexStore {

  def getResources (token: Token): Set[DBpediaResource]

}
