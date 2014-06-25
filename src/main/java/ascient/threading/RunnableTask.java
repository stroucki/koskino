/** RunnableTask.java 19.05.2014
 * 
 */
package ascient.threading;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * RunnableTask as in "Applied Java Patterns"
 *
 * @author stroucki
 * @version 1.0
 *
 */
public class RunnableTask<T> {
  Callable<T> callable;
  Future<T> returnValue;
  LimitedReleaseSemaphore s;
  Object returnLock = new Object();
  boolean isDone = false;
  
  public RunnableTask(Callable<T>callable) {
    this.callable = callable;
  }
  
  public void setCallable(Callable<T> callable) {
    this.callable = callable;
  }
  public Callable<T> getCallable() {
    return callable;
  }
  
  public void setReturn(Future<T> value) {
    returnValue = value;
    isDone = true;
    synchronized(returnLock) {
      returnLock.notifyAll();
    }
  }
  
  public void setSemaphore(LimitedReleaseSemaphore s) {
    this.s = s;
  }
  
  public T returnValue() throws InterruptedException, ExecutionException {
    T retval;
    synchronized (returnLock) {
      if (!isDone) {
        try {
          returnLock.wait();
        } catch (InterruptedException e) {
         ;
        }
      }
    }
    //System.out.println("about to get");
    retval = returnValue.get();
    //System.out.println("gotten");
    if (s != null) {
      s.release();
    }
    return retval;
  }
}
