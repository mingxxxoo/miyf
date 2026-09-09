package cn.miyf.service;

import cn.miyf.bean.entity.BaseEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.AbstractCondition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 应用层通用基类：分页规范化、存在性校验，以及 Mapper 单表 CRUD / 批量操作封装。
 * <p>
 * 业务编排放在具体 Service；数据访问依赖 {@link BaseMapper} 接口。
 * 单表按 ID 删除/修改、新数据保存请优先调用本类方法，避免各业务重复编写。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public abstract class BaseApplicationService {

    /**
     * 默认页码（从 1 开始）
     */
    protected static final long DEFAULT_PAGE = 1L;
    /**
     * 默认每页条数
     */
    protected static final long DEFAULT_PAGE_SIZE = 20L;
    /**
     * 单页最大条数，防止过大分页拖垮查询
     */
    protected static final long MAX_PAGE_SIZE = 100L;

    /**
     * 规范化页码，非法值回落为默认页。
     *
     * @param page 请求页码，可为 null
     * @return 合法页码（&gt;= 1）
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected long normalizePage(Long page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 规范化每页条数，并限制上限。
     *
     * @param pageSize 请求每页条数，可为 null
     * @return 合法每页条数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 计算 SQL OFFSET。
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @return offset
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected long offset(long page, long pageSize) {
        return (page - 1) * pageSize;
    }

    /**
     * 构造空分页结果。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param <T>      记录类型
     * @return 空分页
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected <T> PageResult<T> emptyPage(long page, long pageSize) {
        return PageResult.of(List.of(), 0, page, pageSize);
    }

    /**
     * 从 Optional 取值，不存在则抛出业务异常。
     *
     * @param optional 可选值
     * @param message  不存在时的提示
     * @param <T>      类型
     * @return 非空值
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected <T> T requireFound(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, message));
    }

    /**
     * 空值校验，为 null 则抛出业务异常。
     *
     * @param value   值
     * @param message 提示
     * @param <T>     类型
     * @return 非空值
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected <T> T requireFound(T value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return value;
    }

    /**
     * 断言条件为真，否则按错误码抛出。
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected void requireTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言条件为真，否则按错误码与自定义文案抛出。
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   自定义文案
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected void requireTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 从 {@link AbstractCondition} 规范化页码。
     *
     * @param condition 查询条件
     * @return 页码
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    protected long pageOf(AbstractCondition condition) {
        return normalizePage((long) condition.getPage());
    }

    /**
     * 从查询条件规范化每页条数。
     *
     * @param condition 查询条件
     * @return 每页条数
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    protected long pageSizeOf(AbstractCondition condition) {
        return normalizePageSize((long) condition.getRows());
    }

    /**
     * 按主键加载，不存在则抛出。
     *
     * @param mapper  Mapper
     * @param id      主键
     * @param message 不存在提示
     * @param <E>     实体类型
     * @return 实体
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected <E> E requireById(BaseMapper<E> mapper, Long id, String message) {
        return requireFound(mapper.selectById(id), message);
    }

    /**
     * 按主键集合加载；任一条不存在则抛出。
     *
     * @param mapper  Mapper
     * @param ids     主键集合
     * @param message 不存在提示
     * @param <E>     实体类型
     * @return 实体列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E> List<E> requireByIds(BaseMapper<E> mapper, Collection<Long> ids, String message) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<E> list = mapper.selectBatchIds(ids);
        requireTrue(list.size() == ids.size(), ErrorCode.NOT_FOUND, message);
        return list;
    }

    /**
     * 插入新数据。
     *
     * @param mapper Mapper
     * @param entity 实体
     * @param <E>    实体类型
     * @return 插入后实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> E insert(BaseMapper<E> mapper, E entity) {
        mapper.insert(entity);
        return entity;
    }

    /**
     * 批量插入新数据。
     *
     * @param mapper   Mapper
     * @param entities 实体集合
     * @param <E>      实体类型
     * @return 插入后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> List<E> insertAll(BaseMapper<E> mapper, Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        Db.saveBatch(entities);
        return List.copyOf(entities);
    }

    /**
     * 按主键更新。
     *
     * @param mapper Mapper
     * @param entity 实体（须带主键）
     * @param <E>    实体类型
     * @return 更新后实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> E update(BaseMapper<E> mapper, E entity) {
        mapper.updateById(entity);
        return entity;
    }

    /**
     * 批量按主键更新。
     *
     * @param mapper   Mapper
     * @param entities 实体集合
     * @param <E>      实体类型
     * @return 更新后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> List<E> updateAll(BaseMapper<E> mapper, Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        Db.updateBatchById(entities);
        return List.copyOf(entities);
    }

    /**
     * 保存（无主键插入，有主键更新）。
     *
     * @param mapper Mapper
     * @param entity 实体
     * @param <E>    实体类型
     * @return 保存后实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> E save(BaseMapper<E> mapper, E entity) {
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    /**
     * 批量保存。
     *
     * @param mapper   Mapper
     * @param entities 实体集合
     * @param <E>      实体类型
     * @return 保存后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E extends BaseEntity> List<E> saveAll(BaseMapper<E> mapper, Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<E> toInsert = entities.stream().filter(e -> e.getId() == null).toList();
        List<E> toUpdate = entities.stream().filter(e -> e.getId() != null).toList();
        if (!toInsert.isEmpty()) {
            Db.saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            Db.updateBatchById(toUpdate);
        }
        return List.copyOf(entities);
    }

    /**
     * 按主键物理删除。
     *
     * @param mapper Mapper
     * @param id     主键
     * @param <E>    实体类型
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E> void deleteById(BaseMapper<E> mapper, Long id) {
        mapper.deleteById(id);
    }

    /**
     * 按主键集合批量物理删除。
     *
     * @param mapper Mapper
     * @param ids    主键集合
     * @param <E>    实体类型
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <E> void deleteByIds(BaseMapper<E> mapper, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> idList = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (!idList.isEmpty()) {
            mapper.deleteBatchIds(idList);
        }
    }

    /**
     * 判断主键是否存在。
     *
     * @param mapper Mapper
     * @param id     主键
     * @param <E>    实体类型
     * @return true 表示存在
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    protected <E> boolean existsById(BaseMapper<E> mapper, Long id) {
        return mapper.selectById(id) != null;
    }
}
