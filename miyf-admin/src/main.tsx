import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './styles/global.css';

const theme = {
  token: {
    colorPrimary: '#FFB36B',
    colorSuccess: '#9DD9C4',
    colorBgLayout: '#FFFDF8',
    colorText: '#333333',
    colorTextSecondary: '#888888',
    colorBorder: '#F0ECE5',
    borderRadius: 12,
    borderRadiusLG: 16,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif",
  },
  components: {
    Layout: {
      bodyBg: '#FFFDF8',
      headerBg: '#FFFFFF',
      siderBg: '#FFFFFF',
    },
    Menu: {
      itemSelectedBg: 'rgba(255, 179, 107, 0.15)',
      itemSelectedColor: '#FFB36B',
      itemHoverBg: 'rgba(255, 179, 107, 0.08)',
    },
    Button: {
      primaryShadow: '0 2px 0 rgba(255, 179, 107, 0.1)',
    },
  },
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={theme}>
      <App />
    </ConfigProvider>
  </React.StrictMode>,
);
