import { useCallback, useEffect, useState } from 'react';

import { Button, Select, Space, Tag, message } from 'antd';

import SearchForm from '@/components/SearchForm';

import PageTable from '@/components/PageTable';

import StarRating from '@/components/StarRating';

import { confirmDialog } from '@/components/ConfirmDialog';

import { commentApi } from '@/api';

import type { Comment } from '@/types';



export default function CommentsPage() {

  const [loading, setLoading] = useState(false);

  const [data, setData] = useState<Comment[]>([]);

  const [total, setTotal] = useState(0);

  const [page, setPage] = useState(1);

  const [pageSize, setPageSize] = useState(10);

  const [status, setStatus] = useState<string | undefined>();



  const fetchData = useCallback(async () => {

    setLoading(true);

    try {

      const res = await commentApi.page({ page, pageSize, status });

      setData(res.records ?? []);

      setTotal(res.total ?? 0);

    } catch (err) {

      setData([]);

      setTotal(0);

      message.error(err instanceof Error ? err.message : '加载评论失败');

    } finally {

      setLoading(false);

    }

  }, [page, pageSize, status]);



  useEffect(() => {

    void fetchData();

  }, [fetchData]);



  const toggleHide = (record: Comment) => {

    const hidden = record.status === 'HIDDEN' || record.hidden;

    const action = hidden ? '恢复' : '隐藏';

    confirmDialog({

      title: `${action}评论`,

      content: `确定要${action}这条评论吗？评分会随之重算。`,

      onOk: async () => {

        try {

          if (hidden) await commentApi.restore(record.id);

          else await commentApi.hide(record.id);

          message.success(`已${action}`);

          void fetchData();

        } catch (err) {

          message.error(err instanceof Error ? err.message : '操作失败');

        }

      },

    });

  };



  const handleDelete = (record: Comment) => {

    confirmDialog({

      title: '删除评论',

      content: '删除后不可恢复，评分会重算。确定吗？',

      danger: true,

      onOk: async () => {

        try {

          await commentApi.remove(record.id);

          message.success('已删除');

          void fetchData();

        } catch (err) {

          message.error(err instanceof Error ? err.message : '删除失败');

        }

      },

    });

  };



  return (

    <div className="ck-page">

      <h2 className="ck-page-title">评论管理</h2>

      <SearchForm

        fields={[

          {

            name: 'status',

            label: '状态',

            element: (

              <Select

                allowClear

                placeholder="全部"

                options={[

                  { value: 'NORMAL', label: '显示中' },

                  { value: 'HIDDEN', label: '已隐藏' },

                ]}

              />

            ),

          },

        ]}

        loading={loading}

        onSearch={(values) => {

          setStatus(values.status ? String(values.status) : undefined);

          setPage(1);

        }}

      />

      <PageTable<Comment>

        title="评论列表"

        loading={loading}

        rowKey="id"

        columns={[

          { title: '用户', dataIndex: 'userNickname', render: (v?: string) => v || '厨房朋友' },

          { title: '菜品', dataIndex: 'dishName', render: (v?: string) => v || '—' },

          {

            title: '评分',

            dataIndex: 'rating',

            render: (v: number) => <StarRating value={v} disabled allowHalf={false} />,

          },

          {

            title: '内容',

            dataIndex: 'content',

            ellipsis: true,

            render: (v?: string) => v || '（只打了分）',

          },

          {

            title: '状态',

            dataIndex: 'status',

            render: (s?: string, r?: Comment) =>

              s === 'HIDDEN' || r?.hidden ? <Tag>已隐藏</Tag> : <Tag color="success">显示</Tag>,

          },

          {

            title: '时间',

            dataIndex: 'createdAt',

            render: (v: string) => String(v || '').replace('T', ' ').slice(0, 16),

          },

          {

            title: '操作',

            key: 'action',

            render: (_, record) => {

              const hidden = record.status === 'HIDDEN' || record.hidden;

              return (

                <Space>

                  <Button type="link" onClick={() => toggleHide(record)}>

                    {hidden ? '恢复' : '隐藏'}

                  </Button>

                  <Button type="link" danger onClick={() => handleDelete(record)}>

                    删除

                  </Button>

                </Space>

              );

            },

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

        locale={{ emptyText: '暂无评论，等第一位食客打分吧' }}

      />

    </div>

  );

}


