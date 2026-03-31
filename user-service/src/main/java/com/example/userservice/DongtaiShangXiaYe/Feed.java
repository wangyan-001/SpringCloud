package com.example.userservice.DongtaiShangXiaYe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feed {
    /** 动态唯一ID */
    private String feedId;
    /** 动态内容 */
    private String content;
    /** 发布时间戳（毫秒级） */
    private Long timestamp;
}
