package lt.cybergear.taskmanager.test;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import lt.cybergear.taskmanager.TaskManager;
import lt.cybergear.taskmanager.Callback;
import lt.cybergear.taskmanager.Task;
import lt.cybergear.taskmanager.TaskStartMethod;

public class MainActivity extends ActionBarActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * This line relinks all responses
         */
        TaskManager.relinkIfNecessary(this);

        textView = (TextView) findViewById(R.id.text);

        /**
         * I recommend to add this listener in Application instance.
         * it is helper for Oauth2 error handling
         */
        TaskManager.setTaskListener(new TaskManager.TaskListener() {

            /**
             * After exception in task this method is called for your judge
             * if exception is token exception, runs in different thread
             * @param e exception throwed in Task
             * @return result if token refreshed successfully
             */
            @Override
            public boolean isTokenExpired(Exception e) {
                return false;
            }

            /**
             * Is called if token is expired, runs in different thread
             * @return is token is renewed
             */
            @Override
            public boolean isTokenRenewed() {
                return false;
            }

            /**
             * Runs in UI thread, executed after failed attempt to renew token.
             */
            @Override
            public void onRenewFailed() {

            }
        });
    }

    public void onSendClick(View view) {
        textView.setText("");
        getSomething();
    }

    /**
     * Marks the method, witch registers callback from async task
     */
    @TaskStartMethod
    private void getSomething() {
        /**
         * This callback is relinking after rotation.
         */
        Callback<String> callback = new Callback<String>() {
            @Override
            public void onResponseReceived(String response, Exception serviceError) {
                textView.setText(response);
            }
        };

        SomeClientHelper.someMethod(this, callback);
    }

    public static class SomeClientHelper {

        public static void someMethod(Object object, Callback<String> callback) {
            /**
             * Background task
             */
            Task<String> task = new Task<String>(callback) {
                @Override
                protected String doInBackground() throws Exception {
                    return SomeWebClient.someMethod();
                }
            };
            /**
             * Starting task
             */
            TaskManager.startTask(object, task);
        }

    }

    public static class SomeWebClient {
        public static String someMethod() throws Exception {
            Thread.sleep(3000);
            return "Kazkokia interneto uzklausa";
        }
    }

}
