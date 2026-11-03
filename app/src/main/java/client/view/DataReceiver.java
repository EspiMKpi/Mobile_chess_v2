package client.view;

import model.ObjectWrapper;

/** Chủ: N04. Mọi Activity/Dialog nhận gói tin từ Server cài đặt interface này (luôn được gọi trên main thread). */
public interface DataReceiver {
    void receivedDataProcessing(ObjectWrapper data);
}
