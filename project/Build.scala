import sbt._
import Keys._
import Project.Initialize

object AsciidocBuild extends Build {
  val asciidoc = TaskKey[Set[File]]("asciidoc", "convert asciidoc sources into output formats")
  val asciidocHtml = TaskKey[Set[File]]("asciidoc-html", "convert asciidoc sources into HTML")
  val asciidocPdf = TaskKey[Set[File]]("asciidoc-pdf", "convert asciidoc sources into PDF")
  val asciidocDocbook = TaskKey[Set[File]]("asciidoc-docbook", "convert asciidoc sources into DocBook XML")
  val extractListings = TaskKey[Set[File]]("extract-listings", "cut marked sections out of files for inclusion in documentation")

  val mySettings =
    Seq(asciidoc <<= (asciidocHtml, asciidocPdf) map ((h,p)=> h ++ p),
        asciidocHtml <<= asciidocHtmlTask,
        asciidocPdf <<= asciidocPdfTask,
        sources in asciidocHtml <<= sources in asciidoc,
        sources in asciidocPdf <<= sources in asciidoc,
        sourceDirectory in asciidoc <<= baseDirectory(_ / "src" / "doc"),
        sources in asciidoc <<= (sourceDirectory in asciidoc) map {sd => (sd ** "*.asciidoc").get}
    )

  lazy val root = Project("sbt-asciidoc", file("."), settings = Defaults.defaultSettings ++ mySettings)

  private def asciidocHtmlTask: Initialize[Task[Set[File]]] = {
    (cacheDirectory, sourceDirectory in asciidoc, sources in asciidocHtml, target in asciidocHtml) map
      { (cache, sourceDirectory, sources, target) =>
        val cachedFun = FileFunction.cached(cache / "asciidoc", outStyle=FilesInfo.exists) { (in: Set[File]) =>
          in flatMap asciidoc2Html(sourceDirectory, target)
        }
        cachedFun(sources.toSet)
    }
  }

  private def asciidoc2Html(inputBase: File, target: File)(input: File): Option[File] = {
    IO.relativize(inputBase, input).map { output =>
      val outputFile = target / output.replace(".asciidoc", ".html")
      "asciidoc -o %s %s".format(outputFile, input) !;
      outputFile
    }
  }

  private def asciidocPdfTask: Initialize[Task[Set[File]]] = {
    (baseDirectory, sources in asciidocPdf, target in asciidocPdf) map { (baseDirectory, sources, target) =>
      sources.toSet
    }
  }

}
