package com.sky.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private ReportService reportService;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 查询运营数据
     * @return
     */
    @Override
    public BusinessDataVO businessData(LocalDate begin, LocalDate end) {

        OrderReportVO ordersStatistics = reportService.getOrdersStatistics(begin, end);
        UserReportVO userStatistics = reportService.getUserStatistics(begin, end);
        TurnoverReportVO turnoverStatistics = reportService.getTurnoverStatistics(begin, end);

        int newUsers = Arrays.stream(userStatistics.getNewUserList().split(",")).mapToInt(Integer::parseInt).sum();
        double orderCompletionRate = ordersStatistics.getOrderCompletionRate();
        double turnover = Arrays.stream(turnoverStatistics.getTurnoverList().split(",")).mapToDouble(Double::parseDouble).sum();
        int validOrderCount = ordersStatistics.getValidOrderCount();
        double unitPrice = turnover / (validOrderCount == 0 ? 1 : validOrderCount);

        return BusinessDataVO.builder()
                .newUsers(newUsers)
                .orderCompletionRate(orderCompletionRate)
                .turnover(turnover)
                .unitPrice(unitPrice)
                .validOrderCount(validOrderCount)
                .build();
    }

    @Override
    public SetmealOverViewVO overviewSetmeals() {
        int discontinued = setmealMapper.selectCount(new LambdaQueryWrapper<Setmeal>()
                .eq(Setmeal::getStatus, StatusConstant.DISABLE));
        int sold = setmealMapper.selectCount(new LambdaQueryWrapper<Setmeal>()
                .eq(Setmeal::getStatus, StatusConstant.ENABLE));

        return SetmealOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public DishOverViewVO overviewDishes() {
        int discontinued = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, StatusConstant.DISABLE));
        int sold = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, StatusConstant.ENABLE));

        return DishOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public OrderOverViewVO overviewOrders() {
        int allOrders = orderMapper.selectCount(new LambdaQueryWrapper<>());
        int cancelledOrders = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.CANCELLED));
        int completedOrders = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.COMPLETED));
        int deliveredOrders = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.CONFIRMED));
        int waitingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED));

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();
    }
}
