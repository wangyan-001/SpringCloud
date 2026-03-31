package com.example.userservice.DongtaiShangXiaYe;

import redis.clients.jedis.Jedis;
import java.util.List;
import java.util.Scanner;

/**
 * Redis 列表动态上下滑页功能演示
 * 使用 Jedis 客户端操作 Redis List，实现类似分页浏览的效果
 */
public class RedisPaginationDemo {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String LIST_KEY = "test:items";  // Redis 列表的 key
    private static final int PAGE_SIZE = 5;               // 每页显示条目数

    public static void main(String[] args) {
        // 1. 连接 Redis
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("成功连接到 Redis");

            // 2. 初始化测试数据（清空旧数据，插入 100 条新数据）
            jedis.del(LIST_KEY);  // 先删除，避免之前的数据干扰
            for (int i = 1; i <= 100; i++) {
                jedis.rpush(LIST_KEY, "item:" + i);  // 从右侧推入，保持顺序
            }
            System.out.println("已向 Redis 列表 " + LIST_KEY + " 插入 100 条测试数据 (item:1 ~ item:100)");

            // 3. 获取列表总长度
            long total = jedis.llen(LIST_KEY);
            System.out.println("列表总长度: " + total + "\n");

            // 4. 初始化分页起始索引 (从 0 开始)
            int offset = 0;

            // 5. 交互式翻页循环
            while (true) {
                // 计算当前页的结束索引 (LRANGE 是闭区间)
                int end = offset + PAGE_SIZE - 1;
                // 确保 end 不超出列表最大索引
                if (end >= total) {
                    end = (int) (total - 1);
                }

                // 从 Redis 获取当前页数据
                List<String> pageData = jedis.lrange(LIST_KEY, offset, end);

                // 打印当前页信息
                System.out.println("\n========== 当前页 (索引 " + offset + " ~ " + end + ") ==========");
                if (pageData.isEmpty()) {
                    System.out.println("无数据");
                } else {
                    for (String item : pageData) {
                        System.out.println(item);
                    }
                }

                // 打印导航提示
                System.out.println("\n----------------------------------------");
                System.out.println("总条目: " + total + ", 每页: " + PAGE_SIZE);
                System.out.println("当前显示: " + (offset + 1) + " ~ " + (end + 1) + " 条");
                System.out.print("请输入操作: [n]下一页 [p]上一页 [q]退出: ");

                String input = scanner.nextLine().trim().toLowerCase();
                if ("q".equals(input)) {
                    System.out.println("退出程序");
                    break;
                } else if ("n".equals(input)) {
                    // 下一页：offset 增加 PAGE_SIZE，但不得超过最后一页的起始索引
                    int nextOffset = offset + PAGE_SIZE;
                    if (nextOffset < total) {
                        offset = nextOffset;
                    } else {
                        System.out.println("已是最后一页，无法继续下一页");
                    }
                } else if ("p".equals(input)) {
                    // 上一页：offset 减少 PAGE_SIZE，但不得小于 0
                    int prevOffset = offset - PAGE_SIZE;
                    if (prevOffset >= 0) {
                        offset = prevOffset;
                    } else {
                        System.out.println("已是第一页，无法继续上一页");
                    }
                } else {
                    System.out.println("无效输入，请重新输入");
                }
            }

        } catch (Exception e) {
            System.err.println("Redis 连接失败或操作异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}