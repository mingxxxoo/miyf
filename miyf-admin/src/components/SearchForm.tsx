import { Form, Button, Card, Space } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { FormGrid, FormItem } from '@/ui/form';

export interface SearchField {
  name: string;
  label: string;
  element: React.ReactNode;
  /** 占几列（相对 4 列搜索栅格），默认 1。 */
  col?: number;
  /** @deprecated 使用 col；保留兼容旧 span(24 栅格)。 */
  span?: number;
}

interface SearchFormProps {
  fields: SearchField[];
  onSearch: (values: Record<string, unknown>) => void;
  onReset?: () => void;
  loading?: boolean;
}

/**
 * 列表页搜索条：响应式栅格，默认一行最多 4 项。
 */
export default function SearchForm({ fields, onSearch, onReset, loading }: SearchFormProps) {
  const [form] = Form.useForm();

  const handleReset = () => {
    form.resetFields();
    onReset?.();
    onSearch({});
  };

  return (
    <Card bordered={false} style={{ marginBottom: 16 }}>
      <Form form={form} onFinish={onSearch} layout="vertical">
        <FormGrid columns={4}>
          {fields.map((field) => {
            const col =
              field.col ??
              (field.span != null ? Math.max(1, Math.round(field.span / 6)) : 1);
            return (
              <FormItem key={field.name} name={field.name} label={field.label} col={col}>
                {field.element}
              </FormItem>
            );
          })}
          <FormItem label=" ">
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                搜索
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </FormItem>
        </FormGrid>
      </Form>
    </Card>
  );
}
