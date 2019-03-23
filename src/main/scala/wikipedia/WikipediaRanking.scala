package wikipedia

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import org.apache.log4j.Logger
import org.apache.log4j.Level

case class WikipediaArticle(title: String, text: String)

object WikipediaRanking {

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val langs = List(
    "JavaScript", "Java", "PHP", "Python", "C#", "C++", "Ruby", "CSS",
    "Objective-C", "Perl", "Scala", "Haskell", "MATLAB", "Clojure", "Groovy")

  val conf: SparkConf = new SparkConf().setAppName("wikipedia").setMaster("local")
  val sc: SparkContext = new SparkContext(conf)
  val wikiRdd: RDD[WikipediaArticle] = sc.parallelize(WikipediaData.articles)

  /** Returns the number of articles on which the language `lang` occurs.
    * Hint1: consider using method `aggregate` on RDD[T].
    * Hint2: should you count the "Java" language when you see "JavaScript"?
    * Hint3: the only whitespaces are blanks " "
    * Hint4: no need to search in the title :)
    */
  def occurrencesOfLang(lang: String, rdd: RDD[WikipediaArticle]): Int = {
    rdd.aggregate(0)((acc: Int, wa: WikipediaArticle) => if (wa.text.contains(lang)) acc + 1 else acc, _ + _)
  }

  /** (1) Use `occurrencesOfLang` to compute the ranking of the languages
    * (`langs`) by determining the number of Wikipedia articles that
    * mention each language at least once. Don't forget to sort the
    * languages by their occurrence, in decreasing order!
    *
    * Note: this operation is long-running. It can potentially run for
    * several seconds.
    */
  def rankLangs(langs: List[String], rdd: RDD[WikipediaArticle]): List[(String, Int)] = {
    langs.map((l: String) => (l, occurrencesOfLang(l, rdd))).sortBy(-_._2)
  }

  /** Compute an inverted index of the set of articles, mapping each language
    * to the Wikipedia pages in which it occurs.
    */
  def makeIndex(langs: List[String], rdd: RDD[WikipediaArticle]): RDD[(String, Iterable[WikipediaArticle])] = {
    rdd.flatMap((article: WikipediaArticle) => langs.filter((l: String) => article.text.contains(l)).map((l: String) => (l, article))).groupByKey()
  }

  /** (2) Compute the language ranking again, but now using the inverted index. Can you notice
    * a performance improvement?
    *
    * Note: this operation is long-running. It can potentially run for
    * several seconds.
    */
  def rankLangsUsingIndex(index: RDD[(String, Iterable[WikipediaArticle])]): List[(String, Int)] = ???

  /** (3) Use `reduceByKey` so that the computation of the index and the ranking is combined.
    * Can you notice an improvement in performance compared to measuring *both* the computation of the index
    * and the computation of the ranking? If so, can you think of a reason?
    *
    * Note: this operation is long-running. It can potentially run for
    * several seconds.
    */
  def rankLangsReduceByKey(langs: List[String], rdd: RDD[WikipediaArticle]): List[(String, Int)] = ???

  def main(args: Array[String]) {

    /* Languages ranked according to (1) */
    val langsRanked: List[(String, Int)] = timed("Part 1: naive ranking", rankLangs(langs, wikiRdd))
    println(langsRanked)

    /* An inverted index mapping languages to wikipedia pages on which they appear */
    def index: RDD[(String, Iterable[WikipediaArticle])] = makeIndex(langs, wikiRdd)

    // index.foreach((t: (String, Iterable[WikipediaArticle])) => println(t._1 + s"[${t._2.take(3).map(_.title + " ;; ")}]"))

    /* Languages ranked according to (2), using the inverted index */
    val langsRanked2: List[(String, Int)] = timed("Part 2: ranking using inverted index", rankLangsUsingIndex(index))
    println(langsRanked2)

    /* Languages ranked according to (3) */
    val langsRanked3: List[(String, Int)] = timed("Part 3: ranking using reduceByKey", rankLangsReduceByKey(langs, wikiRdd))
    println(langsRanked3)

    /* Output the speed of each ranking */
    println(timing)
  }

  val timing = new StringBuffer

  def timed[T](label: String, code: => T): T = {
    val start = System.currentTimeMillis()
    val result = code
    val stop = System.currentTimeMillis()
    timing.append(s"Processing $label took ${stop - start} ms.\n")
    result
  }
}
