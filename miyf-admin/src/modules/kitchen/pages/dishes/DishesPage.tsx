import { useCallback, useEffect, useState } from 'react';
import { Button, Descriptions, Image, Input, Space, Tabs, message } from 'antd';
import { PlusOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import StarRating from '@/components/StarRating';
import { confirmAction } from '@/ui';
import {
  DetailDrawer,
  EmptyState,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';
import { dishApi, notifyError } from '@/api';
import type { Dish } from '@/types';
import { DISH_STATUS } from '@/constants/status';

type StatusTab = 'ALL' | 'ON_SALE' | 'OFF_SALE' | 'DRAFT';

export default function DishesPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Dish[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState<string | undefined>();
  const [statusTab, setStatusTab] = useState<StatusTab>('ALL');
  const [detail, setDetail] = useState<Dish | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await dishApi.page({
        page,
        pageSize,
        keyword,
        status: statusTab === 'ALL' ? undefined : statusTab,
      });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '加载菜品失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword, statusTab]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const toggleShelf = (record: Dish) => {
    const isOnSale = record.status === 'ON_SALE';
    confirmAction({
      title: isOnSale ? '下架菜品' : '上架菜品',
      content: isOnSale
        ? `确定下架「${record.name}」吗？用户端将不再展示。`
        : `确定上架「${record.name}」吗？`,
      onOk: async () => {
        setActingId(record.id);
        try {
          if (isOnSale) await dishApi.unpublish(record.id);
          else await dishApi.publish(record.id);
          message.success(isOnSale ? '已下架' : '已上架');
          void fetchData();
          if (detail?.id === record.id) {
            setDetail(isOnSale ? { ...record, status: 'OFF_SALE' } : { ...record, status: 'ON_SALE' });
          }
        } catch (err) {
          notifyError(err, '操作失败');
        } finally {
          setActingId(null);
        }
      },
    });
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="菜品管理"
        description="维护封面、库存与上下架，让餐桌选择更清晰"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/kitchen/dishes/create')}>
            新增菜品
          </Button>
        }
      />

      <SearchForm
        fields={[
          {
            name: 'keyword',
            label: '菜品名称',
            element: <Input placeholder="请输入菜品名称" allowClear />,
          },
        ]}
        loading={loading}
        onSearch={(values) => {
          setKeyword(values.keyword ? String(values.keyword).trim() || undefined : undefined);
          setPage(1);
        }}
      />

      <PageToolbar
        left={
          <Tabs
            activeKey={statusTab}
            onChange={(key) => {
              setStatusTab(key as StatusTab);
              setPage(1);
            }}
            items={[
              { key: 'ALL', label: '全部' },
              { key: 'ON_SALE', label: '已上架' },
              { key: 'OFF_SALE', label: '已下架' },
              { key: 'DRAFT', label: '草稿' },
            ]}
            size="small"
            tabBarStyle={{ marginBottom: 0 }}
          />
        }
      />

      <PageTable<Dish>
        title="菜品列表"
        loading={loading}
        rowKey="id"
        columns={[
          {
            title: '封面',
            dataIndex: 'coverImage',
            width: 72,
            render: (url?: string, r?: Dish) =>
              url || r?.coverUrl ? (
                <Image
                  src={url || r?.coverUrl}
                  width={48}
                  height={48}
                  style={{ objectFit: 'cover', borderRadius: 8 }}
                  preview={false}
                />
              ) : (
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: 8,
                    background: 'var(--ck-border)',
                  }}
                />
              ),
          },
          {
            title: '名称',
            dataIndex: 'name',
            render: (name: string, row) => (
              <Button type="link" style={{ padding: 0 }} onClick={() => setDetail(row)}>
                {name}
              </Button>
            ),
          },
          { title: '分类', dataIndex: 'categoryName', render: (v?: string) => v || '—' },
          {
            title: '评分',
            dataIndex: 'rating',
            render: (rating?: number, r?: Dish) =>
              r?.ratingCount && rating != null ? (
                <StarRating value={rating} disabled />
              ) : (
                <span className="ck-muted">暂无评分</span>
              ),
          },
          {
            title: '可提供',
            key: 'stock',
            render: (_, r) =>
              r.stockType === 'UNLIMITED' ? '不限量' : `${r.stock ?? 0} ${r.unit || '份'}`,
          },
          {
            title: '状态',
            dataIndex: 'status',
            render: (status: string) => <StatusBadge code={status} map={DISH_STATUS} />,
          },
          {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => (
              <Space>
                <Button
                  type="link"
                  icon={<EditOutlined />}
                  onClick={() => navigate(`/kitchen/dishes/${record.id}/edit`)}
                >
                  编辑
                </Button>
                <Button
                  type="link"
                  loading={actingId === record.id}
                  onClick={() => toggleShelf(record)}
                >
                  {record.status === 'ON_SALE' ? '下架' : '上架'}
                </Button>
              </Space>
            ),
          },
        ]}
        dataSource={data}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        locale={{
          emptyText: (
            <EmptyState
              description="菜单还是空的，去发布一道菜吧"
              actionText="新增菜品"
              onAction={() => navigate('/kitchen/dishes/create')}
            />
          ),
        }}
      />

      <DetailDrawer
        title={detail?.name || '菜品详情'}
        open={!!detail}
        onClose={() => setDetail(null)}
        extra={
          detail ? (
            <Space>
              <Button onClick={() => navigate(`/kitchen/dishes/${detail.id}/edit`)}>编辑</Button>
              <Button type="primary" onClick={() => toggleShelf(detail)}>
                {detail.status === 'ON_SALE' ? '下架' : '上架'}
              </Button>
            </Space>
          ) : null
        }
      >
        {detail ? (
          <Descriptions column={1} size="small">
            <Descriptions.Item label="封面">
              {(detail.coverImage || detail.coverUrl) && (
                <Image src={detail.coverImage || detail.coverUrl} width={160} style={{ borderRadius: 12 }} />
              )}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <StatusBadge code={detail.status} map={DISH_STATUS} />
            </Descriptions.Item>
            <Descriptions.Item label="分类">{detail.categoryName || '—'}</Descriptions.Item>
            <Descriptions.Item label="副标题">{detail.subtitle || '—'}</Descriptions.Item>
            <Descriptions.Item label="描述">{detail.description || '—'}</Descriptions.Item>
            <Descriptions.Item label="库存">
              {detail.stockType === 'UNLIMITED' ? '不限量' : `${detail.stock ?? 0} ${detail.unit || '份'}`}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </DetailDrawer>
    </div>
  );
}
