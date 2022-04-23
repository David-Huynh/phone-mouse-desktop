package com.dhuynh;

// Singleton of LinkedBlockingQueue to store data from web socket
public class SocketQueue<E> extends java.util.concurrent.LinkedBlockingQueue<E> {
    private static final SocketQueue<?> queueInstance = new SocketQueue<>();
    
    private SocketQueue() {
        if (queueInstance != null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    public static SocketQueue<?> getInstance() {
        return queueInstance;
    }
}
