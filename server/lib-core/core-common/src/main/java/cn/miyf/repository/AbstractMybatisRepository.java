package cn.miyf.repository;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.springframework.core.ResolvableType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus {@link BaseMapper} 的单表 CRUD 基类。
 * <p>
 * 批量写入/更新通过 {@link Db#saveBatch}/{@link Db#updateBatchById}（JDBC batch）执行；
 * 批量删除使用 {@code deleteBatchIds}（单条 SQL {@code IN (...)}）。
 * 禁止在此使用 Wrapper 拼装复杂条件；复杂 SQL 必须落在 Mapper XML。
 *
 * @param <D> 领域模型
 * @param <E> 持久化实体（继承 {@link BaseEntity}）
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
public abstract class AbstractMybatisRepository<D, E extends BaseEntity> implements BaseRepository<D, Long> {

    /**
     * JDBC batch 默认分片大小
     */
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    protected final BaseMapper<E> mapper;
    private final Function<E, D> toDomain;
    private final Function<D, E> toEntity;
    private final Class<E> entityClass;

    /**
     * 构造通用仓储。
     *
     * @param mapper   MyBatis Mapper
     * @param toDomain 实体转领域
     * @param toEntity 领域转实体
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @SuppressWarnings("unchecked")
    protected AbstractMybatisRepository(BaseMapper<E> mapper, Function<E, D> toDomain, Function<D, E> toEntity) {
        this.mapper = mapper;
        this.toDomain = toDomain;
        this.toEntity = toEntity;
        Class<?> resolved = ResolvableType.forClass(getClass())
                .as(AbstractMybatisRepository.class)
                .getGeneric(1)
                .resolve();
        if (resolved == null) {
            throw new IllegalStateException("无法解析实体泛型: " + getClass().getName());
        }
        this.entityClass = (Class<E>) resolved;
    }

    /**
     * 实体转领域（空安全）。
     *
     * @param entity 实体
     * @return 领域对象，entity 为 null 时返回 null
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected D toDomain(E entity) {
        return entity == null ? null : toDomain.apply(entity);
    }

    /**
     * 领域转实体。
     *
     * @param domain 领域对象
     * @return 实体
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected E toEntity(D domain) {
        return toEntity.apply(domain);
    }

    /**
     * 当前实体 Class（供批量 SQL Statement 解析）。
     *
     * @return 实体类型
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected Class<E> entityClass() {
        return entityClass;
    }

    /**
     * 批量操作分片大小，子类可覆盖。
     *
     * @return batch size
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    protected int batchSize() {
        return DEFAULT_BATCH_SIZE;
    }

    /**
     * 读取领域对象主键。
     *
     * @param domain 领域对象
     * @return 主键，新建对象可为 null
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected abstract Long getDomainId(D domain);

    /**
     * 回写生成的主键到领域对象。
     *
     * @param domain 领域对象
     * @param id     主键
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected abstract void setDomainId(D domain, Long id);

    /**
     * 回写时间戳到领域对象。
     *
     * @param domain    领域对象
     * @param createTime 创建时间
     * @param lastModifyTime 更新时间
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected abstract void setDomainTimestamps(D domain, Instant createTime, Instant lastModifyTime);

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public Optional<D> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public List<D> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectBatchIds(ids).stream().map(this::toDomain).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public D insert(D domain) {
        Instant now = Instant.now();
        E entity = toEntity(domain);
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        beforeInsert(entity, domain);
        mapper.insert(entity);
        setDomainId(domain, entity.getId());
        setDomainTimestamps(domain, entity.getCreateTime(), entity.getLastModifyTime());
        return domain;
    }

    /**
     * {@inheritDoc}
     * <p>
     * JDBC batch：{@link Db#saveBatch}，非逐条独立往返。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<D> insertAll(Collection<D> domains) {
        List<D> domainList = new ArrayList<>();
        List<E> entities = prepareInsertEntities(domains, domainList);
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        Db.saveBatch(entities, batchSize());
        syncDomainAfterWrite(domainList, entities);
        return domainList;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public D update(D domain) {
        Instant now = Instant.now();
        E entity = toEntity(domain);
        entity.setLastModifyTime(now);
        beforeUpdate(entity, domain);
        mapper.updateById(entity);
        setDomainTimestamps(domain, entity.getCreateTime(), entity.getLastModifyTime());
        return domain;
    }

    /**
     * {@inheritDoc}
     * <p>
     * JDBC batch：{@link Db#updateBatchById}，非逐条独立往返。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<D> updateAll(Collection<D> domains) {
        List<D> domainList = new ArrayList<>();
        List<E> entities = prepareUpdateEntities(domains, domainList);
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        Db.updateBatchById(entities, batchSize());
        syncDomainAfterWrite(domainList, entities);
        return domainList;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public D save(D domain) {
        Long id = getDomainId(domain);
        if (id == null) {
            return insert(domain);
        }
        return update(domain);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 按有无主键拆成插入组与更新组，再分别走 JDBC batch。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<D> saveAll(Collection<D> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<D> toInsert = new ArrayList<>();
        List<D> toUpdate = new ArrayList<>();
        for (D domain : domains) {
            if (domain == null) {
                continue;
            }
            if (getDomainId(domain) == null) {
                toInsert.add(domain);
            } else {
                toUpdate.add(domain);
            }
        }
        List<D> result = new ArrayList<>(toInsert.size() + toUpdate.size());
        if (!toInsert.isEmpty()) {
            result.addAll(insertAll(toInsert));
        }
        if (!toUpdate.isEmpty()) {
            result.addAll(updateAll(toUpdate));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteById(id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 单条 SQL：{@code DELETE ... WHERE id IN (...)}。
     * 若子类删除需级联清理，请覆盖本方法（先清理再 {@code deleteBatchIds}）。
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> idList = ids.stream().filter(id -> id != null).distinct().toList();
        if (idList.isEmpty()) {
            return;
        }
        mapper.deleteBatchIds(idList);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public boolean existsById(Long id) {
        return id != null && mapper.selectById(id) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    @Override
    public long countAll() {
        return mapper.selectCount(Wrappers.emptyWrapper());
    }

    /**
     * 插入前钩子，子类可补充默认值。
     *
     * @param entity 实体
     * @param domain 领域对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected void beforeInsert(E entity, D domain) {
        // 默认无额外处理
    }

    /**
     * 更新前钩子，子类可补充字段保护（如禁止改评分）。
     *
     * @param entity 实体
     * @param domain 领域对象
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    protected void beforeUpdate(E entity, D domain) {
        // 默认无额外处理
    }

    /**
     * 组装待插入实体列表（含 beforeInsert）。
     *
     * @param domains    入参
     * @param domainList 输出：与 entities 对齐的领域列表
     * @return 实体列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private List<E> prepareInsertEntities(Collection<D> domains, List<D> domainList) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        Instant now = Instant.now();
        List<E> entities = new ArrayList<>(domains.size());
        for (D domain : domains) {
            if (domain == null) {
                continue;
            }
            E entity = toEntity(domain);
            entity.setCreateTime(now);
            entity.setLastModifyTime(now);
            beforeInsert(entity, domain);
            domainList.add(domain);
            entities.add(entity);
        }
        return entities;
    }

    /**
     * 组装待更新实体列表（含 beforeUpdate）。
     *
     * @param domains    入参
     * @param domainList 输出：与 entities 对齐的领域列表
     * @return 实体列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private List<E> prepareUpdateEntities(Collection<D> domains, List<D> domainList) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        Instant now = Instant.now();
        List<E> entities = new ArrayList<>(domains.size());
        for (D domain : domains) {
            if (domain == null) {
                continue;
            }
            E entity = toEntity(domain);
            entity.setLastModifyTime(now);
            beforeUpdate(entity, domain);
            domainList.add(domain);
            entities.add(entity);
        }
        return entities;
    }

    /**
     * 批量写库后回写主键与时间戳到领域对象。
     *
     * @param domainList 领域列表
     * @param entities   实体列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    private void syncDomainAfterWrite(List<D> domainList, List<E> entities) {
        for (int i = 0; i < domainList.size(); i++) {
            D domain = domainList.get(i);
            E entity = entities.get(i);
            setDomainId(domain, entity.getId());
            setDomainTimestamps(domain, entity.getCreateTime(), entity.getLastModifyTime());
        }
    }
}
