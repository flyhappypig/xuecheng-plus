package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author gushouye
 * @description
 **/
@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto courseParamsDto) {
        // 拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 根据名称模糊查询，在sql中拼接 course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()), CourseBase::getName, courseParamsDto.getCourseName());
        // 根据课程的审核状态查询 course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, courseParamsDto.getAuditStatus());
        // 根据课程的发布状态查询 course_base.publish_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()), CourseBase::getStatus, courseParamsDto.getPublishStatus());
        // 根据课程的公司id查询
        queryWrapper.eq(CourseBase::getCompanyId, companyId);
        // 分页参数对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 数据列表
        List<CourseBase> items = page.getRecords();
        // 总记录数
        long total = pageResult.getTotal();
        PageResult<CourseBase> basePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        return basePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {

        // 向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        // 将传入页面的参数放到courseBaseNew对象
        // 一个一个set太慢，且复杂
        // courseBaseNew.setName(dto.getName());
        BeanUtils.copyProperties(dto, courseBaseNew);// 属性名称一致可以拷贝
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        // 审核状态默认为未提交
        courseBaseNew.setAuditStatus("202002");
        // 发布状态为未发布
        courseBaseNew.setStatus("203001");
        int result = courseBaseMapper.insert(courseBaseNew);
        if (result <= 0) {
            throw new RuntimeException("添加课程失败");
        }
        // 向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(dto, courseMarketNew);
        // 主键的课程id
        courseMarketNew.setId(courseBaseNew.getId());
        saveCourseMarket(courseMarketNew);
        // 从数据查出课程的详细信息，包括课程信息和营销信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseBaseNew.getId());
        return courseBaseInfo;
    }

    // 单独写一个方法保存营销信息，存在则更新，不存在则添加
    private int saveCourseMarket(CourseMarket courseMarket) {
        // 参数合法性校验
        if (StringUtils.isEmpty(courseMarket.getCharge())) {
            XueChengPlusException.cast("收费规则为空");
        }
        // 如果课程收费，价格为空
        if (courseMarket.getCharge().equals("201001")) {
            if (courseMarket.getPrice() == null || courseMarket.getPrice().floatValue() <= 0) {
                XueChengPlusException.cast("课程的价格不能为空并且必须大于0");
            }
        }
        // 从数据查询营销信息，存在则更新，不存在则添加
        CourseMarket market = courseMarketMapper.selectById(courseMarket.getId());
        if (market == null) {
            // 插入数据库
            return courseMarketMapper.insert(courseMarket);
        } else {
            //将courseMarket传入的数据拷贝到market
            BeanUtils.copyProperties(courseMarket, market);
            market.setId(courseMarket.getId());
            // 更新
            return courseMarketMapper.updateById(courseMarket);
        }
    }

    // 查询课程信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //从课程信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        // 从课程营销信息表查询
        // 组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        // 通过courseCategoryMapper查询分类信息，将分类名称放在courseBaseInfoDto对象
        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategory.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        // 拿到课程id
        Long id = editCourseDto.getId();
        // 查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        // 数据合法性校验
        // 根据具体的业务逻辑去校验--->本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //封装数据
        BeanUtils.copyProperties(editCourseDto, courseBase);
        // 修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        // 更新数据库
        int result = courseBaseMapper.updateById(courseBase);
        if (result <= 0) {
            XueChengPlusException.cast("修改课程失败");
        }
        // 更新营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        int courseMarketResult = saveCourseMarket(courseMarket);
        if (courseMarketResult <= 0) {
            XueChengPlusException.cast("修改课程营销信息失败");
        }
        // 查询课程信息
        return getCourseBaseInfo(courseBase.getId());
    }


}
