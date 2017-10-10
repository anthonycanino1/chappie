package chappie;

public class AsynchronousChaperone extends Chaperone {

  private int assigned = 0;

  private Thread watcherThread;
  private Watcher watcher = new Watcher();

  public AsynchronousChaperone() {
    watcherThread = new Thread(watcher);
    watcherThread.setName("Chaperone");
    watcherThread.start();
  }

  public synchronized void assign() {
    if(assigned++ == 0)
      watcher.assign();
  }
  public synchronized double dismiss() {
    if(--assigned == 0)
      watcher.dismiss();

    return 0;
  }

  public void retire() {
    watcher.stop();
    try {
      watcherThread.join();
    } catch (InterruptedException e) { }

    timeLine.putAll(watcher.read());
    super.retire();
  }
}
