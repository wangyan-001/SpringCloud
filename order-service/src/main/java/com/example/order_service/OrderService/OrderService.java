package com.example.order_service.OrderService;

import com.example.order_service.FeignClient.ProductFeignClient;
import com.example.order_service.FeignClient.UserServiceFeignClient;
import com.example.order_service.config.RabbitOrderConfig;
import com.example.order_service.dto.OrderDTO;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderCreatedEvent;
import com.example.order_service.mapper.OrderMapper;
import io.seata.spring.annotation.GlobalTransactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private UserServiceFeignClient userFeignClient;

    // 分布式事务注解：所有异常都回滚
    @GlobalTransactional(rollbackFor = Exception.class)
    public void createOrder(Order order) throws InterruptedException{
        //try {
            // 1. 本地保存订单
            orderMapper.insert(order);

            // 2. 远程扣减库存
            //productFeignClient.deductStock(order.getProductId(), order.getCount());

            // 3. 远程扣减余额
            userFeignClient.deductBalance(order.getUserId(), order.getAmount());

            // ======================
            // 测试：打开下面这行，所有服务都会回滚
            //int i = 1 / 0;
            // ======================

            // 暂停 10 秒，让事务保持 Active 状态
            //Thread.sleep(10000);

            // 再抛异常触发回滚
           // throw new RuntimeException("测试回滚");
            // int i = 1 / 0;

       /* }catch (Exception e) {
            // ❌ 错误：吞掉异常，Seata 收不到回滚信号
            // System.out.println(e.getMessage());

            // ✅ 正确：抛出异常，触发 Seata 回滚
            throw new RuntimeException("下单失败", e);
        }*/
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitOrderConfig config;
    public void createOrderMq(OrderDTO orderDTO) {
        // 1. 执行下单逻辑（保存数据库、生成ID...）
        String orderId = "ORDER_" + System.currentTimeMillis();
        log.info("订单 {} 创建成功", orderId);

        // 2. 构造消息事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setUserId(orderDTO.getUserId());
        event.setProductId(orderDTO.getProductId());
        event.setAmount(orderDTO.getAmount());

        // 3. 发送 Fanout 广播消息
        // 注意：Fanout 模式下，Routing key 填 "" 或 null 都可以
        rabbitTemplate.convertAndSend(config.EXCHANGE_NAME, "", event);
        log.info("订单 {} 已广播 Fanout 消息", orderId);
    }
}
