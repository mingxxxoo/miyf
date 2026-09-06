import { Table, Card, type TableProps } from 'antd';



type PageTableProps<T extends object> = Omit<TableProps<T>, 'title'> & {

  title?: string;

  extra?: React.ReactNode;

};



export default function PageTable<T extends object>({

  title,

  extra,

  ...tableProps

}: PageTableProps<T>) {

  return (

    <Card title={title} extra={extra} bordered={false}>

      <Table<T>

        rowKey="id"

        pagination={{ showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}

        {...tableProps}

      />

    </Card>

  );

}


