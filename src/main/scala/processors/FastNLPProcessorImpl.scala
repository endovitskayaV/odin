package processors

import org.clulab.processors.{ Document }
import org.clulab.processors.fastnlp.FastNLPProcessor
import org.clulab.processors.clu.tokenizer.TokenizerStep
import org.clulab.processors.shallownlp.ShallowNLPProcessor

class FastNLPProcessorImpl(
    tokenizerPostProcessor: Option[TokenizerStep],
    internStrings: Boolean,
    withChunks: Boolean,
    withRelationExtraction: Boolean,
    withDiscourse: Int) extends FastNLPProcessor(tokenizerPostProcessor, internStrings, withChunks,      withRelationExtraction,      withDiscourse) {

  def this(
      internStrings: Boolean = true,
      withChunks: Boolean = true,
      withRelationExtraction: Boolean = false,
      withDiscourse: Int = ShallowNLPProcessor.NO_DISCOURSE) {
    this(None, internStrings, withChunks, withRelationExtraction, withDiscourse)
  }

  override def annotate(doc: Document): Document = {
    tagPartsOfSpeech(doc)
    lemmatize(doc)
    //    recognizeNamedEntities(doc)
    //    parse(doc)
    //    chunking(doc)
    //    relationExtraction(doc)
    //    srl(doc)
    //    resolveCoreference(doc)
    //    discourse(doc)
    doc.clear()
    doc
  }
}
