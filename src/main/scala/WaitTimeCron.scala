/**
  * Created by henneberger on 12/21/17.
  */

//Problem with the assembly plugin
object WaitTimeCron {
  def main(args: Array[String]): Unit = {
    val waitTime = new WaitTime()
    import java.util.concurrent._

    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = {
        println("Running...")
        waitTime.run()
      }
    }
    val f = ex.scheduleAtFixedRate(task, 1, 30, TimeUnit.SECONDS)
  }
}
