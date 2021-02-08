import me.sbarthol.actors.ParserActor.highlight

object AnalyzerTest {

  def main(args: Array[String]): Unit = {

    val txt =
      """Mr Dursley was the director of a firm called Grunnings, 
        |which made drills. He was a big, beefy man with hardly any neck, 
        |although he did have a very large moustache. Mrs Dursley was 
        |thin and blonde and had nearly twice the usual amount of neck, 
        |which came in very useful as she spent so much of her time craning 
        |over garden fences, spying on the neighbours. The Dursleys had 
        |a small son called Dudley and in their opinion there was no 
        |finer boy anywhere.""".stripMargin

    println(highlight(text = txt, keywords = List("dursley")))
  }
}
