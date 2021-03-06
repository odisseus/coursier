package coursier.cache.internal

import java.util.concurrent.{ExecutorService, LinkedBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

object ThreadUtil {

  private val poolNumber = new AtomicInteger(1)

  def fixedThreadPool(size: Int): ExecutorService = {

    val poolNumber0 = poolNumber.getAndIncrement()

    val threadNumber = new AtomicInteger(1)

    val factory: ThreadFactory =
      new ThreadFactory {
        def newThread(r: Runnable) = {
          val threadNumber0 = threadNumber.getAndIncrement()
          val t = new Thread(r, s"coursier-pool-$poolNumber0-thread-$threadNumber0")
          t.setDaemon(true)
          t.setPriority(Thread.NORM_PRIORITY)
          t
        }
      }

    // 1 min keep alive, so that threads get stopped a bit after resolution / downloading is done
    val executor = new ThreadPoolExecutor(
      size, size,
      1L, TimeUnit.MINUTES,
      new LinkedBlockingQueue[Runnable],
      factory
    )
    executor.allowCoreThreadTimeOut(true)
    executor
  }

  def withFixedThreadPool[T](size: Int)(f: ExecutorService => T): T = {

    var pool: ExecutorService = null
    try {
      pool = fixedThreadPool(size)
      f(pool)
    } finally {
      if (pool != null)
        pool.shutdown()
    }
  }

}
