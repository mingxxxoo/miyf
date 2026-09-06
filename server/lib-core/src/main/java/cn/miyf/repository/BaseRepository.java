package cn.miyf.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 通用仓储接口：单表 CRUD（含批量按 ID 删除、批量保存/更新）。
 * <p>
 * 复杂查询（联表、动态条件分页、聚合、库存原子更新等）
 * 必须由具体 Repository 配合 Mapper XML / 手写 SQL 实现，禁止用 MyBatis-Plus Wrapper 拼装。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public interface BaseRepository<T, ID> {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 领域对象，不存在则为空
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    Optional<T> findById(ID id);

    /**
     * 按主键集合批量查询。
     *
     * @param ids 主键集合
     * @return 领域对象列表
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    List<T> findByIds(Collection<ID> ids);

    /**
     * 插入新记录。
     *
     * @param entity 领域对象
     * @return 插入后的领域对象（含生成的主键与时间戳）
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    T insert(T entity);

    /**
     * 批量插入新记录（JDBC batch，非逐条独立往返）。
     *
     * @param entities 领域对象集合
     * @return 插入后的列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<T> insertAll(Collection<T> entities);

    /**
     * 按主键更新。
     *
     * @param entity 领域对象
     * @return 更新后的领域对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    T update(T entity);

    /**
     * 批量按主键更新（JDBC batch，非逐条独立往返）。
     *
     * @param entities 领域对象集合（须带主键）
     * @return 更新后的列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<T> updateAll(Collection<T> entities);

    /**
     * 保存：无主键则插入，有主键则更新。
     *
     * @param entity 领域对象
     * @return 保存后的领域对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    T save(T entity);

    /**
     * 批量保存：按有无主键拆分后分别 JDBC batch 插入/更新。
     *
     * @param entities 领域对象集合
     * @return 保存后的列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<T> saveAll(Collection<T> entities);

    /**
     * 按主键物理删除。
     *
     * @param id 主键
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    void deleteById(ID id);

    /**
     * 按主键集合批量物理删除（单条 SQL {@code IN (...)}）。
     *
     * @param ids 主键集合
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    void deleteByIds(Collection<ID> ids);

    /**
     * 判断主键是否存在。
     *
     * @param id 主键
     * @return true 表示存在
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    boolean existsById(ID id);

    /**
     * 统计全部记录数。
     *
     * @return 总数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    long countAll();
}
