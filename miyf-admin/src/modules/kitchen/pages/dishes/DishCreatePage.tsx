import { useEffect, useState } from 'react';

import { useNavigate } from 'react-router-dom';

import { Button, Card, Form, Input, InputNumber, Select, Switch, message, Space } from 'antd';

import ImageUploader from '@/components/ImageUploader';

import { categoryApi, dishApi } from '@/api';

import type { Category } from '@/types';



export default function DishCreatePage() {

  const navigate = useNavigate();

  const [form] = Form.useForm();

  const [categories, setCategories] = useState<Category[]>([]);

  const [submitting, setSubmitting] = useState(false);



  useEffect(() => {

    categoryApi

      .list()

      .then(setCategories)

      .catch(() => setCategories([]));

  }, []);



  const onFinish = async (values: Record<string, unknown>) => {

    setSubmitting(true);

    try {

      await dishApi.create({

        name: String(values.name),

        categoryId: String(values.categoryId),

        subtitle: values.subtitle ? String(values.subtitle) : undefined,

        description: values.description ? String(values.description) : undefined,

        coverImage: values.coverImage ? String(values.coverImage) : undefined,

        coverUrl: values.coverImage ? String(values.coverImage) : undefined,

        stockType: values.stockType as 'LIMITED' | 'UNLIMITED',

        stock: values.stock != null ? Number(values.stock) : undefined,

        unit: values.unit ? String(values.unit) : undefined,

        recommend: Boolean(values.recommend),

        sortOrder: values.sortOrder != null ? Number(values.sortOrder) : 0,

      });

      message.success('菜品已创建（草稿），上架后用户即可预约');

      navigate('/kitchen/dishes');

    } catch (err) {

      message.error(err instanceof Error ? err.message : '创建失败');

    } finally {

      setSubmitting(false);

    }

  };



  return (

    <div className="ck-page">

      <h2 className="ck-page-title">新增菜品</h2>

      <Card bordered={false}>

        <Form

          form={form}

          layout="vertical"

          style={{ maxWidth: 640 }}

          initialValues={{

            stockType: 'UNLIMITED',

            recommend: false,

            stock: 0,

            sortOrder: 0,

          }}

          onFinish={onFinish}

        >

          <Form.Item name="name" label="菜品名称" rules={[{ required: true, message: '请输入名称' }]}>

            <Input placeholder="例如：番茄炒蛋" />

          </Form.Item>

          <Form.Item name="subtitle" label="副标题">

            <Input placeholder="一句话亮点" />

          </Form.Item>

          <Form.Item

            name="categoryId"

            label="分类"

            rules={[{ required: true, message: '请选择分类' }]}

          >

            <Select

              placeholder="选择分类"

              options={categories

                .filter((c) => c.status === 'ENABLED')

                .map((c) => ({ value: c.id, label: c.name }))}

              notFoundContent="暂无分类"

            />

          </Form.Item>

          <Form.Item name="description" label="简介">

            <Input.TextArea rows={3} placeholder="用一两句话介绍这道菜" />

          </Form.Item>

          <Form.Item name="coverImage" label="封面图">

            <ImageUploader />

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

                <Form.Item name="stock" label="可提供份数" rules={[{ required: true }]}>

                  <InputNumber min={0} style={{ width: '100%' }} />

                </Form.Item>

              ) : null

            }

          </Form.Item>

          <Form.Item name="unit" label="单位">

            <Input placeholder="份 / 盘" />

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

                保存草稿

              </Button>

              <Button onClick={() => navigate('/kitchen/dishes')}>取消</Button>

            </Space>

          </Form.Item>

        </Form>

      </Card>

    </div>

  );

}


