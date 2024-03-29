package service

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import processors.{ api, ConverterUtils, ProcessorsBridge }
import org.clulab.processors
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.{ jackson, DefaultFormats, JNothing, JValue }
import org.clulab.processors.ProcessorsConstants._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContextExecutor
import com.typesafe.config.Config
import org.clulab.odin.ExtractorEngine

import scala.collection.mutable

trait Service {

  implicit val htmlMarshaller = ScalaXmlSupport.nodeSeqMarshaller(MediaTypes.`text/html`)
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  implicit val system: ActorSystem

  implicit def executionContext: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    system.scheduler.scheduleOnce(duration)(body)

  //  def apiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(apiConnectionFlow).runWith(Sink.head)

  def route(implicit materializer: Materializer) = {
    // log results
    logRequestResult("processors-server-microservice") {
      // index page
      path("") {
        getFromResource("static/index.html")
      } ~
        // Display version
        path("version") {
          get {
            val html =
              <html>
                <body>
                  <h1><code>processors-server</code> version { utils.projectVersion } ({ utils.commit })</h1>
                </body>
              </html>
            complete(html)
          }
        } ~
        // buildInfo
        path("buildinfo") {
          get {
            complete(api.jsonBuildInfo)
          }
        } ~
        // Demos
        path("nlp" / "demo") {
          getFromResource("static/annotation-demo.html")
        } ~
        path("odin" / "demo") {
          getFromResource("static/odin.html")
        } ~
        // resources
        path("favicon.ico") {
          val resourcePath = s"static/images/favicon.ico"
          getFromResource(resourcePath)
        } ~
        path("js" / ".*".r) { resource: String =>
          val resourcePath = s"static/js/$resource"
          logger.debug(resource)
          getFromResource(resourcePath)
        } ~
        path("css" / ".*".r) { resource: String =>
          val resourcePath = s"static/css/$resource"
          logger.debug(resource)
          getFromResource(resourcePath)
        } ~
        path("fonts" / ".*".r) { resource: String =>
          val resourcePath = s"static/fonts/$resource"
          logger.debug(resource)
          getFromResource(resourcePath)
        } ~
        path("images" / ".*".r) { imageResource: String =>
          val imagePath = s"static/images/$imageResource"
          logger.debug(imagePath)
          getFromResource(imagePath)
        } ~
        post {
          // display version
          path("version") {
            complete(utils.mkDescription)
          } ~
            // Handle parsing, etc
            path("api" / "annotate") {
              entity(as[api.TextMessage]) { m =>
                logger.info(s"Default Processor received POST with text -> ${m.text}")
                val processorsDoc = ProcessorsBridge.annotate(m.text)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            // annotate from sentences
            path("api" / "annotate") {
              entity(as[api.SegmentedMessage]) { sm =>
                logger.info(s"Default Processor received POST with text already split into sentences...")
                val processorsDoc = ProcessorsBridge.annotate(sm.segments)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            path("api" / "clu" / "annotate") {
              entity(as[api.TextMessage]) { m =>
                logger.info(s"CluProcessor received POST with text -> ${m.text}")
                val processorsDoc = ProcessorsBridge.annotateWithClu(m.text)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            path("api" / "fastnlp" / "annotate") {
              entity(as[api.TextMessage]) { m =>
                logger.info(s"FastNLPProcessor received POST with text -> ${m.text}")
                val processorsDoc = ProcessorsBridge.annotateWithFastNLP(m.text)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            // annotate from sentences
            path("api" / "fastnlp" / "annotate") {
              entity(as[api.SegmentedMessage]) { sm =>
                logger.info(s"FastNLPProcessor received POST with text already segmented into sentences ")
                val processorsDoc = ProcessorsBridge.annotateWithFastNLP(sm.segments)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            path("api" / "bionlp" / "annotate") {
              entity(as[api.TextMessage]) { m =>
                logger.info(s"BioNLPProcessor received POST with text -> ${m.text}")
                val processorsDoc = ProcessorsBridge.annotateWithBioNLP(m.text)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            // annotate from sentences
            path("api" / "bionlp" / "annotate") {
              entity(as[api.SegmentedMessage]) { sm =>
                logger.info(s"BioNLPProcessor received POST with text already segmented into sentences ")
                val processorsDoc = ProcessorsBridge.annotateWithBioNLP(sm.segments)
                val json = ConverterUtils.toJSON(processorsDoc)
                complete(json)
              }
            } ~
            // chunk
            path("api" / "fastnlp" / "chunk") {
              entity(as[JValue]) {
                case s: JValue if s \ "words" != JNothing =>
                  val sentence = ConverterUtils.toProcessorsSentence(s)
                  logger.info(s"FastNLP chunker")
                  val chunkedSentence = ProcessorsBridge.chunkWithFastNLP(sentence)
                  val json = ConverterUtils.toJSON(chunkedSentence)
                  complete(json)
                case d: JValue if d \ "sentences" != JNothing =>
                  val document = ConverterUtils.toProcessorsDocument(d)
                  logger.info(s"FastNLP chunker")
                  val chunkedDoc = ProcessorsBridge.chunkWithFastNLP(document)
                  val json = ConverterUtils.toJSON(chunkedDoc)
                  complete(json)
              }
            } ~
//            // lemmatize
//            path("api" / "fastnlp" / "lemmatize") {
//              entity(as[JValue]) {
//                case s: JValue if s \ "words" != JNothing =>
//                  val sentence = ConverterUtils.toProcessorsSentence(s)
//                  logger.info(s"FastNLP lemmatizer")
//                  val lemmatizedSentence = ProcessorsBridge.lemmatizeWithFastNLP(sentence)
//                  val json = ConverterUtils.toJSON(lemmatizedSentence)
//                  complete(json)
//                case d: JValue if d \ "sentences" != JNothing =>
//                  val document = ConverterUtils.toProcessorsDocument(d)
//                  logger.info(s"FastNLP lemmatizer")
//                  val lemmatizedDoc = ProcessorsBridge.lemmatizeWithFastNLP(document)
//                  val json = ConverterUtils.toJSON(lemmatizedDoc)
//                  complete(json)
//              }
//            } ~
//            // PoS tag
//            path("api" / "fastnlp" / "tag-parts-of-speech") {
//              entity(as[JValue]) {
//                case s: JValue if s \ "words" != JNothing =>
//                  val sentence = ConverterUtils.toProcessorsSentence(s)
//                  logger.info(s"FastNLP PoS tagger")
//                  val taggedSentence = ProcessorsBridge.tagPartsOfSpeechWithFastNLP(sentence)
//                  val json = ConverterUtils.toJSON(taggedSentence)
//                  complete(json)
//                case d: JValue if d \ "sentences" != JNothing =>
//                  val document = ConverterUtils.toProcessorsDocument(d)
//                  logger.info(s"FastNLP PoS tagger")
//                  val taggedDoc = ProcessorsBridge.tagPartsOfSpeechWithFastNLP(document)
//                  val json = ConverterUtils.toJSON(taggedDoc)
//                  complete(json)
//              }
//            } ~
            // Handle sentiment analysis of text
            path("api" / "sentiment" / "corenlp" / "score") {
              entity(as[JValue]) {
                case tm: JValue if tm \ "text" != JNothing =>
                  val m = tm.extract[api.TextMessage]
                  logger.info(s"CoreNLPSentimentAnalyzer received POST with text -> ${m.text}")
                  val scores = ProcessorsBridge.toSentimentScores(m.text)
                  complete(scores)
                // FIXME: this is conflict with sending a Sentence...perhaps rename?
                case sm: JValue if sm \ "segments" != JNothing =>
                  val m = sm.extract[api.SegmentedMessage]
                  logger.info(s"CoreNLPSentimentAnalyzer received POST with ${m.segments.size} sentences")
                  val scores = ProcessorsBridge.toSentimentScores(m.segments)
                  complete(scores)
                // Handle sentiment analysis of a Sentence
                case s: JValue if s \ "words" != JNothing =>
                  val sentence = ConverterUtils.toProcessorsSentence(s)
                  logger.info(s"CoreNLPSentimentAnalyzer received POST of Sentence")
                  val scores = ProcessorsBridge.toSentimentScores(sentence)
                  complete(scores)
                // Handle sentiment analysis of a Document
                case d: JValue if d \ "sentences" != JNothing =>
                  val document = ConverterUtils.toProcessorsDocument(d)
                  logger.info(s"CoreNLPSentimentAnalyzer received POST of Document")
                  val scores = ProcessorsBridge.toSentimentScores(document)
                  complete(scores)
              }
            } ~
            // Handle sentiment analysis of a seq of text
            path("api" / "sentiment" / "corenlp" / "score" / "segmented") {
              entity(as[api.SegmentedMessage]) { sm =>
                logger.info(s"CoreNLPSentimentAnalyzer received POST with text already segmented into sentences")
                val sentences: Seq[String] = sm.segments
                val scores = ProcessorsBridge.toSentimentScores(sentences)
                complete(scores)
              }
            } ~
            path("api" / "sentiment" / "corenlp" / "score") {
              entity(as[JValue]) { json =>
                val document: processors.Document = ConverterUtils.toProcessorsDocument(json)
                val scores = ProcessorsBridge.toSentimentScores(document)
                complete(scores)
              }
            } ~
            // Handle IE with Odin
            path("api" / "odin" / "extract") {
              entity(as[JValue]) {
                case dwu if dwu \ "document" != JNothing && dwu \ "url" != JNothing =>
                  logger.info(s"Odin endpoint received DocumentWithRulesURL")
                  val document = ConverterUtils.toProcessorsDocument(dwu \ "document")
                  val url = (dwu \ "url").extract[String]
                  val json = ProcessorsBridge.getMentionsAsJSON(document, ConverterUtils.urlToRules(url))
                  complete(json)
                case dwr if dwr \ "document" != JNothing && dwr \ "rules" != JNothing =>
                  logger.info(s"Odin endpoint received DocumentWithRules")
                  val document = ConverterUtils.toProcessorsDocument(dwr \ "document")
                  val rules = (dwr \ "rules").extract[String]
                  val json = ProcessorsBridge.getMentionsAsJSON(document, rules)
                  complete(json)
                case twr if twr \ "text" != JNothing && (twr \ "rules" != JNothing || twr \ "rules" == JNothing && twr \ "url" == JNothing) =>
                  logger.info(s"Odin endpoint received text without url")
                  try {
                    val texts = (twr \ "text").extract[List[String]]
                    var engine = ProcessorsBridge.engine
                    if (twr \ "rules" != JNothing) {
                      logger.info(s"Using custom rules")
                      val rulesStr = (twr \ "rules").extract[String]
                      engine = ExtractorEngine(rulesStr)
                      logger.info(s"Parsed custom rules")
                    }

                    var processors = DEFAULT_PROCESSORS
                    if (twr \ "processors" != JNothing) {
                      processors = (twr \ "processors").extract[String]
                    }

                    var mentionsJson = mutable.MutableList[JValue]()
                    var text_i = 0
                    for (text <- texts) {
                      val document = ProcessorsBridge.annotateWithFastNLP(text, processors)
                      val textMentionsJson = ProcessorsBridge.getMentionsAsJSON(document, engine)
                      mentionsJson += textMentionsJson
                      text_i += 1
                      logger.info(text_i.toString)
                    }
                    complete(mentionsJson)
                  } catch {
                    case e: Throwable => {
                      complete(ConverterUtils.toJSON(e))
                    }
                  }
                case twu if twu \ "text" != JNothing && twu \ "url" != JNothing =>
                  logger.info(s"Odin endpoint received TextWithRulesURL")
                  val text = (twu \ "text").extract[String]
                  val url = (twu \ "url").extract[String]
                  val document = ProcessorsBridge.annotateWithFastNLP(text)
                  val json = ProcessorsBridge.getMentionsAsJSON(document, ConverterUtils.urlToRules(url))
                  complete(json)
              }
            } ~
            path("api" / "odin" / "load-rules") {
              entity(as[JValue]) {
                case twr if twr \ "rules" != JNothing =>
                  logger.info(s"Odin endpoint received rules to load")
                  try {
                    logger.info(s"Parsing custom rules")
                    val rulesStr = (twr \ "rules").extract[String]
                    ProcessorsBridge.engine = ExtractorEngine(rulesStr)
                    logger.info(s"Parsed custom rules")
                    complete(parse("""{"is_success":true}"""))
                  }
                  catch {
                    case e: Throwable => {
                      complete(ConverterUtils.toJSON(e))
                    }
                  }
              }
            } ~
            path("api" / "openie" / "entities" / "extract") {
              entity(as[JValue]) {
                case s: JValue if s \ "words" != JNothing =>
                  val sentence = ConverterUtils.toProcessorsSentence(s)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractEntities(sentence)
                  complete(mentions)
                case d: JValue if d \ "sentences" != JNothing =>
                  val document = ConverterUtils.toProcessorsDocument(d)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractEntities(document)
                  complete(mentions)
              }
            } ~
            path("api" / "openie" / "entities" / "base-extract") {
              entity(as[JValue]) {
                case s: JValue if s \ "words" != JNothing =>
                  val sentence = ConverterUtils.toProcessorsSentence(s)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractBaseEntities(sentence)
                  complete(mentions)
                case d: JValue if d \ "sentences" != JNothing =>
                  val document = ConverterUtils.toProcessorsDocument(d)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractBaseEntities(document)
                  complete(mentions)
              }
            } ~
            path("api" / "openie" / "entities" / "extract-filter") {
              entity(as[JValue]) {
                case s: JValue if s \ "words" != JNothing =>
                  val sentence = ConverterUtils.toProcessorsSentence(s)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractAndFilterEntities(sentence)
                  complete(mentions)
                case d: JValue if d \ "sentences" != JNothing =>
                  val document = ConverterUtils.toProcessorsDocument(d)
                  logger.info(s"Openie Entity Extractor")
                  val mentions = ProcessorsBridge.extractAndFilterEntities(document)
                  complete(mentions)
              }
            } ~
            // shuts down the server
            path("shutdown") {
              complete {
                // complete request and then shut down the server in 1 second
                in(1.second) {
                  system.terminate()
                }
                "Stopping processors-server..."
              }
            }
        }
    }
  }
}
