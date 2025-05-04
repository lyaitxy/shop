package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class deleteCart {

    private final ICartService iCartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(name = "trade.topic"),
            key = "order.create"
    ))
    public void listenerDeleteCart(Set<Long> itemIds, Message message) {
        Long userId = message.getMessageProperties().getHeader("userId");
        log.info("接受到订单微服务传来的数据：{}, {}",userId, itemIds);
        UserContext.setUser(userId);
        iCartService.removeByItemIds(itemIds);
    }
}
