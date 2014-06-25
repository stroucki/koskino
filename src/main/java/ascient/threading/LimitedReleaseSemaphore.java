package ascient.threading;

import java.util.concurrent.Semaphore;

public class LimitedReleaseSemaphore {
  private Semaphore s;
  private boolean active;
  
  public LimitedReleaseSemaphore(Semaphore s) {
    this.s = s;
    this.active = true;
    try {
      s.acquire();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  public synchronized void release() {
    if (active) {
      active = false;
      s.release();
      //System.out.println("permits: "+s.availablePermits());
    }
  }
}
