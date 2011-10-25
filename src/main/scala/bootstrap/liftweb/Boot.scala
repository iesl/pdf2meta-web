package bootstrap.liftweb

import net.liftweb._
import common._
import edu.umass.cs.iesl.pdf2meta.webapp.lib.ImageLogic
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.ShowPdfComponent
import edu.umass.cs.iesl.pdf2meta.cli.extract.{XmlExtractorComponent, PdfMiner}
import edu.umass.cs.iesl.pdf2meta.cli.WebPipelineComponent
import http._
import edu.umass.cs.iesl.pdf2meta.cli.pagetransform._
import edu.umass.cs.iesl.pdf2meta.cli.readingorder.RectangularReadingOrder
import edu.umass.cs.iesl.pdf2meta.cli.coarsesegmenter.{WiredPerceptronComponent, StubCoarseSegmenter, CoarseSegmenterComponent}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot
  {

  def boot
    {

    // Use HTML5 for rendering
    //   LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))
    // where to search snippet
    LiftRules.addToPackages("edu.umass.cs.iesl.pdf2meta.webapp")

    LiftRules.snippets.append({case List("showpdf") => WiredApp.ShowPdf})


    // Build SiteMap
    /*   val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu

      Menu.i("File Upload") / "file_upload",

      Menu.i("Show examples") / "show_examples"
                      )

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))
*/
    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
            Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
            Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.dispatch.append(ImageLogic.matcher)
    }
  }


object WiredApp extends ShowPdfComponent with WebPipelineComponent with XmlExtractorComponent with
                        CoarseSegmenterComponent
  {

  val xmlExtractor = new PdfMiner

  val docTransformer = new DocTransformerComponent
    {
    val docTransformer = new DocTransformerPipelineComponent
      {
      val transformers = List(new PageStarDocPartitioner
                              // top-down phase
                              , new SlicingDocPartitioner
                              , new DocDeepSorter(RectangularReadingOrder)
                              //forget all about layout now that the atoms are in order
                              , new StarDocPartitioner
                              // bottom-up phase
                              , new LineMerger
                              //, new ReferencesMerger
                              , new ParagraphMerger
                              // finally ditch any intermediate hierarchy levels
                              , new AtomStarDocPartitioner)
      val docTransformer = new DocTransformerPipeline
      }.docTransformer
    }.docTransformer

  val coarseSegmenter = new WiredPerceptronComponent  //new StubCoarseSegmenter //
  val pipeline = new Pipeline;
  }


