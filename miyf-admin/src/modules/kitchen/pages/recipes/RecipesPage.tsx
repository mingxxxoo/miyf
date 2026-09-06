import { useCallback, useEffect, useState } from 'react';

import { Button, Form, Input, InputNumber, Modal, Select, Space, message } from 'antd';

import PageTable from '@/components/PageTable';

import { confirmDialog } from '@/components/ConfirmDialog';

import { dishApi, recipeApi } from '@/api';

import type { Dish, DishRecipe } from '@/types';



export default function RecipesPage() {

  const [loading, setLoading] = useState(false);

  const [data, setData] = useState<DishRecipe[]>([]);

  const [total, setTotal] = useState(0);

  const [page, setPage] = useState(1);

  const [pageSize, setPageSize] = useState(10);

  const [dishes, setDishes] = useState<Dish[]>([]);

  const [open, setOpen] = useState(false);

  const [editing, setEditing] = useState<DishRecipe | null>(null);

  const [form] = Form.useForm();



  const fetchData = useCallback(async () => {

    setLoading(true);

    try {

      const res = await recipeApi.page({ page, pageSize });

      setData(res.records ?? []);

      setTotal(res.total ?? 0);

    } catch (err) {

      setData([]);

      setTotal(0);

      message.error(err instanceof Error ? err.message : '加载菜谱失败');

    } finally {

      setLoading(false);

    }

  }, [page, pageSize]);



  useEffect(() => {

    void fetchData();

  }, [fetchData]);



  useEffect(() => {

    dishApi

      .page({ page: 1, pageSize: 200 })

      .then((res) => setDishes(res.records || []))

      .catch(() => setDishes([]));

  }, []);



  const openCreate = () => {

    setEditing(null);

    form.resetFields();

    form.setFieldsValue({

      difficulty: 'EASY',

      ingredientsText: '',

      seasoningsText: '',

      stepsText: '',

    });

    setOpen(true);

  };



  const openEdit = (record: DishRecipe) => {

    setEditing(record);

    form.setFieldsValue({

      dishId: record.dishId,

      description: record.description,

      difficulty: record.difficulty || 'EASY',

      prepareMinutes: record.prepareMinutes,

      cookMinutes: record.cookMinutes,

      servings: record.servings,

      tips: record.tips,

      ingredientsText: (record.ingredients || [])

        .map((i) => (i.amount ? `${i.name} ${i.amount}` : i.name))

        .join('\n'),

      seasoningsText: (record.seasonings || [])

        .map((i) => (i.amount ? `${i.name} ${i.amount}` : i.name))

        .join('\n'),

      stepsText: (record.steps || []).map((s) => s.content || s.description || '').join('\n'),

    });

    setOpen(true);

  };



  const parseLines = (text: string) =>

    String(text || '')

      .split('\n')

      .map((l) => l.trim())

      .filter(Boolean)

      .map((line) => {

        const [name, ...rest] = line.split(/\s+/);

        return { name, amount: rest.length ? rest.join(' ') : undefined };

      });



  const handleSubmit = async () => {

    const values = await form.validateFields();

    const ingredients = parseLines(values.ingredientsText);

    const seasonings = parseLines(values.seasoningsText);

    const steps = String(values.stepsText || '')

      .split('\n')

      .map((l) => l.trim())

      .filter(Boolean)

      .map((content, i) => ({ step: i + 1, content }));



    const payload: Partial<DishRecipe> = {

      dishId: String(values.dishId),

      description: values.description,

      difficulty: values.difficulty,

      prepareMinutes: values.prepareMinutes,

      cookMinutes: values.cookMinutes,

      servings: values.servings,

      tips: values.tips,

      ingredients,

      seasonings,

      steps,

    };



    try {

      if (editing?.id) {

        await recipeApi.update(editing.id, payload);

        message.success('菜谱已更新');

      } else {

        await recipeApi.create(payload);

        message.success('菜谱已创建');

      }

      setOpen(false);

      void fetchData();

    } catch (err) {

      message.error(err instanceof Error ? err.message : '保存失败');

    }

  };



  const handleDelete = (record: DishRecipe) => {

    if (!record.id) return;

    confirmDialog({

      title: '删除菜谱',

      content: '确定删除这份菜谱吗？',

      danger: true,

      onOk: async () => {

        try {

          await recipeApi.remove(record.id!);

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

      <h2 className="ck-page-title">食谱管理</h2>

      <PageTable<DishRecipe>

        title="食谱列表"

        loading={loading}

        rowKey={(r) => r.id || r.dishId}

        extra={

          <Button type="primary" onClick={openCreate}>

            新增菜谱

          </Button>

        }

        columns={[

          {

            title: '菜品',

            dataIndex: 'dishName',

            render: (v?: string, r?: DishRecipe) =>

              v || dishes.find((d) => d.id === r?.dishId)?.name || '—',

          },

          { title: '难度', dataIndex: 'difficulty', render: (v?: string) => v || '—' },

          {

            title: '食材数',

            key: 'ingredients',

            render: (_, r) => r.ingredients?.length ?? 0,

          },

          {

            title: '步骤数',

            key: 'steps',

            render: (_, r) => r.steps?.length ?? 0,

          },

          {

            title: '操作',

            key: 'action',

            render: (_, record) => (

              <Space>

                <Button type="link" onClick={() => openEdit(record)}>

                  编辑

                </Button>

                <Button type="link" danger onClick={() => handleDelete(record)}>

                  删除

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

        locale={{ emptyText: '还没有食谱，给菜品补上做法吧' }}

      />



      <Modal

        title={editing ? '编辑菜谱' : '新增菜谱'}

        open={open}

        onOk={() => void handleSubmit()}

        onCancel={() => setOpen(false)}

        width={720}

        destroyOnClose

      >

        <Form form={form} layout="vertical">

          <Form.Item name="dishId" label="菜品" rules={[{ required: true, message: '请选择菜品' }]}>

            <Select

              showSearch

              optionFilterProp="label"

              options={dishes.map((d) => ({ value: d.id, label: d.name }))}

              disabled={Boolean(editing)}

            />

          </Form.Item>

          <Form.Item name="description" label="描述">

            <Input.TextArea rows={2} />

          </Form.Item>

          <Form.Item name="difficulty" label="难度">

            <Select

              options={[

                { value: 'EASY', label: '简单' },

                { value: 'MEDIUM', label: '中等' },

                { value: 'HARD', label: '较难' },

              ]}

            />

          </Form.Item>

          <Space style={{ display: 'flex' }} size="middle">

            <Form.Item name="prepareMinutes" label="准备(分)">

              <InputNumber min={0} />

            </Form.Item>

            <Form.Item name="cookMinutes" label="烹饪(分)">

              <InputNumber min={0} />

            </Form.Item>

            <Form.Item name="servings" label="份量">

              <InputNumber min={1} />

            </Form.Item>

          </Space>

          <Form.Item

            name="ingredientsText"

            label="食材（每行：名称 用量）"

            rules={[{ required: true, message: '请填写食材' }]}

          >

            <Input.TextArea rows={4} placeholder={'鸡蛋 2个\n番茄 2个'} />

          </Form.Item>

          <Form.Item name="seasoningsText" label="调味（每行：名称 用量）">

            <Input.TextArea rows={3} placeholder={'盐 少许'} />

          </Form.Item>

          <Form.Item

            name="stepsText"

            label="步骤（每行一步）"

            rules={[{ required: true, message: '请填写步骤' }]}

          >

            <Input.TextArea rows={5} placeholder={'打散鸡蛋\n炒至凝固'} />

          </Form.Item>

          <Form.Item name="tips" label="小贴士">

            <Input.TextArea rows={2} />

          </Form.Item>

        </Form>

      </Modal>

    </div>

  );

}


