package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * @author gushouye
 * @description 分页查询分页参数
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class PageParams {

    @ApiModelProperty("当前页码")
    private Long pageNo = 1L;

    @ApiModelProperty("每页记录数默认值")
    private Long pageSize = 30L;


}
