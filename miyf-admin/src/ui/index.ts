/**
 * Miyf Admin UI：在 Ant Design 之上的统一封装。
 */
export {
  FormGrid,
  FormItem,
  FormModal,
  FormDrawer,
  DEFAULT_FORM_LAYOUT,
  FormLayoutContext,
  resolveColSpan,
  useFormLayout,
} from './form';
export type {
  FormGridProps,
  FormItemProps,
  FormModalProps,
  FormDrawerProps,
  FormColumns,
  FormLayoutConfig,
} from './form';

export { default as PageHeader } from './PageHeader';
export type { PageHeaderProps } from './PageHeader';
export { default as PageToolbar } from './PageToolbar';
export { default as FilterPanel } from './FilterPanel';
export { default as StatusBadge } from './StatusBadge';
export { default as MetricCard } from './MetricCard';
export { default as EmptyState } from './EmptyState';
export { default as ErrorState } from './ErrorState';
export { default as LoadingState } from './LoadingState';
export { default as DetailDrawer } from './DetailDrawer';
export { default as Breadcrumbs } from './Breadcrumbs';
export { default as ConfirmAction, confirmAction } from './ConfirmAction';
export { default as SettingSection } from './SettingSection';
export type { SettingSectionProps } from './SettingSection';
export { default as SplitWorkspace } from './SplitWorkspace';
export type { SplitWorkspaceProps } from './SplitWorkspace';
export { default as ChangeSummaryModal } from './ChangeSummaryModal';
export type { ChangeItem } from './ChangeSummaryModal';

export { default as PageTable } from '@/components/PageTable';
export { default as SearchForm } from '@/components/SearchForm';
export type { SearchField } from '@/components/SearchForm';
export { default as ConfirmDialog, confirmDialog } from '@/components/ConfirmDialog';
