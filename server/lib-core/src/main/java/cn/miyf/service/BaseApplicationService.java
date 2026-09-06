package cn.miyf.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.AbstractCondition;
import cn.miyf.repository.BaseRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 应用层通用基类：分页规范化、存在性校验，以及仓储单表 CRUD / 批量操作封装。
 * <p>
 * 业务编排放在具体 Service；数据访问只依赖 Repository 接口，不直接依赖 Mapper。
 * 单表按 ID 删除/修改、新数据保存请优先调用本类方法，避免各业务重复编写。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public abstract class BaseApplicationService {

    /** 默认页码（从 1 开始） */
    protected static final long DEFAULT_PAGE = 1L;
    /** 默认每页条数 */
    protected static final long DEFAULT_PAGE_SIZE = 20L;
    /** 单页最大条数，防止过大分页拖垮查询 */
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
     * @param repository 仓储
     * @param id         主键
     * @param message    不存在提示
     * @param <T>        领域类型
     * @return 领域对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected <T> T requireById(BaseRepository<T, Long> repository, Long id, String message) {
        return requireFound(repository.findById(id), message);
    }

    /**
     * 按主键集合加载；任一条不存在则抛出。
     *
     * @param repository 仓储
     * @param ids        主键集合
     * @param message    不存在提示
     * @param <T>        领域类型
     * @return 领域对象列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> List<T> requireByIds(BaseRepository<T, Long> repository, Collection<Long> ids, String message) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> list = repository.findByIds(ids);
        requireTrue(list.size() == ids.size(), ErrorCode.NOT_FOUND, message);
        return list;
    }

    /**
     * 插入新数据。
     *
     * @param repository 仓储
     * @param domain     领域对象
     * @param <T>        领域类型
     * @return 插入后对象
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> T insert(BaseRepository<T, Long> repository, T domain) {
        return repository.insert(domain);
    }

    /**
     * 批量插入新数据。
     *
     * @param repository 仓储
     * @param domains    领域对象集合
     * @param <T>        领域类型
     * @return 插入后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> List<T> insertAll(BaseRepository<T, Long> repository, Collection<T> domains) {
        return repository.insertAll(domains);
    }

    /**
     * 按主键更新。
     *
     * @param repository 仓储
     * @param domain     领域对象（须带主键）
     * @param <T>        领域类型
     * @return 更新后对象
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> T update(BaseRepository<T, Long> repository, T domain) {
        return repository.update(domain);
    }

    /**
     * 批量按主键更新。
     *
     * @param repository 仓储
     * @param domains    领域对象集合
     * @param <T>        领域类型
     * @return 更新后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> List<T> updateAll(BaseRepository<T, Long> repository, Collection<T> domains) {
        return repository.updateAll(domains);
    }

    /**
     * 保存（无主键插入，有主键更新）。
     *
     * @param repository 仓储
     * @param domain     领域对象
     * @param <T>        领域类型
     * @return 保存后对象
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> T save(BaseRepository<T, Long> repository, T domain) {
        return repository.save(domain);
    }

    /**
     * 批量保存。
     *
     * @param repository 仓储
     * @param domains    领域对象集合
     * @param <T>        领域类型
     * @return 保存后列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> List<T> saveAll(BaseRepository<T, Long> repository, Collection<T> domains) {
        return repository.saveAll(domains);
    }

    /**
     * 按主键物理删除。
     *
     * @param repository 仓储
     * @param id         主键
     * @param <T>        领域类型
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> void deleteById(BaseRepository<T, Long> repository, Long id) {
        repository.deleteById(id);
    }

    /**
     * 按主键集合批量物理删除。
     *
     * @param repository 仓储
     * @param ids        主键集合
     * @param <T>        领域类型
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected <T> void deleteByIds(BaseRepository<T, Long> repository, Collection<Long> ids) {
        repository.deleteByIds(ids);
    }
}
