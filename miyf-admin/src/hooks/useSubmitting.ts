import { useCallback, useRef, useState } from 'react';

/**
 * 防重复提交：执行期间 locked，按钮可绑定 submitting。
 */
export function useSubmitting() {
  const [submitting, setSubmitting] = useState(false);
  const locked = useRef(false);

  const run = useCallback(async <T,>(fn: () => Promise<T>): Promise<T | undefined> => {
    if (locked.current) return undefined;
    locked.current = true;
    setSubmitting(true);
    try {
      return await fn();
    } finally {
      locked.current = false;
      setSubmitting(false);
    }
  }, []);

  return { submitting, run };
}
