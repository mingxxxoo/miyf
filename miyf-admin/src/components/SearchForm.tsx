import { Form, Row, Col, Button, Card, Space } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';

export interface SearchField {
  name: string;
  label: string;
  element: React.ReactNode;
  span?: number;
}

interface SearchFormProps {
  fields: SearchField[];
  onSearch: (values: Record<string, unknown>) => void;
  onReset?: () => void;
  loading?: boolean;
}

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
        <Row gutter={16}>
          {fields.map((field) => (
            <Col key={field.name} xs={24} sm={12} md={8} lg={field.span ?? 6}>
              <Form.Item name={field.name} label={field.label}>
                {field.element}
              </Form.Item>
            </Col>
          ))}
          <Col xs={24} sm={12} md={8} lg={6}>
            <Form.Item label=" ">
              <Space>
                <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                  搜索
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Card>
  );
}
