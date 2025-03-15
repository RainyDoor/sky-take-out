package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    void paySuccess(String outTradeNo);

    PageResult historyOrders(OrdersPageQueryDTO pageQueryDTO);

    void reminder(Long id);

    void repetition(Long id);

    void cancel(Long id);

    OrderVO orderDetail(Long id);

    OrderStatisticsVO orderStatistics();

    void complete(Long id);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void delivery(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
}
