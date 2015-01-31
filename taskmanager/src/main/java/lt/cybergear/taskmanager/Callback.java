package lt.cybergear.taskmanager;

/**
 * Created by nk on 13.12.11.
 */
public interface Callback<T> {
    abstract void onResponseReceived(T response, Exception serviceError);
}
