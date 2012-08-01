package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.InvertedIndexStore

/**
 * @author Chris Hokamp
 */
//TODO: remove abstract
abstract class MemoryInvertedIndex
  extends MemoryStore
  with InvertedIndexStore {

}
