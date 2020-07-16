package rental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import rental.config.kafka.KafkaProcessor;

import java.util.List;

@Service
public class OrderStateViewHandler {


    @Autowired
    private OrderStateRepository orderStateRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_ (@Payload Ordered ordered) {
        try {
            if (ordered.isMe()) {
                // view 객체 생성
                OrderState orderState = new OrderState();
                // view 객체에 이벤트의 Value 를 set 함
                orderState.setOrderId(ordered.getId());
                orderState.setStatus(ordered.getStatus());
                orderState.setContractDate(ordered.getContractDate());
                orderState.setProductId(ordered.getProductId());

                // view 레파지 토리에 save
                orderStateRepository.save(orderState);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrderCanceled_then_UPDATE_(@Payload OrderCanceled orderCanceled) {
        try {
            if (orderCanceled.isMe()) {
                // view 객체 조회
                List<OrderState> orderStateList = orderStateRepository.findByOrderId(orderCanceled.getId());
                for(OrderState orderState : orderStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderState.setStatus(orderCanceled.getStatus());
                    // view 레파지 토리에 save
                    orderStateRepository.save(orderState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDelivered_then_UPDATE_(@Payload Delivered delivered) {
        try {
            if (delivered.isMe()) {
                // view 객체 조회
                List<OrderState> orderStateList = orderStateRepository.findByOrderId(delivered.getOrderId());
                for(OrderState orderState : orderStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderState.setDeliveryId(delivered.getId());
                    orderState.setDeliveryStatus(delivered.getStatus());
                    orderState.setOrderId(delivered.getOrderId());
                    // view 레파지 토리에 save
                    orderStateRepository.save(orderState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCanceled_then_UPDATE_(@Payload DeliveryCanceled deliveryCanceled) {
        try {
            if (deliveryCanceled.isMe()) {
                // view 객체 조회
                List<OrderState> orderStateList = orderStateRepository.findByOrderId(deliveryCanceled.getOrderId());
                for(OrderState orderState : orderStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderState.setDeliveryStatus(deliveryCanceled.getStatus());
                    // view 레파지 토리에 save
                    orderStateRepository.save(orderState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenChecked_then_UPDATE_(@Payload Checked checked) {
        try {
            if (checked.isMe()) {
                // view 객체 조회
                List<OrderState> orderStateList = orderStateRepository.findByOrderId(checked.getOrderId());
                for(OrderState orderState : orderStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderState.setCheckStatus(checked.getStatus());
                    orderState.setScore(checked.getScore());
                    // view 레파지 토리에 save
                    orderStateRepository.save(orderState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenScheduleFixed_then_UPDATE_(@Payload ScheduleFixed scheduleFixed) {
        try {
            if (scheduleFixed.isMe()) {
                // view 객체 조회
                List<OrderState> orderStateList = orderStateRepository.findByOrderId(scheduleFixed.getOrderId());
                for(OrderState orderState : orderStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderState.setCheckDate(scheduleFixed.getCheckDate());
                    orderState.setCheckId(scheduleFixed.getId());
                    orderState.setCheckStatus(scheduleFixed.getStatus());
                    // view 레파지 토리에 save
                    orderStateRepository.save(orderState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

   // @StreamListener(KafkaProcessor.INPUT)
    //public void when_then_DELETE_(@Payload  ) {
   //     try {
   //         if (.isMe()) {
                // view 레파지 토리에 삭제 쿼리
  //              orderStateRepository.deleteBy(.get());
   //         }
   //     }catch (Exception e){
   //         e.printStackTrace();
  //      }
   // }
}