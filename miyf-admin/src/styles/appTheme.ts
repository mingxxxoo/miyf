import { getCssVar } from './cssVars';

/** Ant Design ConfigProvider 主题，色值与 theme.css 对齐。 */
export const appTheme = {
  token: {
    colorPrimary: getCssVar('--ck-primary', '#FFB36B'),
    colorSuccess: getCssVar('--ck-secondary', '#9DD9C4'),
    colorBgLayout: getCssVar('--ck-bg', '#FFFDF8'),
    colorText: getCssVar('--ck-text', '#332E2A'),
    colorTextSecondary: getCssVar('--ck-muted', '#8F8880'),
    colorBorder: getCssVar('--ck-border', '#EEE8DF'),
    borderRadius: 12,
    borderRadiusLG: 16,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif",
  },
  components: {
    Layout: {
      bodyBg: getCssVar('--ck-bg', '#FFFDF8'),
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
