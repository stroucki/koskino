/** Queue.java 19.05.2014
 * 
 */
package ascient.interfaces;

import ascient.threading.RunnableTask;


/**
 * Queue interface as from "Applied Java Patterns"
 *
 * @author stroucki
 * @version 1.0
 *
 */
public interface Queue<T> {

  void put(RunnableTask<T> r);
  RunnableTask<T> take();
  
}
