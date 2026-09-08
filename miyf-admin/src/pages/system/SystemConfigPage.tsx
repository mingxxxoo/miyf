import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Switch,
  Typography,
  message,
} from 'antd';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';
import { sysConfigApi, type SysConfig } from '@/modules/system/api';
import {
  ChangeSummaryModal,
  EmptyState,
  FormItem,
  FormModal,
  LoadingState,
  PageHeader,
  PageToolbar,
  SettingSection,
  type ChangeItem,
} from '@/ui';

const VALUE_TYPES = [
  { value: 'STRING', label: '字符串' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '布尔' },
  { value: 'JSON', label: 'JSON' },
];

const GROUP_LABELS: Record<string, string> = {
  platform: '平台信息',
  business: '业务设置',
  security: '安全设置',
  search: '搜索',
  site: '站点',
  upload: '上传',
};

const GROUP_ORDER = ['platform', 'business', 'search', 'security', 'site', 'upload'];

function groupTitle(code: string) {
  return GROUP_LABELS[code] || code;
}

function isHighRisk(cfg: SysConfig) {
  const key = (cfg.configKey || '').toLowerCase();
  const group = (cfg.groupCode || '').toLowerCase();
  return (
    group.includes('security') ||
    key.includes('password') ||
    key.includes('secret') ||
    key.includes('token')
  );
}

function normalizeValue(v: string | undefined) {
  return v ?? '';
}

/**
 * 基础设置：按分组展示可编辑配置项，保存前摘要确认。
 */
