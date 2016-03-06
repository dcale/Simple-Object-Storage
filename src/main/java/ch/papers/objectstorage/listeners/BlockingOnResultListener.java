package ch.papers.objectstorage.listeners;

import java.util.concurrent.CountDownLatch;

/**
 * Created by ale on 06/03/16.
 */
public class BlockingOnResultListener<T> implements OnResultListener<T> {
    private final CountDownLatch countDownLatch;
    private T resultObject;
    private String errorMessage;


    public BlockingOnResultListener(CountDownLatch countDownLatch){
        this.countDownLatch = countDownLatch;
    }

    public BlockingOnResultListener(){
        this(new CountDownLatch(1));
    }

    @Override
    public void onSuccess(T result) {
        this.resultObject = result;
        countDownLatch.countDown();
    }

    @Override
    public void onError(String message) {
        errorMessage = message;
        countDownLatch.countDown();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public T getResultObject() {
        return resultObject;
    }

    public boolean isSuccess(){
        return resultObject!=null;
    }
}
