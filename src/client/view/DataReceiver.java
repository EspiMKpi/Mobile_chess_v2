package client.view;

import model.ObjectWrapper;

/**
 * Chủ: N04. Form nào muốn nhận gói tin thì implement và đăng ký vào
 * ClientCtr.getActiveFunction() dưới dạng new ObjectWrapper(mã, this).
 * ClientListening gọi hàm này trên EDT.
 */
public interface DataReceiver {
    void receivedDataProcessing(ObjectWrapper data);
}
