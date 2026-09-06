import { Card, Col, Row, Typography } from 'antd';
import { Link } from 'react-router-dom';

const tiles = [
  {
    title: '权限与角色',
    desc: '权限树、权限组、角色绑定（系统级）',
    to: '/system/permissions',
  },
  {
    title: '系统配置',
    desc: '平台参数、开关与分组配置项',
    to: '/system/config',
  },
  {
    title: '数据字典',
    desc: '字典类型与字典项维护',
    to: '/system/dicts',
  },
  {
    title: '通知中心',
    desc: '站内信落库、邮件/短信发送',
    to: '/system/notifications',
  },
  {
    title: '定时任务',
    desc: '任务列表、启停与手动触发',
    to: '/system/jobs',
  },
  {
    title: '系统监控',
    desc: 'JVM / 磁盘 / Redis / 数据库状态',
    to: '/system/monitor',
  },
];

/**
 * 系统设置首页：仅系统管理员从头像菜单进入。
 */
export default function SystemHomePage() {
  return (
    <div className="ck-page">
      <h2 className="ck-page-title">系统设置</h2>
      <Typography.Paragraph type="secondary">
        系统级权限、配置、字典、通知、任务与监控。厨房/健康等业务请回到业务菜单操作。
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        {tiles.map((t) => (
          <Col xs={24} md={8} key={t.to}>
            <Link to={t.to} style={{ textDecoration: 'none' }}>
              <Card hoverable>
                <Typography.Title level={4} style={{ marginTop: 0 }}>
                  {t.title}
                </Typography.Title>
                <Typography.Text type="secondary">{t.desc}</Typography.Text>
              </Card>
            </Link>
          </Col>
        ))}
      </Row>
    </div>
  );
}
