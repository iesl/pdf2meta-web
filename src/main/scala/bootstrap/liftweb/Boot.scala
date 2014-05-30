package bootstrap.liftweb

import net.liftweb._
import common._
import edu.umass.cs.iesl.pdf2meta.webapp.lib.ImageLogic
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.{ShowMetataggerComponent, ShowPdfComponent}
import edu.umass.cs.iesl.pdf2meta.cli.WebPipelineComponent
import http._
import edu.umass.cs.iesl.pdf2meta.cli.readingorder.RectangularReadingOrder
import edu.umass.cs.iesl.pdf2meta.cli.coarsesegmenter.{PerceptronCoarseSegmenterComponent, AlignedPerceptronCoarseSegmenterComponent}
import edu.umass.cs.iesl.pdf2meta.cli.segmentsmoother.BestCoarseLabelModelAligner
import edu.umass.cs.iesl.pdf2meta.cli.config.{StandardCoarseLabelModel, StandardScoringModel}
import edu.umass.cs.iesl.pdf2meta.cli.extract.metatagger.MetataggerExtractor

//import org.scala_tools.subcut.inject.NewBindingModule
import com.escalatesoft.subcut.inject.NewBindingModule
//import NewBindingModule._
//import module.NewBindingModule
import com.davidsoergel.dsutils.PropertiesUtils
import edu.umass.cs.iesl.pdf2meta.webapp.snippet.PdfExamples
import edu.umass.cs.iesl.pdf2meta.cli.extract.pdfbox.{SpaceEstimator, PdfBoxExtractor}
import edu.umass.cs.iesl.pdf2meta.cli.pagetransform._
import edu.umass.cs.iesl.pdf2meta.cli.extract.PdfMinerExtractor

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {

  def boot {
    implicit val bindingModule = ProjectConfiguration

    // Use HTML5 for rendering
    //   LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))
    // where to search snippet
    LiftRules.addToPackages("edu.umass.cs.iesl.pdf2meta.webapp")

    LiftRules.snippets.append({
      case List("showpdf") => new WiredApp.ShowPdf()
    })

    LiftRules.snippets.append({
      case List("showmetatagger") => new WiredAppMetatagger.ShowMetatagger()
    })

    LiftRules.snippets.append({
      case List("pdfexamples") => new PdfExamples().render
    }) // ensure that the subcut config is passed


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

object WiredApp extends ShowPdfComponent with WebPipelineComponent {

  val pdfExtractor = new PdfBoxExtractor
  //val pdfExtractor = new PdfMinerExtractor

  val docTransformer = new DocTransformerPipelineComponent {
    val transformers = List(

	    new PageHonoringDocFlattener
    // don't try to merge lines or paras before sorting, because they may not be in order
      //, new ContinuousLineMerger
	 // , new SpaceEstimator

    // try merging paragraphs before slicing, to avoid slicing a para in half
	//  , new IndentedParagraphsMerger

      // top-down phase
      , new WhitespaceDocPartitioner //Slicing
      , new WeakPartitionRemover
      , new ContinuousLineMerger
      , new SpaceEstimator
      , new DocDeepSorter(RectangularReadingOrder)

      // bottom-up phase
      , new LineMerger
      , new SidewaysLineMerger
      //, new IndentedParagraphsMerger
      , new EmptyEndNodeAdder

      // finally ditch any intermediate hierarchy levels
      , new DocFlattener
    )

  }



  val coarseSegmenter = new AlignedPerceptronCoarseSegmenterComponent {
    lazy val perceptronPhase = new PerceptronCoarseSegmenterComponent {
      lazy val scoringModel = StandardScoringModel
    }
    lazy val segmentSmoother = new BestCoarseLabelModelAligner {
      val coarseLabelModels = List(new StandardCoarseLabelModel) //, new LetterCoarseLabelModel)
    }
  }

  val pipeline = new Pipeline;
}


//kzaporojets: configuration for being used with the output produced by metatagger
object WiredAppMetatagger extends ShowMetataggerComponent with WebPipelineComponent {

  val pdfExtractor = new PdfBoxExtractor
  //val pdfExtractor = new PdfMinerExtractor

  val docTransformer = new DocTransformerPipelineComponent {
    val transformers = List(
      new MetataggerExtractor
    )

  }
  val coarseSegmenter = new AlignedPerceptronCoarseSegmenterComponent {
    lazy val perceptronPhase = new PerceptronCoarseSegmenterComponent {
      lazy val scoringModel = StandardScoringModel
    }
    lazy val segmentSmoother = new BestCoarseLabelModelAligner {
      val coarseLabelModels = List(new StandardCoarseLabelModel) //, new LetterCoarseLabelModel)
    }
  }

  val pipeline = new Pipeline;
}

object ProjectConfiguration extends NewBindingModule({
  module =>
    import module._
    // can now use bind directly

    val props = PropertiesUtils.loadPropsFromFile(PropertiesUtils.findPropertiesFile("pdf2meta.properties", ".pdf2meta", "pdf2meta.properties"))
    bind[String] idBy 'convert toSingle props.getProperty("convert")
    bind[String] idBy 'examples toSingle props.getProperty("examples")
})
