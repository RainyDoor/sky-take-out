package com.sky.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = getDateList(begin, end);

        List<BigDecimal> turnoverList = new ArrayList<>();
        for (LocalDate cur : dateList) {
            LocalDateTime curBegin = cur.atStartOfDay();
            LocalDateTime curEnd = cur.plusDays(1).atStartOfDay();
            turnoverList.add(orderMapper.selectOne(new QueryWrapper<Orders>()
                    .select("ifnull(sum(amount), 0) amount")
                    .eq("status", Orders.COMPLETED)
                    .between("order_time", curBegin, curEnd)).getAmount());
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate cur : dateList) {
            LocalDateTime curBegin = cur.atStartOfDay();
            LocalDateTime curEnd = cur.plusDays(1).atStartOfDay();
            int curUserAmount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .lt(User::getCreateTime, curEnd));
            int preUserAmount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .lt(User::getCreateTime, curBegin));
            totalUserList.add(curUserAmount);
            newUserList.add(curUserAmount - preUserAmount);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate cur : dateList) {
            LocalDateTime curBegin = cur.atStartOfDay();
            LocalDateTime curEnd = cur.plusDays(1).atStartOfDay();

            int total = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .between(Orders::getOrderTime, curBegin, curEnd));
            totalOrderCount += total;

            orderCountList.add(total);

            int valid = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .between(Orders::getOrderTime, curBegin, curEnd)
                    .eq(Orders::getStatus, Orders.COMPLETED));

            validOrderCount += valid;

            validOrderCountList.add(valid);
        }

        double orderCompletionRate = 1.0 * validOrderCount / (totalOrderCount == 0 ? 1 : totalOrderCount);

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();

        List<OrderDetail> orderDetailList = orderMapper.selectTop10(beginTime, endTime);
        for (OrderDetail orderDetail : orderDetailList) {
            nameList.add(orderDetail.getName());
            numberList.add(orderDetail.getNumber());
        }
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    @Override
    public void export(HttpServletResponse response) {
        //1.查询数据库，获取营业数据--查询最近30天
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.businessData(dateBegin, dateEnd);

        //2.通过POI将数据写入到Excel文件中
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(input);

            XSSFSheet sheet = excel.getSheet("Sheet1");
            XSSFRow row = sheet.getRow(1);

            //填充数据——时间
            row.getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            row = sheet.getRow(3);
            //填充营业额
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            //填充订单完成率
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            //填充新增用户数
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            //填充有效订单
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            //填充平均客单价
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                row = sheet.getRow(i + 7);

                LocalDate cur = LocalDate.now().minusDays(30 - i);
                BusinessDataVO data = workspaceService.businessData(cur, cur);
                row.getCell(1).setCellValue(cur.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }

            //3.通过输出流将Excel文件下载到客户端浏览器
            excel.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定日期范围内的每一天的集合
     * @param begin
     * @param end
     * @return
     */
    public List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate cur = begin; cur.isBefore(end); cur = cur.plusDays(1)) {
            dateList.add(cur);
        }
        dateList.add(end);
        return dateList;
    }
}
