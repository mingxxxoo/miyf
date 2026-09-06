import { useCallback, useEffect, useState } from 'react';

import { Button, Input, Select, Space, Tag, message } from 'antd';

import { PlusOutlined, EditOutlined } from '@ant-design/icons';

import { useNavigate } from 'react-router-dom';

import SearchForm from '@/components/SearchForm';

import PageTable from '@/components/PageTable';

import StarRating from '@/components/StarRating';

import { confirmDialog } from '@/components/ConfirmDialog';

import { dishApi } from '@/api';

import type { Dish } from '@/types';



const statusMap: Record<string, { color: string; text: string }> = {

  ON_SALE: { color: 'success', text: '已上架' },

  OFF_SALE: { color: 'default', text: '已下架' },

  DRAFT: { color: 'processing', text: '草稿' },

};



export default function DishesPage() {

  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);

  const [data, setData] = useState<Dish[]>([]);

  const [total, setTotal] = useState(0);

  const [page, setPage] = useState(1);

  const [pageSize, setPageSize] = useState(10);

  const [filters, setFilters] = useState<{ keyword?: string; status?: string }>({});



  const fetchData = useCallback(async () => {

    setLoading(true);

    try {

      const res = await dishApi.page({

        page,

        pageSize,

        keyword: filters.keyword,

        status: filters.status,

      });

      setData(res.records ?? []);

      setTotal(res.total ?? 0);

    } catch (err) {

      setData([]);

      setTotal(0);

      message.error(err instanceof Error ? err.message : '加载菜品失败');

    } finally {

      setLoading(false);

    }

  }, [page, pageSize, filters]);



  useEffect(() => {

    void fetchData();

  }, [fetchData]);



  const toggleShelf = (record: Dish) => {

    const isOnSale = record.status === 'ON_SALE';

    confirmDialog({

      title: isOnSale ? '下架菜品' : '上架菜品',

      content: isOnSale

        ? `确定下架「${record.name}」吗？用户端将不再展示。`

        : `确定上架「${record.name}」吗？`,

      onOk: async () => {

        try {

          if (isOnSale) await dishApi.unpublish(record.id);

          else await dishApi.publish(record.id);

          message.success(isOnSale ? '已下架' : '已上架');

          void fetchData();

        } catch (err) {

          message.error(err instanceof Error ? err.message : '操作失败');

        }

      },

    });

  };



  return (

    <div className="ck-page">

      <h2 className="ck-page-title">菜品管理</h2>

      <SearchForm

        fields={[

          {

            name: 'keyword',

            label: '菜品名称',

            element: <Input placeholder="请输入菜品名称" allowClear />,

          },

          {

            name: 'status',

            label: '状态',

            element: (

              <Select

                allowClear

                placeholder="全部状态"

                options={[

                  { value: 'ON_SALE', label: '已上架' },

                  { value: 'OFF_SALE', label: '已下架' },

                  { value: 'DRAFT', label: '草稿' },

                ]}

              />

            ),

          },

        ]}

        loading={loading}

        onSearch={(values) => {

          setFilters({

            keyword: values.keyword ? String(values.keyword) : undefined,

            status: values.status ? String(values.status) : undefined,

          });

          setPage(1);

        }}

      />

      <PageTable<Dish>

        title="菜品列表"

        loading={loading}

        rowKey="id"

        extra={

          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/kitchen/dishes/create')}>

            新增菜品

          </Button>

        }

        columns={[

          { title: '名称', dataIndex: 'name' },

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

            render: (status: string) => {

              const meta = statusMap[status] ?? { color: 'default', text: status };

              return <Tag color={meta.color}>{meta.text}</Tag>;

            },

          },

          {

            title: '操作',

            key: 'action',

            render: (_, record) => (

              <Space>

                <Button

                  type="link"

                  icon={<EditOutlined />}

                  onClick={() => navigate(`/kitchen/dishes/${record.id}/edit`)}

                >

                  编辑

                </Button>

                <Button type="link" onClick={() => toggleShelf(record)}>

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

          onChange: (p, ps) => {

            setPage(p);

            setPageSize(ps);

          },

        }}

        locale={{ emptyText: '菜单还是空的，去发布一道菜吧' }}

      />

    </div>

  );

}


