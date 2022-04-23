package com.dhuynh;

import java.util.concurrent.BlockingQueue;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/app")
public class PhoneServerEndpoint {


    @OnOpen
    public void onOpen(Session session) {
        System.out.println ("--- Connected: " + session.getId());
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        System.out.println("--- Message: " + message);	
        BlockingQueue<Integer[]> queue = (BlockingQueue<Integer[]>) SocketQueue.getInstance();
        Integer[] arr = new Integer[2];
        arr[0] = Integer.parseInt(message.split(",")[0]);
        arr[1] = Integer.parseInt(message.split(",")[1]);

        queue.offer(arr);
        return message;
    }
        
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("--- Session: " + session.getId());
        System.out.println("--- Closing because: " + closeReason);
    }
}