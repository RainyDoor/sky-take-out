package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> list = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, time));

        if (list != null && !list.isEmpty()) {
            for (Orders order : list) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
        }

    }

    /**
     * 处理一直处于派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusHours(-1);

        List<Orders> list = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, time));

        if (list != null && !list.isEmpty()) {
            for (Orders order : list) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.updateById(order);
            }
        }

    }

}
