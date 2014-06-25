/** FCFSQueue.java 19.05.2014
 * 
 */
package ascient.threading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import ascient.interfaces.Queue;

/**
 * Place class description here
 * 
 * @author stroucki
 * @version 1.0
 * 
 */
public class FCFSQueue<T> implements Queue<T> {

  // queue name
  private String name;

  // queue length
  private int length = 0;
  private Object lengthLock = new Object();

  // task storage
  private BlockingQueue<RunnableTask<T>> taskList = new ArrayBlockingQueue<RunnableTask<T>>(100);
  private Object listLock = new Object();

  // queue status
  private boolean shutdown = false;
  private int cycles = 0;

  // task status
  // private boolean waiting = false;

  private Arrival arrival = new Arrival();
  private Service service = new Service();

  private class Arrival {
    // arrival rate (lambda)
    private double arrivalRate = 0.0;
    private long lastTime = System.currentTimeMillis();

    public synchronized void click() {
      long nowTime = System.currentTimeMillis();
      long timeDiff = nowTime - lastTime;
      lastTime = nowTime;
      timeDiff++;
      // weighted average
      arrivalRate = 0.9 * arrivalRate + 0.1 * (1000.0 / timeDiff);
      incrementLength();
    }

    /**
     * @return the arrivalRate
     */
    public double getArrivalRate() {
      return arrivalRate;
    }
  }

  private class Service {
    // service rate (mu)
    private double serviceRate = 0.0;
    private long lastTime = System.currentTimeMillis();

    public synchronized void click() {
      long nowTime = System.currentTimeMillis();
      long timeDiff = nowTime - lastTime;
      lastTime = nowTime;
      timeDiff++;
      // weighted average
      serviceRate = 0.9 * serviceRate + 0.1 * (1000.0 / timeDiff);
      decrementLength();
    }

    /**
     * @return the serviceRate
     */
    public double getServiceRate() {
      return serviceRate;
    }

  }

  public FCFSQueue(String name) {
    this.name = name;
    new Thread(new Dispatcher<T>()).start();
  }

  @Override
  public void put(RunnableTask<T> r) {
    arrival.click();
    cycles++;
    if (cycles % 100 == 0) {
      System.out.println(report());
    }
    try {
      taskList.put(r);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
  private void oldput(RunnableTask<T> r) {
    arrival.click();
    cycles++;
    
    if (cycles % 100 == 0) {
      System.out.println(report());
    }
    
    //System.out.println(cycles);
    synchronized (listLock) {
      try {
        taskList.put(r);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      listLock.notifyAll();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see ascient.interfaces.Queue#take()
   */
  @Override
  public RunnableTask<T> take() {
    try {
      RunnableTask<T> task = taskList.take();
      service.click();
      return task;

    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;

  }
  private RunnableTask<T> oldtake() {
    synchronized (listLock) {

      while (taskList.isEmpty()) {
        try {
          listLock.wait();
        } catch (InterruptedException e) {
          // got some
          ;
        }
      }

      service.click();

      try {
        RunnableTask<T> task = taskList.take();
        return task;

      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      return null;

    }
  }

  private void incrementLength() {
    synchronized (lengthLock) {
      length++;
    }
  }

  private void decrementLength() {
    synchronized (lengthLock) {
      length--;
    }
  }

  public String report() {
    int mylen = length;
    double mylambda = arrival.getArrivalRate();
    double mymu = service.getServiceRate();

    StringBuilder sb = new StringBuilder();
    sb.append("Queue ");
    sb.append(name);
    sb.append(" (length ");
    sb.append(mylen);
    sb.append(" arrival rate: ");
    sb.append(mylambda);
    sb.append(" service rate: ");
    sb.append(mymu);
    sb.append(" utilization: ");
    sb.append(mylambda/mymu);
    sb.append(')');

    return sb.toString();
  }
  
  public void setShutdown(boolean flag) {
    shutdown = flag;
  }
  
  int poolSize = 4;
  ExecutorService executor = Executors.newFixedThreadPool(poolSize);
  //ExecutorService executor = new ThreadPoolExecutor(0, 10, 10, TimeUnit.SECONDS, workQueue, handler);
 
  //ExecutorService executor = Executors.newWorkStealingPool(4);
  private class Dispatcher<X> implements Runnable {

    @Override
    public void run() {
      Semaphore semaphorePool = new Semaphore(poolSize);

      while (!shutdown) {
        LimitedReleaseSemaphore s = new LimitedReleaseSemaphore(semaphorePool);
        //System.out.println(name+"val: "+semaphorePool.availablePermits());
        RunnableTask<T> r = take();
        //executor.execute(command);
        //System.out.println("A");
        Future<T> result = executor.submit(r.getCallable());
        //System.out.println("B");

        r.setSemaphore(s);
        r.setReturn(result);
        //System.out.println("here");
      }
    }

  }

}
