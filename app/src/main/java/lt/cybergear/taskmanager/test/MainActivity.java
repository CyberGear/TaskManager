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
        TaskManager.relinkIfNecessary(this);

        textView = (TextView) findViewById(R.id.text);
    }

    public void onSendClick(View view) {
        textView.setText("");
        getSomething();
    }

    @TaskStartMethod
    private void getSomething() {
        getSomethingDeeper();
    }

    private void getSomethingDeeper() {
        SomeClientHelper.someMethod(this, new Callback<String>() {
            @Override
            public void onResponseReceived(String response, Exception serviceError) {
                textView.setText(response);
            }
        });
    }

    public static class SomeClientHelper {

        public static void someMethod(Object object, Callback<String> callback) {
            TaskManager.startTask(object, new Task<String>(callback) {
                @Override
                protected String doInBackground() throws Exception {
                    return SomeWebClient.someMethod();
                }
            });
        }

    }

    public static class SomeWebClient {
        public static String someMethod() throws Exception {
            Thread.sleep(3000);
            return "Kazkokia interneto uzklausa";
        }
    }

}
