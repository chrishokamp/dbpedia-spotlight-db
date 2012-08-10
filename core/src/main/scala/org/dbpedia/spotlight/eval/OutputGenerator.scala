package org.dbpedia.spotlight.eval

import org.dbpedia.spotlight.eval.DisambiguationResult
import org.dbpedia.spotlight.model.{HasFeatures}

/**
 *
 * @author pablomendes
 */

trait OutputGenerator {

  def write(result: DisambiguationResult)

  def close()

  def flush()

  def summary(summaryString: String)

}
