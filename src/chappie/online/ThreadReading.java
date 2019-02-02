package chappie.online;

public class ThreadReading {
    public long user_jiffies;
    public long kernel_jiffies;
    public int core_id;
    public long tid;
    //Socket will be inferred from core_id
    public int socket;
    int epoch;
}
