import { useEffect, useState } from 'react';

import { useNavigate, useParams } from 'react-router-dom';

import { Button, Card, Form, Input, InputNumber, Select, Switch, Spin, Empty, message, Space } from 'antd';

import ImageUploader from '@/components/ImageUploader';

import { categoryApi, dishApi } from '@/api';

import type { Category, Dish } from '@/types';
import { PageHeader } from '@/ui';



export default function DishEditPage() {

  const { id } = useParams<{ id: string }>();

  const navigate = useNavigate();

  const [form] = Form.useForm();

  const [categories, setCategories] = useState<Category[]>([]);

  const [loading, setLoading] = useState(true);

  const [submitting, setSubmitting] = useState(false);

  const [found, setFound] = useState(true);

  const [dish, setDish] = useState<Dish | null>(null);



  useEffect(() => {

    categoryApi

      .list()

      .then(setCategories)

      .catch(() => setCategories([]));

  }, []);



  useEffect(() => {

    if (!id) return;

    let cancelled = false;

    (async () => {

      setLoading(true);

      try {

        const data = await dishApi.detail(id);

        if (!cancelled) {

          setDish(data);

          form.setFieldsValue({

            ...data,

            coverImage: data.coverImage || data.coverUrl,

            recommend: data.recommend ?? data.recommended,

          });

          setFound(true);

        }

      } catch {

        if (!cancelled) setFound(false);

      } finally {

        if (!cancelled) setLoading(false);

      }

    })();

    return () => {

      cancelled = true;

    };

  }, [id, form]);



  const onFinish = async (values: Record<string, unknown>) => {

    if (!id) return;

    setSubmitting(true);

    try {

      await dishApi.update(id, {

        name: String(values.name),

        categoryId: String(values.categoryId),

        subtitle: values.subtitle ? String(values.subtitle) : undefined,

        description: values.description ? String(values.description) : undefined,

        coverImage: values.coverImage ? String(values.coverImage) : undefined,

        stockType: values.stockType as 'LIMITED' | 'UNLIMITED',

        stock: values.stock != null ? Number(values.stock) : undefined,

        unit: values.unit ? String(values.unit) : undefined,

        recommend: Boolean(values.recommend),

        sortOrder: values.sortOrder != null ? Number(values.sortOrder) : 0,

      });

      message.success('菜品已更新');

      navigate('/kitchen/dishes');

    } catch (err) {

      message.error(err instanceof Error ? err.message : '保存失败');

    } finally {

      setSubmitting(false);

    }

  };



  const handlePublish = async () => {

    if (!id) return;

    try {

      const updated = await dishApi.publish(id);

      setDish(updated);

      message.success('已上架');

    } catch (err) {

      message.error(err instanceof Error ? err.message : '上架失败');

    }

  };



  const handleUnpublish = async () => {

    if (!id) return;

    try {

      const updated = await dishApi.unpublish(id);

      setDish(updated);

      message.success('已下架');

    } catch (err) {

      message.error(err instanceof Error ? err.message : '下架失败');

    }

  };



  if (!loading && !found) {

    return (

      <div className="ck-page">

        <Empty description="未找到该菜品" />

        <Button onClick={() => navigate('/kitchen/dishes')} style={{ marginTop: 16 }}>

          返回列表

        </Button>

      </div>

    );

  }



  return (

    <div className="ck-page">

      <PageHeader title="编辑菜品" />
<Spin spinning={loading}>

        <Card bordered={false}>

          {dish && (

            <Space style={{ marginBottom: 16 }}>

              <span className="ck-muted">当前状态：{dish.status}</span>

              {dish.status !== 'ON_SALE' ? (

                <Button type="primary" onClick={() => void handlePublish()}>

                  上架

                </Button>

              ) : (

                <Button onClick={() => void handleUnpublish()}>下架</Button>

              )}

            </Space>

          )}

          <Form form={form} layout="vertical" style={{ maxWidth: 640 }} onFinish={onFinish}>

            <Form.Item name="name" label="菜品名称" rules={[{ required: true, message: '请输入名称' }]}>

              <Input />

            </Form.Item>

            <Form.Item name="subtitle" label="副标题">

              <Input />

            </Form.Item>

            <Form.Item

              name="categoryId"

              label="分类"

              rules={[{ required: true, message: '请选择分类' }]}

            >

              <Select options={categories.map((c) => ({ value: c.id, label: c.name }))} />

            </Form.Item>

            <Form.Item name="description" label="简介">

              <Input.TextArea rows={3} />

            </Form.Item>

            <Form.Item name="coverImage" label="封面图">

              <ImageUploader appCode="kitchen" source="dish-cover" />

            </Form.Item>

            <Form.Item name="stockType" label="可提供方式">

              <Select

                options={[

                  { value: 'UNLIMITED', label: '不限量' },

                  { value: 'LIMITED', label: '限量' },

                ]}

              />

            </Form.Item>

            <Form.Item noStyle shouldUpdate={(prev, cur) => prev.stockType !== cur.stockType}>

              {() =>

                form.getFieldValue('stockType') === 'LIMITED' ? (

                  <Form.Item name="stock" label="可提供份数">

                    <InputNumber min={0} style={{ width: '100%' }} />

                  </Form.Item>

                ) : null

              }

            </Form.Item>

            <Form.Item name="unit" label="单位">

              <Input />

            </Form.Item>

            <Form.Item name="sortOrder" label="排序">

              <InputNumber min={0} style={{ width: '100%' }} />

            </Form.Item>

            <Form.Item name="recommend" label="今日推荐" valuePropName="checked">

              <Switch />

            </Form.Item>

            <Form.Item>

              <Space>

                <Button type="primary" htmlType="submit" loading={submitting}>

                  保存

                </Button>

                <Button onClick={() => navigate('/kitchen/dishes')}>取消</Button>

              </Space>

            </Form.Item>

          </Form>

        </Card>

      </Spin>

    </div>

  );

}


