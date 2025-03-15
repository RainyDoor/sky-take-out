package com.sky.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常（地址簿为空、购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询当前用户的购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getAddress());

        orderMapper.insert(orders);


        //向订单明细表插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            log.info("购物车详细：{}", cart);
            log.info("订单详细：{}", orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        //清空当前用户的购物车数据
        shoppingCartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));

        //封装VO返回结果

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal("0.01"), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        Orders orders = Orders.builder()
                .payStatus(Orders.PAID)
                .status(Orders.TO_BE_CONFIRMED)
                .checkoutTime(LocalDateTime.now())
                .number(ordersPaymentDTO.getOrderNumber())
                .build();

        orderMapper.update(orders, new LambdaQueryWrapper<Orders>()
                .eq(Orders::getNumber, orders.getNumber()));

        return vo;
    }

    @Override
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(Orders::getNumber, outTradeNo));
        log.info("orders: {}", ordersDB);

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);
    }

    @Override
    public PageResult historyOrders(OrdersPageQueryDTO pageQueryDTO) {
        Long userId = BaseContext.getCurrentId();
        IPage<Orders> iPage = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(pageQueryDTO.getStatus() != null, Orders::getStatus, pageQueryDTO.getStatus())
                .orderByDesc(Orders::getOrderTime);
        orderMapper.selectPage(iPage, wrapper);

        List<OrderVO> list = new ArrayList<>();
        for (Orders order : iPage.getRecords()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            Long orderId = order.getId();
            List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                    .eq(OrderDetail::getOrderId, orderId));
            orderVO.setOrderDetailList(orderDetails);
            list.add(orderVO);
        }

        return new PageResult(iPage.getTotal(), list);
    }

    @Override
    public void reminder(Long id) {
        //TODO 实现催单功能
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
            BeanUtils.copyProperties(orderDetail, shoppingCartDTO);
            for (int i = 0; i < orderDetail.getNumber(); i++) {
                shoppingCartService.addShoppingCart(shoppingCartDTO);
            }
        }
    }

    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.selectById(id);

        //支付状态
//        Integer payStatus = orders.getPayStatus();
//        if (payStatus == 1) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        orders.setStatus(Orders.CANCELLED);
        orderMapper.updateById(orders);
    }

    @Override
    public OrderVO orderDetail(Long id) {
        Orders orders = orderMapper.selectById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        long orderId = orders.getId();
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Override
    public OrderStatisticsVO orderStatistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(orderMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.CONFIRMED)));
        orderStatisticsVO.setToBeConfirmed(orderMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)));
        return orderStatisticsVO;
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.selectById(id);

        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.COMPLETED);
        orderMapper.updateById(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.selectById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
//        Integer payStatus = orders.getPayStatus();
//        if (payStatus == Orders.PAID) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = orderMapper.selectById(ordersConfirmDTO.getId());

        if (orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.CONFIRMED);
        orderMapper.updateById(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.selectById(id);

        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.updateById(orders);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
//        log.info("orderPageQueryDTO: {}", ordersPageQueryDTO);

        IPage<Orders> iPage = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .ge(ordersPageQueryDTO.getBeginTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .le(ordersPageQueryDTO.getEndTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .like(ordersPageQueryDTO.getNumber() != null, Orders::getNumber, ordersPageQueryDTO.getNumber())
                .like(ordersPageQueryDTO.getPhone() != null, Orders::getPhone, ordersPageQueryDTO.getPhone())
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())
                .orderByDesc(Orders::getOrderTime);

        orderMapper.selectPage(iPage, wrapper);

        List<OrderVO> list = new ArrayList<>();
        for (Orders order : iPage.getRecords()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDishes(getOrderDishesStr(order.getId()));
            list.add(orderVO);
        }

        return new PageResult(iPage.getTotal(), list);
    }

    private String getOrderDishesStr(Long orderId) {
        List<OrderDetail> list = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        StringJoiner orderDishesJoiner = new StringJoiner(",");
        for (OrderDetail orderDetail : list) {
            orderDishesJoiner.add(orderDetail.getName() + "*" + orderDetail.getNumber());
        }
        return orderDishesJoiner.toString();
    }
}