export default function SystemConfigPage() {
  const [loading, setLoading] = useState(false);
  const [configs, setConfigs] = useState<SysConfig[]>([]);
  const [originals, setOriginals] = useState<Record<string, string>>({});
  const [values, setValues] = useState<Record<string, string>>({});
  const [keyword, setKeyword] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [form] = Form.useForm();
  const { submitting, run } = useSubmitting();

  const hydrate = useCallback((list: SysConfig[]) => {
    const nextOrig: Record<string, string> = {};
    const nextVals: Record<string, string> = {};
    for (const item of list) {
      const v = normalizeValue(item.configValue);
      nextOrig[item.id] = v;
      nextVals[item.id] = v;
    }
    setConfigs(list);
    setOriginals(nextOrig);
    setValues(nextVals);
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await sysConfigApi.list({
        keyword: keyword.trim() || undefined,
      });
      hydrate(list);
    } catch (err) {
      hydrate([]);
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, [hydrate, keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const dirtyIds = useMemo(() => {
    return configs
      .filter((c) => normalizeValue(values[c.id]) !== normalizeValue(originals[c.id]))
      .map((c) => c.id);
  }, [configs, values, originals]);

  const dirtyCount = dirtyIds.length;

  const changeItems: ChangeItem[] = useMemo(() => {
    return dirtyIds.map((id) => {
      const cfg = configs.find((c) => c.id === id)!;
      return {
        label: cfg.name || cfg.configKey,
        from: originals[id] || '—',
        to: values[id] || '—',
      };
    });
  }, [dirtyIds, configs, originals, values]);

  const hasHighRiskChange = useMemo(() => {
    return dirtyIds.some((id) => {
      const cfg = configs.find((c) => c.id === id);
      return cfg ? isHighRisk(cfg) : false;
    });
  }, [dirtyIds, configs]);

  const grouped = useMemo(() => {
    const map = new Map<string, SysConfig[]>();
    for (const cfg of configs) {
      const code = cfg.groupCode || 'default';
      const list = map.get(code) ?? [];
      list.push(cfg);
      map.set(code, list);
    }
    for (const list of map.values()) {
      list.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.configKey.localeCompare(b.configKey));
    }
    const keys = Array.from(map.keys()).sort((a, b) => {
      const ia = GROUP_ORDER.indexOf(a);
      const ib = GROUP_ORDER.indexOf(b);
      if (ia >= 0 || ib >= 0) {
        return (ia >= 0 ? ia : 999) - (ib >= 0 ? ib : 999);
      }
      return a.localeCompare(b);
    });
    return keys.map((code) => ({ code, title: groupTitle(code), items: map.get(code)! }));
  }, [configs]);

  const setValue = (id: string, next: string) => {
    setValues((prev) => ({ ...prev, [id]: next }));
  };

  const restoreOne = (id: string) => {
    setValues((prev) => ({ ...prev, [id]: originals[id] ?? '' }));
  };

  const openCreate = () => {
    form.resetFields();
    form.setFieldsValue({
      valueType: 'STRING',
      groupCode: 'platform',
      status: 'ENABLED',
      sortOrder: 0,
    });
    setCreateOpen(true);
  };

  const handleCreate = () =>
    run(async () => {
      const raw = await form.validateFields();
      try {
        await sysConfigApi.create({
          configKey: raw.configKey as string,
          configValue: raw.configValue as string | undefined,
          valueType: raw.valueType as string,
          groupCode: raw.groupCode as string,
          name: raw.name as string,
          description: raw.description as string | undefined,
          status: raw.status as string,
          sortOrder: raw.sortOrder as number | undefined,
        });
        message.success('配置已创建');
        setCreateOpen(false);
        await fetchData();
      } catch (err) {
        notifyError(err, '创建失败');
      }
    });

  const handleDelete = (id: string) =>
    run(async () => {
      try {
        await sysConfigApi.remove(id);
        message.success('已删除');
        await fetchData();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  const requestSave = () => {
    if (!dirtyCount) {
      message.info('没有需要保存的变更');
      return;
    }
    setSummaryOpen(true);
  };

  const confirmSave = () =>
    run(async () => {
      try {
        for (const id of dirtyIds) {
          const cfg = configs.find((c) => c.id === id);
          if (!cfg) continue;
          await sysConfigApi.update(id, {
            configKey: cfg.configKey,
            configValue: values[id],
            valueType: cfg.valueType,
            groupCode: cfg.groupCode,
            name: cfg.name,
            description: cfg.description,
            status: cfg.status,
            sortOrder: cfg.sortOrder,
          });
        }
        message.success(`已保存 ${dirtyIds.length} 项配置`);
        setSummaryOpen(false);
        await fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const renderEditor = (cfg: SysConfig) => {
    const value = values[cfg.id] ?? '';
    const type = (cfg.valueType || 'STRING').toUpperCase();
    if (type === 'BOOLEAN') {
      return (
        <Switch
          checked={value === 'true' || value === '1'}
          onChange={(checked) => setValue(cfg.id, checked ? 'true' : 'false')}
        />
      );
    }
    if (type === 'NUMBER') {
      return (
        <InputNumber
          style={{ width: '100%', maxWidth: 320 }}
          value={value === '' ? null : Number(value)}
          onChange={(n) => setValue(cfg.id, n == null ? '' : String(n))}
        />
      );
    }
    if (type === 'JSON') {
      return (
        <Input.TextArea
          rows={3}
          value={value}
          onChange={(e) => setValue(cfg.id, e.target.value)}
          placeholder="JSON"
        />
      );
    }
    return (
      <Input
        value={value}
        onChange={(e) => setValue(cfg.id, e.target.value)}
        placeholder={cfg.configKey}
      />
    );
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="基础设置"
        description="按分组维护平台、业务与安全配置；保存前可预览变更摘要"
        extra={
          <Space>
            <Button onClick={() => void fetchData()} loading={loading}>
              刷新
            </Button>
            <Button type="primary" disabled={!dirtyCount} onClick={requestSave}>
              保存变更{dirtyCount ? ` (${dirtyCount})` : ''}
            </Button>
          </Space>
        }
      />

      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索键 / 名称"
            style={{ width: 240 }}
            onSearch={setKeyword}
            onChange={(e) => {
              if (!e.target.value) setKeyword('');
            }}
          />
        }
        right={
          <Button onClick={openCreate}>新增配置</Button>
        }
      />

      {loading && !configs.length ? (
        <LoadingState tip="加载配置中…" />
      ) : !configs.length ? (
        <EmptyState description="暂无配置" actionText="新增配置" onAction={openCreate} />
      ) : (
        grouped.map((group) => (
          <SettingSection
            key={group.code}
            title={group.title}
            description={`分组：${group.code}`}
            danger={group.code.toLowerCase().includes('security')}
          >
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {group.items.map((cfg) => {
                const dirty = dirtyIds.includes(cfg.id);
                return (
                  <div key={cfg.id} className="setting-config-row">
                    <div className="setting-config-main">
                      <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                        <div>
                          <Typography.Text strong>{cfg.name || cfg.configKey}</Typography.Text>
                          {dirty ? (
                            <Typography.Text type="warning" style={{ marginLeft: 8 }}>
                              已修改
                            </Typography.Text>
                          ) : null}
                          <div>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {cfg.configKey}
                              {cfg.description ? ` · ${cfg.description}` : ''}
                            </Typography.Text>
                          </div>
                        </div>
                        <Space>
                          <Button
                            type="link"
                            size="small"
                            disabled={!dirty}
                            onClick={() => restoreOne(cfg.id)}
                          >
                            恢复
                          </Button>
                          <Popconfirm
                            title="确认删除该配置？"
                            onConfirm={() => void handleDelete(cfg.id)}
                          >
                            <Button type="link" size="small" danger>
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      </Space>
                      <div style={{ marginTop: 8 }}>{renderEditor(cfg)}</div>
                    </div>
                  </div>
                );
              })}
            </Space>
          </SettingSection>
        ))
      )}

      <ChangeSummaryModal
        open={summaryOpen}
        title={hasHighRiskChange ? '确认高风险配置变更' : '确认保存配置变更'}
        items={changeItems}
        danger={hasHighRiskChange}
        confirmLoading={submitting}
        onOk={() => void confirmSave()}
        onCancel={() => setSummaryOpen(false)}
      />

      <FormModal
        title="新增配置"
        open={createOpen}
        form={form}
        confirmLoading={submitting}
        onOk={() => void handleCreate()}
        onCancel={() => setCreateOpen(false)}
      >
        <FormItem name="configKey" label="配置键" rules={[{ required: true }]}>
          <Input placeholder="如：site.name" />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true }]}>
          <Input />
        </FormItem>
        <FormItem name="valueType" label="值类型" rules={[{ required: true }]}>
          <Select options={VALUE_TYPES} />
        </FormItem>
        <FormItem name="groupCode" label="分组" rules={[{ required: true }]}>
          <Input placeholder="platform / business / security" />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '禁用' },
            ]}
          />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="configValue" label="配置值" full>
          <Input.TextArea rows={3} />
        </FormItem>
        <FormItem name="description" label="说明" full>
          <Input.TextArea rows={2} />
        </FormItem>
      </FormModal>
    </div>
  );
}
